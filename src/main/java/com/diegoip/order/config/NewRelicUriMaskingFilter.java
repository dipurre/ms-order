package com.diegoip.order.config;

import com.newrelic.api.agent.NewRelic;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro que ofusca datos sensibles en la URI de la transacción de New Relic.
 * Esto afecta cómo se muestra request.uri en las transacciones.
 */
/*@Component
@Order(1)*/
public class NewRelicUriMaskingFilter implements Filter {

    private static final Pattern DNI_PATTERN = Pattern.compile("/dni/([0-9]{8})(?:/|$|\\?)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("/email/([^/]+@[^/]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("/phone/([0-9]{9,10})(?:/|$|\\?)");
    private static final Pattern ID_NUMERIC_PATTERN = Pattern.compile("/id/([0-9]{8,})(?:/|$|\\?)");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String originalUri = httpRequest.getRequestURI();
            String maskedUri = maskUri(originalUri);

            // Si la URI fue modificada, renombrar la transacción en New Relic
            if (!originalUri.equals(maskedUri)) {
                // Establecer el nombre de la transacción con la URI ofuscada
                NewRelic.setTransactionName("Custom", maskedUri);

                // También agregar como atributo personalizado
                NewRelic.addCustomParameter("request.uri.masked", maskedUri);
                NewRelic.addCustomParameter("request.uri.original.masked", true);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    private String maskUri(String uri) {
        if (uri == null) {
            return uri;
        }

        String masked = uri;

        // Ofuscar DNI (8 dígitos después de /dni/)
        Matcher dniMatcher = DNI_PATTERN.matcher(masked);
        if (dniMatcher.find()) {
            String dni = dniMatcher.group(1);
            String maskedDni = dni.substring(0, 2) + "***" + dni.substring(6);
            masked = masked.replace("/dni/" + dni, "/dni/" + maskedDni);
        }

        // Ofuscar Email
        Matcher emailMatcher = EMAIL_PATTERN.matcher(masked);
        if (emailMatcher.find()) {
            String email = emailMatcher.group(1);
            String maskedEmail = maskEmail(email);
            masked = masked.replace("/email/" + email, "/email/" + maskedEmail);
        }

        // Ofuscar Teléfono
        Matcher phoneMatcher = PHONE_PATTERN.matcher(masked);
        if (phoneMatcher.find()) {
            String phone = phoneMatcher.group(1);
            String maskedPhone = "***" + phone.substring(phone.length() - 3);
            masked = masked.replace("/phone/" + phone, "/phone/" + maskedPhone);
        }

        // Ofuscar IDs numéricos largos (posibles DNIs o documentos)
        Matcher idMatcher = ID_NUMERIC_PATTERN.matcher(masked);
        if (idMatcher.find()) {
            String id = idMatcher.group(1);
            if (id.length() == 8) {
                // Probablemente un DNI
                String maskedId = id.substring(0, 2) + "***" + id.substring(6);
                masked = masked.replace("/id/" + id, "/id/" + maskedId);
            }
        }

        return masked;
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
}

