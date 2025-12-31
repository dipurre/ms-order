package com.diegoip.order.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.diegoip.order.config.masking.MaskingProcessor;
import com.diegoip.order.config.newrelic.NewRelicContextExtractor;
import com.diegoip.order.config.newrelic.NewRelicContextExtractor.NewRelicContext;
import com.diegoip.order.config.newrelic.NewRelicLogApiClient;
import com.diegoip.order.config.newrelic.NewRelicLogContext;
import com.diegoip.order.config.newrelic.NewRelicLogPayloadBuilder;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Appender que envía logs ofuscados directamente a la Log API de New Relic.
 *
 * <p>Características:
 * <ul>
 *   <li>Ofusca datos sensibles usando el patrón Strategy (MaskingProcessor)</li>
 *   <li>Correlaciona logs con transacciones usando trace.id y span.id</li>
 *   <li>Envía logs de forma asíncrona para no bloquear la aplicación</li>
 * </ul>
 *
 * <p>Patrones utilizados:
 * <ul>
 *   <li><b>Strategy</b>: MaskingStrategies para diferentes tipos de ofuscación</li>
 *   <li><b>Chain of Responsibility</b>: MaskingProcessor aplica reglas en orden</li>
 *   <li><b>Builder</b>: NewRelicLogContext y NewRelicLogPayloadBuilder</li>
 *   <li><b>Single Responsibility</b>: Cada clase tiene una responsabilidad única</li>
 * </ul>
 *
 * @see MaskingProcessor
 * @see NewRelicLogApiClient
 * @see NewRelicContextExtractor
 */
public class MaskingNewRelicLogApiAppender extends AppenderBase<ILoggingEvent> {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final NewRelicContextExtractor contextExtractor = new NewRelicContextExtractor();
    private final MaskingProcessor maskingProcessor = MaskingProcessor.defaultProcessor();

    private NewRelicLogApiClient logApiClient;

    @Override
    public void start() {
        String licenseKey = resolveLicenseKey();
        logApiClient = new NewRelicLogApiClient(licenseKey);

        if (!logApiClient.isConfigured()) {
            System.err.println("[MaskingNewRelicLogApiAppender] WARN: New Relic License Key not found. Logs will not be sent to Log API.");
            addWarn("New Relic License Key not found. Logs will not be sent to Log API.");
        } else {
            System.out.println("[MaskingNewRelicLogApiAppender] INFO: Initialized successfully. Logs will be sent to New Relic Log API.");
            addInfo("MaskingNewRelicLogApiAppender initialized successfully.");
        }

        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!logApiClient.isConfigured()) {
            return;
        }

        // Capturar todo el contexto en el thread actual (crítico para correlación)
        LogEventSnapshot snapshot = captureSnapshot(event);

