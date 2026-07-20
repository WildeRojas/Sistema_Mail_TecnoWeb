package com.grupo06sa.sistema_inventario.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.grupo06sa.sistema_inventario.service.PagoService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PagoFacilWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(PagoFacilWebhookController.class);
    private static final PagoFacilWebhookResponse OK_RESPONSE = new PagoFacilWebhookResponse();
    private static final int ESTADO_PAGADO = 2;

    private final PagoService pagoService;

    public PagoFacilWebhookController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/pagos/pagofacil/callback")
    public ResponseEntity<PagoFacilWebhookResponse> callback(
        @RequestBody(required = false) Map<String, Object> payload
    ) {
        try {
            procesar(payload);
        } catch (RuntimeException ex) {
            logger.error("Fallo interno al procesar el callback de Pago Fácil", ex);
        }
        return ResponseEntity.ok(OK_RESPONSE);
    }

    private void procesar(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            logger.info("Callback de Pago Fácil recibido sin cuerpo.");
            return;
        }

        Integer estado = asInt(payload.getOrDefault("Estado", payload.get("estado")));
        logger.info("Callback de Pago Fácil recibido. Estado={}, campos={}", estado, payload.size());
        if (estado == null || estado != ESTADO_PAGADO) {
            return;
        }

        String referencia = firstNonBlank(
            asString(payload.get("PedidoID")),
            asString(payload.get("pedidoId")),
            asString(payload.get("paymentNumber"))
        );

        String datosSanitizados = "estado=" + estado + ";campos=" + payload.keySet();

        if (referencia != null) {
            pagoService.marcarPagadoPorReferencia(referencia, datosSanitizados);
            return;
        }

        String transaccion = asString(payload.get("transaccion"));
        if (transaccion != null) {
            pagoService.marcarPagadoPorTransactionId(transaccion, datosSanitizados);
        }
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @JsonPropertyOrder({"error", "status", "message", "values"})
    public static class PagoFacilWebhookResponse {
        @JsonProperty("error")
        private final int error = 0;

        @JsonProperty("status")
        private final int status = 1;

        @JsonProperty("message")
        private final String message = "Notificación recibida";

        @JsonProperty("values")
        private final boolean values = true;

        public int getError() {
            return error;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValues() {
            return values;
        }
    }
}
