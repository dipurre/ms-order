package com.diegoip.order.config.masking;

/**
 * Estrategia de ofuscación para datos sensibles.
 * Patrón Strategy: Define una interfaz común para diferentes tipos de ofuscación.
 */
@FunctionalInterface
public interface MaskingStrategy {

    /**
     * Aplica la ofuscación al valor dado.
     *
     * @param value El valor a ofuscar
     * @return El valor ofuscado
     */
    String mask(String value);
}

