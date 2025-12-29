package com.diegoip.order.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskingConsoleAppender extends ConsoleAppender<ILoggingEvent> {

    private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,10}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{16}\\b");

    @Override
    protected void append(ILoggingEvent eventObject) {
        // Crear un nuevo evento con el mensaje ofuscado
        ILoggingEvent maskedEvent = createMaskedEvent(eventObject);
        super.append(maskedEvent);
    }

    private ILoggingEvent createMaskedEvent(ILoggingEvent event) {
        String originalMessage = event.getFormattedMessage();
        String maskedMessage = maskMessage(originalMessage);

        if (originalMessage.equals(maskedMessage)) {
            return event;
        }

        // Crear un nuevo LoggingEvent con el mensaje ofuscado
        LoggingEvent maskedEvent = new LoggingEvent();
        maskedEvent.setLoggerName(event.getLoggerName());
        maskedEvent.setLevel(event.getLevel());
        maskedEvent.setMessage(maskedMessage);
        maskedEvent.setTimeStamp(event.getTimeStamp());
        maskedEvent.setThreadName(event.getThreadName());
        maskedEvent.setMDCPropertyMap(event.getMDCPropertyMap());

        return maskedEvent;
    }

    private String maskMessage(String message) {
        if (message == null) {
            return message;
        }

        String masked = message;

        // Ofuscar tarjetas primero (16 dígitos) para evitar conflictos con otros patrones
        masked = maskPattern(masked, CARD_PATTERN, this::maskCard);

        // Ofuscar teléfonos (9-10 dígitos)
        masked = maskPattern(masked, PHONE_PATTERN, this::maskPhone);

        // Ofuscar DNI (8 dígitos)
        masked = maskPattern(masked, DNI_PATTERN, this::maskDni);

        // Ofuscar emails
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

