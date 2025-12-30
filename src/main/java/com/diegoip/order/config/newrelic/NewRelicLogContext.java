package com.diegoip.order.config.newrelic;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa el contexto de un log para New Relic.
 * Contiene toda la información necesaria para enviar un log a la Log API.
 */
public class NewRelicLogContext {

    private final String message;
    private final String level;
    private final String loggerName;
    private final String threadName;
    private final long threadId;
    private final long timestamp;
    private final String traceId;
    private final String spanId;
    private final String entityGuid;
    private final String entityName;
    private final String hostname;
    private final Map<String, String> customAttributes;
    private final String errorClass;
    private final String errorMessage;
    private final String errorStack;

    private NewRelicLogContext(Builder builder) {
        this.message = builder.message;
        this.level = builder.level;
        this.loggerName = builder.loggerName;
        this.threadName = builder.threadName;
        this.threadId = builder.threadId;
        this.timestamp = builder.timestamp;
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.entityGuid = builder.entityGuid;
        this.entityName = builder.entityName;
        this.hostname = builder.hostname;
        this.customAttributes = new HashMap<>(builder.customAttributes);
        this.errorClass = builder.errorClass;
        this.errorMessage = builder.errorMessage;
        this.errorStack = builder.errorStack;
    }

    // Getters
    public String getMessage() { return message; }
    public String getLevel() { return level; }
    public String getLoggerName() { return loggerName; }
    public String getThreadName() { return threadName; }
    public long getThreadId() { return threadId; }
    public long getTimestamp() { return timestamp; }
    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getEntityGuid() { return entityGuid; }
    public String getEntityName() { return entityName; }
    public String getHostname() { return hostname; }
    public Map<String, String> getCustomAttributes() { return customAttributes; }
    public String getErrorClass() { return errorClass; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorStack() { return errorStack; }

    public boolean hasTraceContext() {
        return traceId != null && !traceId.isEmpty();
    }

    public boolean hasEntityInfo() {
        return entityGuid != null && !entityGuid.isEmpty();
    }

    public boolean hasError() {
        return errorClass != null && !errorClass.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder para crear el contexto de forma fluida.
     */
    public static class Builder {
        private String message;
        private String level;
        private String loggerName;
        private String threadName;
        private long threadId;
        private long timestamp;
        private String traceId;
        private String spanId;
        private String entityGuid;
        private String entityName;
        private String hostname;
        private final Map<String, String> customAttributes = new HashMap<>();
        private String errorClass;
        private String errorMessage;
        private String errorStack;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder entityGuid(String entityGuid) {
            this.entityGuid = entityGuid;
            return this;
        }

        public Builder entityName(String entityName) {
            this.entityName = entityName;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder addAttribute(String key, String value) {
            this.customAttributes.put(key, value);
            return this;
        }

        public Builder addAttributes(Map<String, String> attributes) {
            if (attributes != null) {
                this.customAttributes.putAll(attributes);
            }
            return this;
        }

        public Builder errorClass(String errorClass) {
            this.errorClass = errorClass;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorStack(String errorStack) {
            this.errorStack = errorStack;
            return this;
        }

        public NewRelicLogContext build() {
            return new NewRelicLogContext(this);
        }
    }
}

