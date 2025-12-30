package com.diegoip.order.config.newrelic;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;

import java.net.InetAddress;
import java.util.Map;

/**
 * Extrae información de contexto del agente de New Relic.
 * Responsabilidad única: Obtener metadata del agente.
 */
public class NewRelicContextExtractor {

    private static final String DEFAULT_APP_NAME = "ms-order";
    private static final String DEFAULT_HOSTNAME = "unknown";

    /**
     * Extrae el trace ID del contexto actual.
     */
    public String extractTraceId() {
        try {
            TraceMetadata metadata = NewRelic.getAgent().getTraceMetadata();
            return metadata != null ? metadata.getTraceId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae el span ID del contexto actual.
     */
    public String extractSpanId() {
        try {
            TraceMetadata metadata = NewRelic.getAgent().getTraceMetadata();
            return metadata != null ? metadata.getSpanId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae el entity GUID del agente.
     */
    public String extractEntityGuid() {
        return getLinkingMetadataValue("entity.guid");
    }

    /**
     * Extrae el nombre de la entidad (aplicación).
     */
    public String extractEntityName() {
        String name = getLinkingMetadataValue("entity.name");
        if (name == null || name.isEmpty()) {
            name = System.getProperty("newrelic.config.app_name", DEFAULT_APP_NAME);
        }
        return name;
    }

    /**
     * Extrae el hostname.
     */
    public String extractHostname() {
        String hostname = getLinkingMetadataValue("hostname");
        if (hostname == null || hostname.isEmpty()) {
            hostname = getLocalHostname();
        }
        return hostname;
    }

    /**
     * Extrae todo el contexto de New Relic de una vez.
     */
    public NewRelicContext extractAll() {
        return new NewRelicContext(
            extractTraceId(),
            extractSpanId(),
            extractEntityGuid(),
            extractEntityName(),
            extractHostname()
        );
    }

    private String getLinkingMetadataValue(String key) {
        try {
            Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
            return metadata != null ? metadata.get(key) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return DEFAULT_HOSTNAME;
        }
    }

    /**
     * Contenedor inmutable para el contexto de New Relic.
     */
    public record NewRelicContext(
        String traceId,
        String spanId,
        String entityGuid,
        String entityName,
        String hostname
    ) {}
}

