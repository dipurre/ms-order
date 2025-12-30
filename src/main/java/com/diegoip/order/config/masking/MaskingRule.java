package com.diegoip.order.config.masking;

import java.util.regex.Pattern;

/**
 * Regla de ofuscación que combina un patrón regex con una estrategia de ofuscación.
 * Parte del patrón Chain of Responsibility.
 */
public class MaskingRule {

    private final String name;
    private final Pattern pattern;
    private final MaskingStrategy strategy;
    private final int priority;

    private MaskingRule(Builder builder) {
        this.name = builder.name;
        this.pattern = builder.pattern;
        this.strategy = builder.strategy;
        this.priority = builder.priority;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public MaskingStrategy getStrategy() {
        return strategy;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Builder para crear reglas de ofuscación de forma fluida.
     */
    public static class Builder {
        private String name;
        private Pattern pattern;
        private MaskingStrategy strategy;
        private int priority = 0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder pattern(String regex) {
            this.pattern = Pattern.compile(regex);
            return this;
        }

        public Builder pattern(Pattern pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder strategy(MaskingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public MaskingRule build() {
            if (name == null || pattern == null || strategy == null) {
                throw new IllegalStateException("name, pattern and strategy are required");
            }
            return new MaskingRule(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

