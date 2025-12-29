package com.diegoip.order.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appender que envía logs ofuscados directamente a New Relic usando la API de Logs in Context.
 * Incluye todos los campos necesarios para correlación de traces.
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

            // Crear atributos para el log
            Map<String, Object> logAttributes = new HashMap<>();

            // Campos básicos del log
            logAttributes.put("message", maskedMessage);
            logAttributes.put("level", event.getLevel().toString());
            logAttributes.put("log.level", event.getLevel().toString());
            logAttributes.put("logger.name", event.getLoggerName());
            logAttributes.put("logger.fqcn", "ch.qos.logback.classic.Logger");
            logAttributes.put("thread.name", event.getThreadName());
            logAttributes.put("thread.id", Thread.currentThread().getId());
            logAttributes.put("timestamp", event.getTimeStamp());

            // Obtener información de tracing de New Relic
            TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
            String traceId = traceMetadata.getTraceId();
            String spanId = traceMetadata.getSpanId();

            if (traceId != null && !traceId.isEmpty()) {
                logAttributes.put("trace.id", traceId);
            }
            if (spanId != null && !spanId.isEmpty()) {
                logAttributes.put("span.id", spanId);
            }

            // Información de la entidad
            Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            if (linkingMetadata != null) {
                if (linkingMetadata.containsKey("entity.guid")) {
                    logAttributes.put("entity.guid", linkingMetadata.get("entity.guid"));
                    logAttributes.put("entity.guids", linkingMetadata.get("entity.guid"));
                }
                if (linkingMetadata.containsKey("entity.name")) {
                    logAttributes.put("entity.name", linkingMetadata.get("entity.name"));
                }
                if (linkingMetadata.containsKey("hostname")) {
                    logAttributes.put("hostname", linkingMetadata.get("hostname"));
                }
            }

            // Campos adicionales de New Relic
            logAttributes.put("newrelic.source", "logs.APM");
            logAttributes.put("instrumentation", "logback-custom-masking");

            // Agregar MDC si existe (incluye trace.id y span.id si local_decorating está activo)
            if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
                event.getMDCPropertyMap().forEach((key, value) -> {
                    // Evitar sobrescribir los campos que ya agregamos
                    if (!logAttributes.containsKey(key)) {
                        logAttributes.put(key, value);
                    }
                });
            }

            // Enviar como evento "Log" a New Relic
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

