package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.OfertaService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OfertaCommandService {
    private final OfertaService ofertaService;

    public OfertaCommandService(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<ProveedorProducto> ofertas = ofertaService.listarPorProveedor(ctx.getProveedorId());
        if (ofertas.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Mis ofertas", "Aún no registraste ofertas."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildOfertasTable(ofertas));
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        Long productoId = parseLong(params.get(0));
        BigDecimal costo = parseBigDecimal(params.get(1));
        Integer tiempoReposicion = params.size() >= 3 && !params.get(2).isBlank() ? parseInt(params.get(2)) : null;
        BigDecimal cantidadMinima = params.size() >= 4 && !params.get(3).isBlank() ? parseBigDecimal(params.get(3)) : null;

        ofertaService.crear(ctx.getProveedorId(), productoId, costo, tiempoReposicion, cantidadMinima);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Oferta registrada", "Tu oferta se registró correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Long ofertaId = parseLong(params.get(0));
        BigDecimal costo = parseBigDecimal(params.get(1));
        Integer tiempoReposicion = params.size() >= 3 && !params.get(2).isBlank() ? parseInt(params.get(2)) : null;
        BigDecimal cantidadMinima = params.size() >= 4 && !params.get(3).isBlank() ? parseBigDecimal(params.get(3)) : null;
        Boolean disponible = params.size() >= 5 && !params.get(4).isBlank() ? parseDisponible(params.get(4)) : null;

        ofertaService.actualizar(ofertaId, costo, tiempoReposicion, cantidadMinima, disponible, ctx);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Oferta actualizada", "Tu oferta se actualizó correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Long ofertaId = parseLong(params.get(0));
        ofertaService.eliminar(ofertaId, ctx);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Oferta eliminada", "Tu oferta se eliminó correctamente."));
    }

    public CommandResult comparar(ContextoAutenticado ctx, List<String> params) {
        Long productoId = parseLong(params.get(0));
        List<ProveedorProducto> ofertas = ofertaService.compararOfertas(productoId);
        if (ofertas.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Comparación de ofertas", "No hay ofertas registradas para este producto."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildComparacionOfertasTable(ofertas));
    }

    private Boolean parseDisponible(String value) {
        String normalizado = value.trim().toUpperCase(Locale.ROOT);
        if ("SI".equals(normalizado) || "SÍ".equals(normalizado)) {
            return Boolean.TRUE;
        }
        if ("NO".equals(normalizado)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("El campo disponible debe ser SI o NO.");
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

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor numérico proporcionado no es válido.");
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
