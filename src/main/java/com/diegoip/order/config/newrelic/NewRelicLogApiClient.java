package com.diegoip.order.config.newrelic;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP para enviar logs a la Log API de New Relic.
 * Responsabilidad única: Comunicación HTTP con New Relic.
 */
public class NewRelicLogApiClient {

    private static final String LOG_API_URL = "https://log-api.newrelic.com/log/v1";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String licenseKey;

    public NewRelicLogApiClient(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    /**
     * Envía el payload JSON a la Log API de New Relic.
     *
     * @param jsonPayload El payload JSON a enviar
     * @return El código de respuesta HTTP
     * @throws Exception Si hay un error de comunicación
     */
    public int send(String jsonPayload) throws Exception {
        URI uri = URI.create(LOG_API_URL);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

        try {
            configureConnection(conn);
            sendPayload(conn, jsonPayload);
            return conn.getResponseCode();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Envía el payload y verifica que la respuesta sea exitosa.
     *
     * @param jsonPayload El payload JSON a enviar
     * @return true si la respuesta fue exitosa (200 o 202)
     * @throws Exception Si hay un error de comunicación
     */
    public boolean sendAndVerify(String jsonPayload) throws Exception {
        int responseCode = send(jsonPayload);
        return responseCode == 200 || responseCode == 202;
    }

    private void configureConnection(HttpURLConnection conn) throws Exception {
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Api-Key", licenseKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
    }

    private void sendPayload(HttpURLConnection conn, String jsonPayload) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Verifica si el cliente tiene una license key válida.
     */
    public boolean isConfigured() {
        return licenseKey != null && !licenseKey.isEmpty();
    }
}

