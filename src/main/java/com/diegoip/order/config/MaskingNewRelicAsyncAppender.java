package com.diegoip.order.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.newrelic.api.agent.NewRelic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appender que envía logs ofuscados directamente a New Relic usando la API de Logs in Context.
 *
 * Uso en logback-spring.xml:
 * <appender name="NEW_RELIC_API" class="com.diegoip.order.config.MaskingNewRelicAsyncAppender"/>
 *
 * Requiere que el agente de New Relic esté corriendo.
 */
public class MaskingNewRelicAsyncAppender extends AppenderBase<ILoggingEvent> {

    private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,10}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{16}\\b");

    @Override
    protected void append(ILoggingEvent event) {
        try {
            // Ofuscar el mensaje
            String maskedMessage = maskMessage(event.getFormattedMessage());

            // Crear atributos para el evento de log
            Map<String, Object> logAttributes = new HashMap<>();
            logAttributes.put("message", maskedMessage);
            logAttributes.put("log.level", event.getLevel().toString());
            logAttributes.put("logger.name", event.getLoggerName());
            logAttributes.put("thread.name", event.getThreadName());
            logAttributes.put("timestamp", event.getTimeStamp());

            // Agregar MDC si existe (incluye trace.id y span.id si local_decorating está activo)
            if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
                event.getMDCPropertyMap().forEach(logAttributes::put);
            }

            // Enviar como evento personalizado "Log" a New Relic
            // Esto aparecerá en NRQL: SELECT * FROM Log WHERE appName = 'ms-order'
            NewRelic.getAgent().getInsights().recordCustomEvent("Log", logAttributes);

        } catch (Exception e) {
            addError("Error al enviar log a New Relic: " + e.getMessage(), e);
        }
    }

    private String maskMessage(String message) {
        if (message == null) {
            return message;
        }

        String masked = message;
        masked = maskPattern(masked, CARD_PATTERN, this::maskCard);
        masked = maskPattern(masked, PHONE_PATTERN, this::maskPhone);
        masked = maskPattern(masked, DNI_PATTERN, this::maskDni);
        masked = maskPattern(masked, EMAIL_PATTERN, this::maskEmail);

        return masked;
    }

    private String maskPattern(String message, Pattern pattern, MaskFunction maskFunction) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = pattern.matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(message, lastEnd, matcher.start());
            sb.append(maskFunction.mask(matcher.group()));
            lastEnd = matcher.end();
        }
        sb.append(message.substring(lastEnd));

        return sb.toString();
    }

    private String maskDni(String dni) {
        if (dni == null || dni.length() != 8) {
            return "***";
        }
        return dni.substring(0, 2) + "***" + dni.substring(6);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return "***@" + domain;
        }
        return localPart.charAt(0) + "***@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 3);
    }

    private String maskCard(String card) {
        if (card == null || card.length() != 16) {
            return "***";
        }
        return "***" + card.substring(12);
    }

    @FunctionalInterface
    private interface MaskFunction {
        String mask(String value);
    }
}

