package com.grupo06sa.sistema_inventario.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.grupo06sa.sistema_inventario.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagofacil")
public class PagoFacilWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(PagoFacilWebhookController.class);
    private static final PagoFacilWebhookResponse OK_RESPONSE = new PagoFacilWebhookResponse();

    private final PedidoService pedidoService;

    public PagoFacilWebhookController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<PagoFacilWebhookResponse> webhook(@RequestBody PagoFacilWebhookRequest request) {
        try {
            pedidoService.procesarPagoWebhook(
                request.getPedidoId(),
                request.getMetodoPago(),
                request.getEstado()
            );
        } catch (Exception ex) {
            logger.error("PagoFacil webhook processing failed", ex);
        }

        return ResponseEntity.ok(OK_RESPONSE);
    }

    public static class PagoFacilWebhookRequest {
        @JsonProperty("PedidoID")
        private Long pedidoId;

        @JsonProperty("Fecha")
        private String fecha;

        @JsonProperty("Hora")
        private String hora;

        @JsonProperty("MetodoPago")
        private String metodoPago;

        @JsonProperty("Estado")
        private String estado;

        public Long getPedidoId() {
            return pedidoId;
        }

        public String getFecha() {
            return fecha;
        }

        public String getHora() {
            return hora;
        }

        public String getMetodoPago() {
            return metodoPago;
        }

        public String getEstado() {
            return estado;
        }
    }

    @JsonPropertyOrder({"error", "status", "message", "values"})
    public static class PagoFacilWebhookResponse {
        @JsonProperty("error")
        private final int error = 0;

        @JsonProperty("status")
        private final int status = 1;

        @JsonProperty("message")
        private final String message = "Pago procesado exitosamente";

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
