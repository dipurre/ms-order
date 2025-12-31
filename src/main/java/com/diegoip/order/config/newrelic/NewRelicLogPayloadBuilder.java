package com.diegoip.order.config.newrelic;

import java.util.Map;

/**
 * Construye el payload JSON para la Log API de New Relic.
 * Patrón Builder: Construye el JSON de forma estructurada y mantenible.
 */
public class NewRelicLogPayloadBuilder {

    private static final String[] RESERVED_KEYS = {
        "trace.id", "span.id", "entity.guid", "entity.name", "hostname",
        "error.class", "error.message", "error.stack"
    };

    private final StringBuilder json;
    private final NewRelicLogContext context;

    public NewRelicLogPayloadBuilder(NewRelicLogContext context) {
        this.context = context;
        this.json = new StringBuilder();
    }

    /**
     * Construye el payload JSON completo.
     */
    public String build() {
        json.setLength(0);
        json.append("[{");

        appendCommonAttributes();
        appendLogs();

        json.append("}]");
        return json.toString();
    }

    private void appendCommonAttributes() {
        json.append("\"common\":{\"attributes\":{");
        json.append("\"logtype\":\"application\",");
        json.append("\"service\":\"").append(escape(context.getEntityName())).append("\",");
        json.append("\"hostname\":\"").append(escape(context.getHostname())).append("\",");
        json.append("\"instrumentation\":\"logback-masking-appender\"");
        json.append("}},");
    }

    private void appendLogs() {
        json.append("\"logs\":[{");

        // Timestamp y mensaje
        json.append("\"timestamp\":").append(context.getTimestamp()).append(",");
        json.append("\"message\":\"").append(escape(context.getMessage())).append("\",");

        // Attributes
        json.append("\"attributes\":{");
        appendLogAttributes();
        appendTraceContext();
        appendEntityInfo();
        appendErrorInfo();
        appendCustomAttributes();
        json.append("}}]");
    }

    private void appendLogAttributes() {
        json.append("\"level\":\"").append(context.getLevel()).append("\",");
        json.append("\"log.level\":\"").append(context.getLevel()).append("\",");
        json.append("\"logger.name\":\"").append(escape(context.getLoggerName())).append("\",");
        json.append("\"logger.fqcn\":\"ch.qos.logback.classic.Logger\",");
        json.append("\"thread.name\":\"").append(escape(context.getThreadName())).append("\",");
        json.append("\"thread.id\":").append(context.getThreadId()).append(",");
        json.append("\"newrelic.source\":\"logs.APM\"");
    }

    private void appendTraceContext() {
        if (context.getTraceId() != null && !context.getTraceId().isEmpty()) {
            json.append(",\"trace.id\":\"").append(escape(context.getTraceId())).append("\"");
        }else {
            String tracr = context.getCustomAttributes().get("trace.id");

        }
        if (context.getSpanId() != null && !context.getSpanId().isEmpty()) {
            json.append(",\"span.id\":\"").append(escape(context.getSpanId())).append("\"");
        }
    }

    private void appendEntityInfo() {
        String entityGuid = context.getEntityGuid();
        String entityName = context.getEntityName();
        String hostname = context.getHostname();

        if (entityGuid != null && !entityGuid.isEmpty()) {
            json.append(",\"entity.guid\":\"").append(escape(entityGuid)).append("\"");
            json.append(",\"entity.guids\":\"").append(escape(entityGuid)).append("\"");
        }
        if (entityName != null && !entityName.isEmpty()) {
            json.append(",\"entity.name\":\"").append(escape(entityName)).append("\"");
        }
        if (hostname != null && !hostname.isEmpty()) {
            json.append(",\"hostname\":\"").append(escape(hostname)).append("\"");
        }
    }

    private void appendErrorInfo() {
        if (!context.hasError()) {
            return;
        }

        String errorClass = context.getErrorClass();
        String errorMessage = context.getErrorMessage();
        String errorStack = context.getErrorStack();

        if (errorClass != null && !errorClass.isEmpty()) {
            json.append(",\"error.class\":\"").append(escape(errorClass)).append("\"");
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            json.append(",\"error.message\":\"").append(escape(errorMessage)).append("\"");
        }
        if (errorStack != null && !errorStack.isEmpty()) {
            json.append(",\"error.stack\":\"").append(escape(errorStack)).append("\"");
        }
    }

    private void appendCustomAttributes() {
        Map<String, String> attrs = context.getCustomAttributes();
        if (attrs == null || attrs.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            String key = entry.getKey();
            // Evitar duplicar campos reservados
            if (!isReservedKey(key)) {
                json.append(",\"").append(escape(key)).append("\":\"")
                    .append(escape(entry.getValue())).append("\"");
            }
        }
    }

    private boolean isReservedKey(String key) {
        for (String reserved : RESERVED_KEYS) {
            if (reserved.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}

