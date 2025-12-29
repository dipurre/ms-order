package com.diegoip.order.util;

public class LogMaskingUtil {
    
    private static final String MASK = "***";
    
    public static String maskDni(String dni) {
        if (dni == null || dni.length() < 4) {
            return MASK;
        }
        return dni.substring(0, 2) + MASK + dni.substring(dni.length() - 2);
    }
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return MASK;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return MASK + "@" + domain;
        }
        return localPart.charAt(0) + MASK + "@" + domain;
    }
    
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return MASK;
        }
        return MASK + phone.substring(phone.length() - 3);
    }
    
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return MASK;
        }
        return MASK + cardNumber.substring(cardNumber.length() - 4);
    }
    
    public static String maskGeneric(String value) {
        if (value == null || value.length() <= 4) {
            return MASK;
        }
        return value.substring(0, 2) + MASK + value.substring(value.length() - 2);
    }
}