        // Enviar en background para no bloquear
        executor.submit(() -> processAndSend(snapshot));
    }

    /**
     * Captura toda la información del evento en el thread actual.
     * Esto es necesario porque el trace context solo está disponible en el thread de la request.
     */
    private LogEventSnapshot captureSnapshot(ILoggingEvent event) {
        // Extraer información de excepción si existe
        String errorClass = null;
        String errorMessage = null;
        String errorStack = null;

        if (event.getThrowableProxy() != null) {
            var throwableProxy = event.getThrowableProxy();
            errorClass = throwableProxy.getClassName();
            errorMessage = throwableProxy.getMessage();
            errorStack = buildStackTrace(throwableProxy);
        }

        return new LogEventSnapshot(
            event.getFormattedMessage(),
            event.getLoggerName(),
            event.getLevel().toString(),
            event.getThreadName(),
            Thread.currentThread().threadId(),
            event.getTimeStamp(),
            event.getMDCPropertyMap(),
            contextExtractor.extractAll(),
            errorClass,
            errorMessage,
            errorStack
        );
    }

    /**
     * Construye el stack trace como string a partir del ThrowableProxy.
     */
    private String buildStackTrace(ch.qos.logback.classic.spi.IThrowableProxy throwableProxy) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName())
          .append(": ")
          .append(throwableProxy.getMessage())
          .append("\n");

        // Agregar stack trace elements
        for (var element : throwableProxy.getStackTraceElementProxyArray()) {
            sb.append("\tat ").append(element.getSTEAsString()).append("\n");
        }

        // Agregar causa si existe
        if (throwableProxy.getCause() != null) {
            sb.append("Caused by: ");
            sb.append(buildStackTrace(throwableProxy.getCause()));
        }

        return sb.toString();
    }

    /**
     * Procesa el snapshot y envía el log a New Relic.
     */
    private void processAndSend(LogEventSnapshot snapshot) {
        try {
            // 1. Ofuscar el mensaje usando el procesador de masking
            String maskedMessage = maskingProcessor.process(snapshot.message());

            // 2. Construir el contexto del log
            NewRelicLogContext logContext = buildLogContext(snapshot, maskedMessage);

            // 3. Construir el payload JSON
            String jsonPayload = new NewRelicLogPayloadBuilder(logContext).build();

            // 4. Enviar a New Relic
            if (!logApiClient.sendAndVerify(jsonPayload)) {
                addWarn("Failed to send log to New Relic Log API");
            }

        } catch (Exception e) {
            addError("Error sending log to New Relic Log API", e);
        }
    }

    private NewRelicLogContext buildLogContext(LogEventSnapshot snapshot, String maskedMessage) {
        NewRelicContext nrContext = snapshot.newRelicContext();
        Map<String, String> mdcMap = snapshot.mdcMap();

        // Obtener trace.id y span.id desde MDC (capturado por NewRelicTraceTurboFilter)
        // Si no están en MDC, usar los valores del contextExtractor como fallback
        String traceId = getValueFromMdcOrContext(mdcMap, "trace.id", nrContext.traceId());
        String spanId = getValueFromMdcOrContext(mdcMap, "span.id", nrContext.spanId());
        String entityGuid = getValueFromMdcOrContext(mdcMap, "entity.guid", nrContext.entityGuid());
        String entityName = getValueFromMdcOrContext(mdcMap, "entity.name", nrContext.entityName());
        String hostname = getValueFromMdcOrContext(mdcMap, "hostname", nrContext.hostname());

        var builder = NewRelicLogContext.builder()
            .message(maskedMessage)
            .level(snapshot.level())
            .loggerName(snapshot.loggerName())
            .threadName(snapshot.threadName())
            .threadId(snapshot.threadId())
            .timestamp(snapshot.timestamp())
            .traceId(traceId)
            .spanId(spanId)
            .entityGuid(entityGuid)
            .entityName(entityName)
            .hostname(hostname)
            .addAttributes(mdcMap);

        // Agregar información de error si existe
        if (snapshot.errorClass() != null) {
            builder.errorClass(snapshot.errorClass())
                   .errorMessage(snapshot.errorMessage())
                   .errorStack(snapshot.errorStack());
        }

        return builder.build();
    }

    /**
     * Obtiene un valor del MDC, o usa el fallback si no existe o está vacío.
     */
    private String getValueFromMdcOrContext(Map<String, String> mdcMap, String key, String fallback) {
        if (mdcMap != null && mdcMap.containsKey(key)) {
            String value = mdcMap.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }

    private String resolveLicenseKey() {
        String key = System.getProperty("newrelic.config.license_key");
        if (key == null || key.isEmpty()) {
            key = System.getenv("NEW_RELIC_LICENSE_KEY");
        }
        return key;
    }

    @Override
    public void stop() {
        shutdownExecutor();
        super.stop();
    }

    private void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Snapshot inmutable del evento de log.
     * Captura toda la información necesaria en el thread de la request.
     */
    private record LogEventSnapshot(
        String message,
        String loggerName,
        String level,
        String threadName,
        long threadId,
        long timestamp,
        Map<String, String> mdcMap,
        NewRelicContext newRelicContext,
        String errorClass,
        String errorMessage,
        String errorStack
    ) {}
}

