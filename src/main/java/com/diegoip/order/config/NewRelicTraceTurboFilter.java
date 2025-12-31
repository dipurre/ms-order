package com.diegoip.order.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.Map;

/**
 * TurboFilter que captura el trace.id y span.id de New Relic en el MDC.
 * Se ejecuta en el hilo original de la request, antes de que AsyncAppender cambie de hilo.
 */
public class NewRelicTraceTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        try {
            // Capturar trace metadata
            TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
            String traceId = traceMetadata.getTraceId();
            String spanId = traceMetadata.getSpanId();

            if (traceId != null && !traceId.isEmpty()) {
                MDC.put("trace.id", traceId);
            }
            if (spanId != null && !spanId.isEmpty()) {
                MDC.put("span.id", spanId);
            }

            // Capturar linking metadata (entity.guid, entity.name, hostname)
            Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            if (linkingMetadata != null) {
                linkingMetadata.forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) {
                        MDC.put(key, value);
                    }
                });
            }

        } catch (Exception ignored) {
            // Ignorar errores si el agente no está disponible
        }
        return FilterReply.NEUTRAL;
    }
}
