package com.diegoip.order.config.masking;

/**
 * Implementaciones de estrategias de ofuscación para diferentes tipos de datos sensibles.
 * Patrón Strategy: Cada clase implementa una forma específica de ofuscar.
 */
public final class MaskingStrategies {

    private MaskingStrategies() {
        // Utility class
    }

    /**
     * Estrategia para ofuscar DNI (8 dígitos).
     * Ejemplo: 12345678 → 12***78
     */
    public static final MaskingStrategy DNI = value -> {
        if (value == null || value.length() != 8) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(6);
    };

    /**
     * Estrategia para ofuscar Email.
     * Ejemplo: usuario@domain.com → u***@domain.com
     */
    public static final MaskingStrategy EMAIL = value -> {
        if (value == null || !value.contains("@")) {
            return "***";
        }
        String[] parts = value.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return "***@" + domain;
        }
        return localPart.charAt(0) + "***@" + domain;
    };

    /**
     * Estrategia para ofuscar Teléfono (9-10 dígitos).
     * Ejemplo: 987654321 → ***321
     */
    public static final MaskingStrategy PHONE = value -> {
        if (value == null || value.length() < 4) {
            return "***";
        }
        return "***" + value.substring(value.length() - 3);
    };

    /**
     * Estrategia para ofuscar Tarjeta de Crédito (16 dígitos).
     * Ejemplo: 1234567890123456 → ***3456
     */
    public static final MaskingStrategy CREDIT_CARD = value -> {
        if (value == null || value.length() != 16) {
            return "***";
        }
        return "***" + value.substring(12);
    };

    /**
     * Estrategia para ofuscar RUC (11 dígitos).
     * Ejemplo: 12345678901 → 123***901
     */
    public static final MaskingStrategy RUC = value -> {
        if (value == null || value.length() != 11) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(8);
    };

    /**
     * Estrategia que reemplaza completamente el valor.
     */
    public static final MaskingStrategy FULL_MASK = value -> "***";
}

