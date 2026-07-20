package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.AlmacenService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlmacenCommandService {
    private final AlmacenService almacenService;

    public AlmacenCommandService(AlmacenService almacenService) {
        this.almacenService = almacenService;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Almacen> almacenes = almacenService.listar();
        if (almacenes.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Almacenes", "Sin almacenes registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildAlmacenesTable(almacenes));
    }

    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Almacen almacen = almacenService.obtener(id);
        BigDecimal ocupacion = almacenService.ocupacion(id);

        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(almacen.getId()).append("\n")
            .append("Nombre: ").append(safe(almacen.getNombre())).append("\n")
            .append("Capacidad: ").append(plain(almacen.getCapacidad())).append("\n")
            .append("Ocupación actual: ").append(plain(ocupacion)).append("\n")
            .append("Direccion: ").append(safe(almacen.getDireccion())).append("\n")
            .append("Coordenadas: ").append(safe(almacen.getCoordenadasGps()));

        return CommandResult.text(HtmlBuilderUtil.buildPlainTemplate("Almacén", detail.toString()));
    }

    public CommandResult crear(ContextoAutenticado ctx, List<String> params) {
        String nombre = params.get(0);
        BigDecimal capacidad = parseBigDecimal(params.get(1));
        String direccion = params.get(2);
        String coordenadas = params.get(3);

        almacenService.crear(nombre, capacidad, direccion, coordenadas);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Almacén registrado correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        String nombre = params.get(1);
        BigDecimal capacidad = parseBigDecimal(params.get(2));
        String direccion = params.get(3);
        String coordenadas = params.get(4);

        almacenService.actualizar(id, nombre, capacidad, direccion, coordenadas);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Actualización completa", "Almacén actualizado correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        almacenService.eliminar(id);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Eliminación completa", "Almacén eliminado correctamente."));
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
