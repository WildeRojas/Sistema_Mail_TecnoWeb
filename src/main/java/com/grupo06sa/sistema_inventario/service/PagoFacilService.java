package com.grupo06sa.sistema_inventario.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo06sa.sistema_inventario.entity.DetallePedido;
import com.grupo06sa.sistema_inventario.entity.Pedido;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmailSenderService emailSenderService;

    @Value("${pagofacil.url.base}")
    private String baseUrl;

    @Value("${pagofacil.token.service}")
    private String tokenService;

    @Value("${pagofacil.token.secret}")
    private String tokenSecret;

    @Value("${pagofacil.callback.url:}")
    private String callbackUrl;

    // FIX: asegúrate de tener pagofacil.payment.method=34 en application.properties
    @Value("${pagofacil.payment.method:34}")
    private String paymentMethod;

    // FIX: documentType debe ser numérico, no "CI"
    @Value("${pagofacil.document.type:1}")
    private String documentType;

    @Value("${pagofacil.currency:2}")
    private String currency;

    public PagoFacilService(EmailSenderService emailSenderService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.emailSenderService = emailSenderService;
    }

    public String login() {
        // FIX: La API solo requiere las credenciales en el HEADER, no en el body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Los nombres de header deben ser exactamente estos (case-insensitive en HTTP
        // pero
        // algunos servidores son sensibles — usar exactamente como la doc indica)
        headers.set("tcTokenSecret", tokenSecret);
        headers.set("tcTokenService", tokenService);

        // Body vacío — la doc solo pide header para el login
        String body = postForString(
                baseUrl + "/login",
                headers,
                "{}", // Body vacío como JSON
                "No se pudo autenticar con Pago Facil.");

        String token = extractToken(body);
        if (token == null || token.isBlank()) {
            logger.error("Pago Facil login response without accessToken: {}", safeBody(body));
            throw new IllegalStateException("No se obtuvo accessToken de Pago Facil.");
        }

        logger.info("Pago Facil login exitoso. Token obtenido.");
        return token;
    }

    public void listarMetodosHabilitados() {
        String token = login();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Response-Language", "es");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(new EmptyBody(), headers);
        try {
            String body = restTemplate.postForEntity(
                    baseUrl + "/list-enabled-services", entity, String.class).getBody();
            logger.info("METODOS HABILITADOS: {}", body);
        } catch (Exception ex) {
            logger.error("Error listando métodos: {}", ex.getMessage());
        }
    }

    public String generarQr(Pedido pedido) {
        return generarQr(pedido, null, null);
    }

    public String generarQr(Pedido pedido, String documentIdOverride, String clientCodeOverride) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido es nulo.");
        }
        listarMetodosHabilitados();
        String token = login();
        QrRequest request = buildQrRequest(pedido, documentIdOverride, clientCodeOverride);

        // FIX: Log del request para debuggear fácilmente
        try {
            logger.info("Pago Facil generate-qr REQUEST body: {}", objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            logger.warn("No se pudo serializar el request para logging");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        // FIX: Agregar idioma español para mensajes más claros en los errores
        headers.set("Response-Language", "es");

        String body = postForString(
                baseUrl + "/generate-qr",
                headers,
                request,
                "No se pudo generar el QR de Pago Facil.");

        logger.info("Pago Facil generate-qr RESPONSE: {}", safeBody(body));

        String qrBase64 = extractQrBase64(body);
        if (qrBase64 == null || qrBase64.isBlank()) {
            logger.error("Pago Facil generate-qr response without qrBase64: {}", safeBody(body));
            throw new IllegalStateException("No se obtuvo qrBase64 de Pago Facil.");
        }

        // enviarQrPorEmail(pedido, qrBase64);  // eliminado: el correo lo envía PedidoService

        return qrBase64;
    }

    private QrRequest buildQrRequest(Pedido pedido, String documentIdOverride, String clientCodeOverride) {
        Usuario usuario = pedido.getUsuario();
        String clientName = buildClientName(usuario);
        String documentId = resolveDocumentId(documentIdOverride, usuario);
        String clientCode = resolveClientCode(clientCodeOverride, usuario);
        String phoneNumber = usuario != null ? safe(usuario.getTelefono()) : "";
        String email = usuario != null ? safe(usuario.getEmail()) : "";

        if (phoneNumber.isBlank()) {
            phoneNumber = "70000000";
        }
        if (email.isBlank()) {
            email = "cliente@demo.com";
        }

        List<OrderDetail> orderDetail = new ArrayList<>();
        String productName = "Detalle_Item";
        List<DetallePedido> detalles = pedido.getDetallesPedido();
        if (detalles != null && !detalles.isEmpty()) {
            Producto producto = detalles.get(0).getProducto();
            if (producto != null) {
                productName = safe(producto.getNombre());
            }
        }

        double amount = 0.10;
        orderDetail.add(new OrderDetail(1, productName, 1, amount, 0.0, amount));

        QrRequest request = new QrRequest();
        request.paymentMethod = parseIntegerOrDefault(paymentMethod, 34, "paymentMethod");
        request.clientName = clientName;
        request.documentType = parseIntegerOrDefault(documentType, 1, "documentType");
        request.documentId = documentId;
        request.phoneNumber = phoneNumber;
        request.email = email;
        // FIX: paymentNumber único usando UUID corto para evitar duplicados en
        // reintentos
        request.paymentNumber = buildPaymentNumber(pedido);
        request.amount = amount;
        request.currency = parseIntegerOrDefault(currency, 2, "currency");
        request.clientCode = clientCode;
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            request.callbackUrl = callbackUrl;
        }
        request.orderDetail = orderDetail;
        return request;
    }

    private String buildPaymentNumber(Pedido pedido) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(LocalDateTime.now());
        if (pedido != null && pedido.getId() != null) {
            return timestamp + pedido.getId();
        }
        return timestamp + "0";
    }

    private String resolveDocumentId(String override, Usuario usuario) {
        String cleaned = safe(override).trim();
        if (!cleaned.isBlank())
            return cleaned;

        // Usar teléfono primero (ya tiene 8 dígitos, seguro válido)
        String telefono = usuario != null ? safe(usuario.getTelefono()).replaceAll("\\D", "").trim() : "";
        if (!telefono.isBlank())
            return telefono;

        return "12345678";
    }

    private String resolveClientCode(String override, Usuario usuario) {
        String cleaned = safe(override).trim();
        if (!cleaned.isBlank()) {
            return cleaned;
        }

        if (usuario != null && usuario.getId() != null) {
            return "11" + String.format("%03d", usuario.getId());
        }

        return "11001";
    }

    private Integer parseIntegerOrDefault(String value, int defaultValue, String fieldName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            logger.warn("Invalid Pago Facil {} value '{}', using default {}", fieldName, value, defaultValue);
            return defaultValue;
        }
    }

    private String buildClientName(Usuario usuario) {
        if (usuario == null) {
            return "Cliente";
        }

        String nombre = safe(usuario.getNombre());
        String apellido = safe(usuario.getApellido());
        String fullName = (nombre + " " + apellido).trim();
        return fullName.isBlank() ? "Cliente" : fullName;
    }

    private String postForString(String url, HttpHeaders headers, Object request, String errorMessage) {
        String jsonBody;
        try {
            // Si ya es String, usarlo directamente
            if (request instanceof String) {
                jsonBody = (String) request;
            } else {
                jsonBody = objectMapper.writeValueAsString(request);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error serializando request para: " + url, ex);
        }

        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            return restTemplate.postForEntity(url, entity, String.class).getBody();
        } catch (HttpStatusCodeException ex) {
            logger.error("Pago Facil request failed: status={}, body={}",
                    ex.getStatusCode(), safeBody(ex.getResponseBodyAsString()));
            throw new IllegalStateException(errorMessage, ex);
        } catch (RestClientException ex) {
            logger.error("Pago Facil request failed", ex);
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    private String extractToken(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode values = root.path("values");
            String token = textOrNull(values, "accessToken");
            if (token == null)
                token = textOrNull(values, "token");
            if (token == null)
                token = textOrNull(root, "accessToken");
            if (token == null)
                token = textOrNull(root, "token");
            return token;
        } catch (Exception ex) {
            logger.error("Failed to parse Pago Facil login response", ex);
            return null;
        }
    }

    private String extractQrBase64(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode values = root.path("values");
            String qr = textOrNull(values, "qrBase64");
            if (qr == null)
                qr = textOrNull(root, "qrBase64");
            if (qr != null) {
                qr = qr.replace("\\/", "/").replaceAll("[^A-Za-z0-9+/=]", "");
            }
            return qr;
        } catch (Exception ex) {
            logger.error("Failed to parse Pago Facil QR response", ex);
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode())
            return null;
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull())
            return null;
        String text = value.asText();
        return (text != null && !text.isBlank()) ? text : null;
    }

    private String safeBody(String body) {
        if (body == null)
            return "";
        String trimmed = body.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) + "..." : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // Body vacío para el POST de login (credenciales van solo en header)
    private static class EmptyBody {
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

        @JsonProperty("clientCode")
        private String clientCode;

        @JsonProperty("callbackUrl")
        private String callbackUrl;

        @JsonProperty("orderDetail")
        private List<OrderDetail> orderDetail;
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