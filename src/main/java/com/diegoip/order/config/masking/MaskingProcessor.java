package com.diegoip.order.config.masking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Procesador de ofuscación que aplica múltiples reglas en cadena.
 * Patrón Chain of Responsibility: Las reglas se aplican en orden de prioridad.
 */
public class MaskingProcessor {

    private final List<MaskingRule> rules;

    private MaskingProcessor(List<MaskingRule> rules) {
        // Ordenar por prioridad (mayor prioridad primero)
        this.rules = new ArrayList<>(rules);
        this.rules.sort(Comparator.comparingInt(MaskingRule::getPriority).reversed());
    }

    /**
     * Aplica todas las reglas de ofuscación al mensaje.
     *
     * @param message El mensaje a procesar
     * @return El mensaje con datos sensibles ofuscados
     */
    public String process(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;
        for (MaskingRule rule : rules) {
            result = applyRule(result, rule);
        }
        return result;
    }

    private String applyRule(String message, MaskingRule rule) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = rule.getPattern().matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(message, lastEnd, matcher.start());
            sb.append(rule.getStrategy().mask(matcher.group()));
            lastEnd = matcher.end();
        }
        sb.append(message.substring(lastEnd));

        return sb.toString();
    }

    /**
     * Crea un builder para configurar el procesador.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crea un procesador con las reglas por defecto.
     */
    public static MaskingProcessor defaultProcessor() {
        return builder()
            .addRule(MaskingRule.builder()
                .name("credit-card")
                .pattern("\\b\\d{16}\\b")
                .strategy(MaskingStrategies.CREDIT_CARD)
                .priority(100)  // Mayor prioridad (se aplica primero)
                .build())
            .addRule(MaskingRule.builder()
                .name("ruc")
                .pattern("\\b\\d{11}\\b")
                .strategy(MaskingStrategies.RUC)
                .priority(90)
                .build())
            .addRule(MaskingRule.builder()
                .name("phone")
                .pattern("\\b\\d{9,10}\\b")
                .strategy(MaskingStrategies.PHONE)
                .priority(80)
                .build())
            .addRule(MaskingRule.builder()
                .name("dni")
                .pattern("\\b\\d{8}\\b")
                .strategy(MaskingStrategies.DNI)
                .priority(70)
                .build())
            .addRule(MaskingRule.builder()
                .name("email")
                .pattern("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
                .strategy(MaskingStrategies.EMAIL)
                .priority(60)
                .build())
            .build();
    }

    /**
     * Builder para crear el procesador de forma fluida.
     */
    public static class Builder {
        private final List<MaskingRule> rules = new ArrayList<>();

        public Builder addRule(MaskingRule rule) {
            rules.add(rule);
            return this;
        }

        public MaskingProcessor build() {
            return new MaskingProcessor(rules);
        }
    }
}

