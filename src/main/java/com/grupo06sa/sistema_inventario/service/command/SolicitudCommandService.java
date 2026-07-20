package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.SolicitudCompraService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SolicitudCommandService {
    private final SolicitudCompraService solicitudCompraService;

    public SolicitudCommandService(SolicitudCompraService solicitudCompraService) {
        this.solicitudCompraService = solicitudCompraService;
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        Long proveedorId = parseLong(params.get(0));
        Long productoId = parseLong(params.get(1));
        BigDecimal cantidad = parseBigDecimal(params.get(2));
        Long almacenId = parseLong(params.get(3));

        SolicitudCompra solicitud = solicitudCompraService.crear(proveedorId, productoId, cantidad, almacenId, ctx.getUsuario());
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate(
            "Solicitud registrada",
            "Se registró la solicitud de compra #" + solicitud.getId() + " y se notificó al proveedor."
        ));
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<SolicitudCompra> solicitudes = solicitudCompraService.listar(ctx);
        if (solicitudes.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Solicitudes de compra", "No hay solicitudes registradas."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildSolicitudesTable(solicitudes));
    }

    public CommandResult atender(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        BigDecimal costoOfrecido = parseBigDecimal(params.get(1));
        solicitudCompraService.atender(id, costoOfrecido, ctx);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Solicitud atendida", "La solicitud fue marcada como ATENDIDA."));
    }

    public CommandResult rechazar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        String motivo = params.get(1);
        solicitudCompraService.rechazar(id, motivo, ctx);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Solicitud rechazada", "La solicitud fue marcada como RECHAZADA."));
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

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar un valor numérico válido.");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor numérico proporcionado no es válido.");
        }
    }
}
