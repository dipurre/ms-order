package com.diegoip.order.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {
    
    private Pattern multiPattern;
    private final List<String> maskPatterns = new ArrayList<>();
    
    public void addMaskPattern(String maskPattern) {
        maskPatterns.add(maskPattern);
        multiPattern = Pattern.compile(
            String.join("|", maskPatterns),
            Pattern.MULTILINE
        );
    }
    
    @Override
    public String doLayout(ILoggingEvent event) {
        return maskMessage(super.doLayout(event));
    }
    
    private String maskMessage(String message) {
        if (multiPattern == null) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder(message);
        Matcher matcher = multiPattern.matcher(sb);
        
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String matched = sb.substring(start, end);
            sb.replace(start, end, mask(matched));
            matcher = multiPattern.matcher(sb);
        }
        
        return sb.toString();
    }
    
    private String mask(String value) {
        if (value == null || value.length() < 4) {
            return "***";
        }
        
        // Para DNI (8 dígitos)
        if (value.matches("\\d{8}")) {
            return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
        }
        
        // Para email
        if (value.contains("@")) {
            String[] parts = value.split("@");
            String localPart = parts[0];
            String domain = parts[1];
            if (localPart.length() <= 2) {
                return "***@" + domain;
            }
            return localPart.charAt(0) + "***@" + domain;
        }
        
        // Para teléfonos (9-10 dígitos)
        if (value.matches("\\d{9,10}")) {
            return "***" + value.substring(value.length() - 3);
        }
        
        // Para tarjetas (16 dígitos)
        if (value.matches("\\d{16}")) {
            return "***" + value.substring(value.length() - 4);
        }
        
        // Genérico
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
