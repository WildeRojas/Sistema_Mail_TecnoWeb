package com.grupo06sa.sistema_inventario.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PagoFacilService {
    private static final Logger logger = LoggerFactory.getLogger(PagoFacilService.class);
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pagofacil.url.base}")
    private String baseUrl;

    @Value("${pagofacil.token.service}")
    private String tokenService;

    @Value("${pagofacil.token.secret}")
    private String tokenSecret;

    @Value("${pagofacil.callback.url:}")
    private String callbackUrl;

    @Value("${pagofacil.monto.tope:0}")
    private BigDecimal montoTope;

    @Value("${pagofacil.payment.method:34}")
    private String paymentMethod;

    @Value("${pagofacil.document.type:1}")
    private String documentType;

    @Value("${pagofacil.currency:2}")
    private String currency;

    public PagoFacilService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public record GeneracionQr(String qrBase64, String transactionId, LocalDateTime expiracion) {
    }

    public record EstadoPagoFacil(int paymentStatus, boolean pagado, boolean cancelado, boolean expirado) {
    }

    public GeneracionQr generarQr(
        String clientName,
        String documentId,
        String phoneNumber,
        String email,
        String paymentNumber,
        String productName,
        BigDecimal montoReal
    ) {
        if (paymentNumber == null || paymentNumber.isBlank()) {
            throw new IllegalArgumentException("paymentNumber es obligatorio.");
        }

        BigDecimal amount = (montoTope != null && montoTope.compareTo(BigDecimal.ZERO) > 0 && montoReal != null)
            ? montoReal.min(montoTope)
            : montoReal;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El monto a cobrar debe ser mayor a 0.");
        }

        String token = login();
        QrRequest request = buildQrRequest(clientName, documentId, phoneNumber, email, paymentNumber, productName, amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("Response-Language", "es");

        String body = postForString(
            baseUrl + "/generate-qr",
            headers,
            request,
            "No se pudo generar el QR de Pago Fácil."
        );

        String qrBase64 = extractQrBase64(body);
        if (qrBase64 == null || qrBase64.isBlank()) {
            logger.error("Respuesta de generate-qr sin código QR; claves recibidas: {}", clavesDeRespuesta(body));
            throw new IllegalStateException("No se obtuvo el código QR de Pago Fácil.");
        }

        String transactionId = extractTransactionId(body);
        LocalDateTime expiracion = extractExpiracion(body);
        return new GeneracionQr(qrBase64, transactionId, expiracion);
    }

    public EstadoPagoFacil consultarEstado(String transactionId, String referencia) {
        String token = login();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("Response-Language", "es");

        QueryRequest request = new QueryRequest();
        request.pagofacilTransactionId = transactionId;
        request.companyTransactionId = referencia;

        String body = postForString(
            baseUrl + "/query-transaction",
            headers,
            request,
            "No se pudo consultar el estado del pago en Pago Fácil."
        );

        int status = extractPaymentStatus(body);
        boolean pagado = status == 2 || status == 5;
        boolean cancelado = status == 3;
        boolean expirado = status == 4;
        return new EstadoPagoFacil(status, pagado, cancelado, expirado);
    }

    private String login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("tcTokenSecret", tokenSecret);
        headers.set("tcTokenService", tokenService);

        String body = postForString(baseUrl + "/login", headers, "{}", "No se pudo autenticar con Pago Fácil.");

        String token = extractToken(body);
        if (token == null || token.isBlank()) {
            logger.error("Respuesta de login de Pago Fácil sin accessToken; claves recibidas: {}", clavesDeRespuesta(body));
            throw new IllegalStateException("No se obtuvo el token de acceso de Pago Fácil.");
        }
        return token;
    }

    private QrRequest buildQrRequest(
        String clientNameInput,
        String documentIdOverride,
        String phoneNumberInput,
        String emailInput,
        String paymentNumber,
        String productNameInput,
        BigDecimal amount
    ) {
        String clientName = blankToDefault(clientNameInput, "Proveedor");
        String documentId = blankToDefault(documentIdOverride, "12345678");
        String phoneNumber = blankToDefault(phoneNumberInput, "70000000");
        String email = blankToDefault(emailInput, "contacto@tiendasjunior.local");
        String productName = blankToDefault(productNameInput, "Pago a proveedor");

        double montoDouble = amount.doubleValue();
        List<OrderDetail> orderDetail = new ArrayList<>();
        orderDetail.add(new OrderDetail(1, productName, 1, montoDouble, 0.0, montoDouble));

        QrRequest request = new QrRequest();
        request.paymentMethod = parseIntegerOrDefault(paymentMethod, 34);
        request.clientName = clientName;
        request.documentType = parseIntegerOrDefault(documentType, 1);
        request.documentId = documentId;
        request.phoneNumber = phoneNumber;
        request.email = email;
        request.paymentNumber = paymentNumber;
        request.amount = montoDouble;
        request.currency = parseIntegerOrDefault(currency, 2);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            request.callbackUrl = callbackUrl;
        }
        request.orderDetail = orderDetail;
        return request;
    }

    private Integer parseIntegerOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String blankToDefault(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private String postForString(String url, HttpHeaders headers, Object request, String errorMessage) {
        String jsonBody;
        try {
            jsonBody = request instanceof String stringBody ? stringBody : objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar la solicitud a Pago Fácil.", ex);
        }

        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            return restTemplate.postForEntity(url, entity, String.class).getBody();
        } catch (HttpStatusCodeException ex) {
            logger.error("Pago Fácil respondió con error: status={}, body={}",
                ex.getStatusCode(), safeBody(ex.getResponseBodyAsString()));
            throw new IllegalStateException(errorMessage, ex);
        } catch (RestClientException ex) {
            logger.error("Fallo de comunicación con Pago Fácil", ex);
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    private String extractToken(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return null;
        }
        JsonNode values = root.path("values");
        String token = textOrNull(values, "accessToken");
        if (token == null) token = textOrNull(values, "token");
        if (token == null) token = textOrNull(root, "accessToken");
        if (token == null) token = textOrNull(root, "token");
        return token;
    }

    private String extractQrBase64(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return null;
        }
        JsonNode values = root.path("values");
        String qr = textOrNull(values, "qrBase64");
        if (qr == null) qr = textOrNull(values, "qrImage");
        if (qr == null) qr = textOrNull(root, "qrBase64");
        if (qr == null) qr = textOrNull(root, "qrImage");
        if (qr != null) {
            qr = qr.replace("\\/", "/").replaceAll("[^A-Za-z0-9+/=]", "");
        }
        return qr;
    }

    private String extractTransactionId(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return null;
        }
        JsonNode values = root.path("values");
        String id = textOrNull(values, "transactionId");
        if (id == null) id = textOrNull(root, "transactionId");
        return id;
    }

    private LocalDateTime extractExpiracion(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return null;
        }
        JsonNode values = root.path("values");
        String fecha = textOrNull(values, "expirationDate");
        if (fecha == null) fecha = textOrNull(root, "expirationDate");
        return parseFecha(fecha);
    }

    private int extractPaymentStatus(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return -1;
        }
        JsonNode values = root.path("values");
        JsonNode statusNode = values.path("paymentStatus");
        if (statusNode.isMissingNode() || statusNode.isNull()) {
            statusNode = root.path("paymentStatus");
        }
        if (statusNode.isMissingNode() || statusNode.isNull()) {
            return -1;
        }
        if (statusNode.isNumber()) {
            return statusNode.asInt();
        }
        try {
            return Integer.parseInt(statusNode.asText().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private LocalDateTime parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : FORMATOS_FECHA) {
            try {
                return LocalDateTime.parse(fecha.trim(), formatter);
            } catch (DateTimeParseException ex) {

            }
        }
        logger.warn("No se pudo interpretar la fecha de expiración del QR: {}", fecha);
        return null;
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            logger.error("No se pudo interpretar la respuesta de Pago Fácil", ex);
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return (text != null && !text.isBlank()) ? text : null;
    }

    private String safeBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) + "..." : trimmed;
    }

    private String clavesDeRespuesta(String body) {
        JsonNode root = parse(body);
        if (root == null) {
            return "(vacío o no interpretable)";
        }
        List<String> claves = new ArrayList<>();
        root.fieldNames().forEachRemaining(claves::add);
        JsonNode values = root.path("values");
        if (values.isObject()) {
            values.fieldNames().forEachRemaining(nombre -> claves.add("values." + nombre));
        }
        return claves.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QrRequest {
        @JsonProperty("paymentMethod")
        private Integer paymentMethod;

        @JsonProperty("clientName")
        private String clientName;

        @JsonProperty("documentType")
        private Integer documentType;

        @JsonProperty("documentId")
        private String documentId;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("email")
        private String email;

        @JsonProperty("paymentNumber")
        private String paymentNumber;

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency")
        private Integer currency;

        @JsonProperty("callbackUrl")
        private String callbackUrl;

        @JsonProperty("orderDetail")
        private List<OrderDetail> orderDetail;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QueryRequest {
        @JsonProperty("pagofacilTransactionId")
        private String pagofacilTransactionId;

        @JsonProperty("companyTransactionId")
        private String companyTransactionId;
    }

    private static class OrderDetail {
        @JsonProperty("serial")
        private final Integer serial;

        @JsonProperty("product")
        private final String product;

        @JsonProperty("quantity")
        private final Integer quantity;

        @JsonProperty("price")
        private final Double price;

        @JsonProperty("discount")
        private final Double discount;

        @JsonProperty("total")
        private final Double total;

        private OrderDetail(Integer serial, String product, Integer quantity,
                             Double price, Double discount, Double total) {
            this.serial = serial;
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.discount = discount;
            this.total = total;
        }
    }
}
