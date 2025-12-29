package com.diegoip.order.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TurboFilter que modifica el formato de los mensajes ANTES de que sean procesados por cualquier appender
 * Esto incluye el log forwarding de New Relic
 */
public class MaskingTurboFilterV2 extends TurboFilter {

    private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,10}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{16}\\b");

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        // Intentar ofuscar los parámetros si existen
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    params[i] = maskMessage((String) params[i]);
                }
            }
        }

        return FilterReply.NEUTRAL;
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

