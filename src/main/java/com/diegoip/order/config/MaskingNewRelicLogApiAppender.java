package com.diegoip.order.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.newrelic.api.agent.NewRelic;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appender que envía logs ofuscados directamente a la Log API de New Relic.
 * Incluye trace.id y span.id para correlación con transacciones en APM.
 *
 * Los logs enviados con este appender aparecerán en:
 * - New Relic Logs UI
 * - Correlacionados con transacciones en APM
 * - Distributed Tracing
 */
public class MaskingNewRelicLogApiAppender extends AppenderBase<ILoggingEvent> {

    private static final String LOG_API_URL = "https://log-api.newrelic.com/log/v1";

    private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,10}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{16}\\b");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String licenseKey;

    @Override
    public void start() {
        // Obtener license key desde system property o variable de entorno
        licenseKey = System.getProperty("newrelic.config.license_key");
        if (licenseKey == null || licenseKey.isEmpty()) {
            licenseKey = System.getenv("NEW_RELIC_LICENSE_KEY");
        }

        if (licenseKey == null || licenseKey.isEmpty()) {
            addWarn("New Relic License Key not found. Logs will not be sent to New Relic Log API.");
        } else {
            addInfo("MaskingNewRelicLogApiAppender initialized successfully.");
        }

        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (licenseKey == null || licenseKey.isEmpty()) {
            return;
        }

        // Capturar datos del evento en el thread actual
        final String formattedMessage = event.getFormattedMessage();
        final String loggerName = event.getLoggerName();
        final String levelStr = event.getLevel().toString();
        final String threadName = event.getThreadName();
        final long timestamp = event.getTimeStamp();
        final long threadId = Thread.currentThread().getId();
        final Map<String, String> mdcMap = event.getMDCPropertyMap();

        // Capturar trace context en el thread actual (importante!)
        final String traceId = getTraceId();
        final String spanId = getSpanId();
        final String entityGuid = getEntityGuid();
        final String entityName = getEntityName();
        final String hostname = getHostname();

        // Ejecutar envío en background para no bloquear
        executor.submit(() -> {
            try {
                sendLogToNewRelic(
                    formattedMessage, loggerName, levelStr, threadName,
                    timestamp, threadId, mdcMap,
                    traceId, spanId, entityGuid, entityName, hostname
                );
            } catch (Exception e) {
                addError("Error sending log to New Relic Log API", e);
            }
        });
    }

    private void sendLogToNewRelic(
            String formattedMessage, String loggerName, String levelStr,
            String threadName, long timestamp, long threadId,
            Map<String, String> mdcMap,
            String traceId, String spanId, String entityGuid,
            String entityName, String hostname) {

        try {
            // Ofuscar el mensaje
            String maskedMessage = maskMessage(formattedMessage);

            // Construir JSON payload
            String jsonPayload = buildJsonPayload(
                maskedMessage, loggerName, levelStr, threadName,
                timestamp, threadId, mdcMap,
                traceId, spanId, entityGuid, entityName, hostname
            );

            // Enviar a Log API
            sendHttpRequest(jsonPayload);

        } catch (Exception e) {
            addError("Error in sendLogToNewRelic", e);
        }
    }

    private String buildJsonPayload(
            String maskedMessage, String loggerName, String levelStr,
            String threadName, long timestamp, long threadId,
            Map<String, String> mdcMap,
            String traceId, String spanId, String entityGuid,
            String entityName, String hostname) {

        StringBuilder json = new StringBuilder();
        json.append("[{");

        // Common attributes
        json.append("\"common\":{");
        json.append("\"attributes\":{");
        json.append("\"logtype\":\"application\",");
        json.append("\"service\":\"").append(escapeJson(entityName)).append("\",");
        json.append("\"hostname\":\"").append(escapeJson(hostname)).append("\",");
        json.append("\"instrumentation\":\"logback-masking-log-api\"");
        json.append("}},");

        // Logs array
        json.append("\"logs\":[{");

        // Timestamp
        json.append("\"timestamp\":").append(timestamp).append(",");

        // Message (ofuscado)
        json.append("\"message\":\"").append(escapeJson(maskedMessage)).append("\",");

        // Attributes
        json.append("\"attributes\":{");

        // Log level
        json.append("\"level\":\"").append(levelStr).append("\",");
        json.append("\"log.level\":\"").append(levelStr).append("\",");

        // Logger info
        json.append("\"logger.name\":\"").append(escapeJson(loggerName)).append("\",");
        json.append("\"logger.fqcn\":\"ch.qos.logback.classic.Logger\",");

        // Thread info
        json.append("\"thread.name\":\"").append(escapeJson(threadName)).append("\",");
        json.append("\"thread.id\":").append(threadId).append(",");

        // New Relic source
        json.append("\"newrelic.source\":\"logs.APM\"");

        // Trace context para correlación con transacciones
        if (traceId != null && !traceId.isEmpty()) {
            json.append(",\"trace.id\":\"").append(escapeJson(traceId)).append("\"");
        }
        if (spanId != null && !spanId.isEmpty()) {
            json.append(",\"span.id\":\"").append(escapeJson(spanId)).append("\"");
        }

        // Entity info para linking con APM
        if (entityGuid != null && !entityGuid.isEmpty()) {
            json.append(",\"entity.guid\":\"").append(escapeJson(entityGuid)).append("\"");
            json.append(",\"entity.guids\":\"").append(escapeJson(entityGuid)).append("\"");
        }
        if (entityName != null && !entityName.isEmpty()) {
            json.append(",\"entity.name\":\"").append(escapeJson(entityName)).append("\"");
        }
        if (hostname != null && !hostname.isEmpty()) {
            json.append(",\"hostname\":\"").append(escapeJson(hostname)).append("\"");
        }

        // MDC attributes
        if (mdcMap != null && !mdcMap.isEmpty()) {
            for (Map.Entry<String, String> entry : mdcMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                // Evitar duplicar campos que ya agregamos
                if (!key.equals("trace.id") && !key.equals("span.id") &&
                    !key.equals("entity.guid") && !key.equals("entity.name")) {
                    json.append(",\"").append(escapeJson(key)).append("\":\"")
                        .append(escapeJson(value)).append("\"");
                }
            }
        }

        json.append("}}]}]");

        return json.toString();
    }

    private void sendHttpRequest(String jsonPayload) throws Exception {
        URI uri = URI.create(LOG_API_URL);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Api-Key", licenseKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 202 && responseCode != 200) {
                addWarn("New Relic Log API returned: " + responseCode);
            }
        } finally {
            conn.disconnect();
        }
    }

    // ========== Métodos para obtener contexto de New Relic ==========

    private String getTraceId() {
        try {
            return NewRelic.getAgent().getTraceMetadata().getTraceId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getSpanId() {
        try {
            return NewRelic.getAgent().getTraceMetadata().getSpanId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getEntityGuid() {
        try {
            Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
            return metadata != null ? metadata.get("entity.guid") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getEntityName() {
        try {
            Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
            String name = metadata != null ? metadata.get("entity.name") : null;
            return name != null ? name : System.getProperty("newrelic.config.app_name", "ms-order");
        } catch (Exception e) {
            return System.getProperty("newrelic.config.app_name", "ms-order");
        }
    }

    private String getHostname() {
        try {
            Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
            String hostname = metadata != null ? metadata.get("hostname") : null;
            if (hostname == null) {
                hostname = java.net.InetAddress.getLocalHost().getHostName();
            }
            return hostname;
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ========== Métodos de ofuscación ==========

    private String maskMessage(String message) {
        if (message == null) {
            return message;
        }

        String masked = message;
        masked = maskPattern(masked, CARD_PATTERN, this::maskCard);
        masked = maskPattern(masked, PHONE_PATTERN, this::maskPhone);
        masked = maskPattern(masked, DNI_PATTERN, this::maskDni);
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

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        super.stop();
    }

    @FunctionalInterface
    private interface MaskFunction {
        String mask(String value);
    }
}

