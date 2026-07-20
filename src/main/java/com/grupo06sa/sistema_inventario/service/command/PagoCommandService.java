package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.EstadoPago;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.PagoService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PagoCommandService {
    private final PagoService pagoService;

    public PagoCommandService(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    public CommandResult pagar(ContextoAutenticado ctx, List<String> params) {
        Long compraId = parseLong(params.get(0));
        Pago pago = pagoService.generarQrCompra(compraId);

        if (pago.getQrImagen() == null || pago.getQrImagen().isBlank()) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                "QR no disponible", "No se pudo obtener la imagen del código QR."
            ));
        }

        byte[] qrBytes = Base64.getDecoder().decode(pago.getQrImagen());
        EmailAttachment attachment = new EmailAttachment("qr.png", "image/png", qrBytes);
        String body = HtmlBuilderUtil.buildQrTemplate(
            "Pago de compra #" + compraId,
            "Escanea este código con tu aplicación bancaria para pagar al proveedor. Monto: Bs. " + plain(pago.getMonto())
                + ". Referencia: " + safe(pago.getReferencia()) + ".",
            null
        );
        return CommandResult.withAttachment(body, attachment);
    }

    public CommandResult verificar(ContextoAutenticado ctx, List<String> params) {
        Long compraId = parseLong(params.get(0));
        Pago pago = pagoService.verificar(compraId, ctx);
        String estado = pago.getEstado() == EstadoPago.PAGADO ? "PAGADO" : "PENDIENTE";
        return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate(
            "Estado del pago", "El pago de la compra #" + compraId + " está " + estado + "."
        ));
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Pago> pagos = pagoService.listar(ctx);
        if (pagos.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Pagos", "No hay pagos registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildPagosTable(pagos));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar un id numérico válido.");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El id proporcionado no es válido.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
