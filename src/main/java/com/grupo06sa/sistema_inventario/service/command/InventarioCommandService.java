package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
import com.grupo06sa.sistema_inventario.repository.MovimientoInventarioRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.InventarioService;
import com.grupo06sa.sistema_inventario.service.TrasladoService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class InventarioCommandService {
    private final InventarioService inventarioService;
    private final TrasladoService trasladoService;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;

    public InventarioCommandService(
        InventarioService inventarioService,
        TrasladoService trasladoService,
        InventarioRepository inventarioRepository,
        MovimientoInventarioRepository movimientoRepository,
        ProductoRepository productoRepository,
        AlmacenRepository almacenRepository
    ) {
        this.inventarioService = inventarioService;
        this.trasladoService = trasladoService;
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.almacenRepository = almacenRepository;
    }

    public CommandResult listarInventario(ContextoAutenticado ctx, List<String> params) {
        List<Inventario> existencias = inventarioRepository.findAll();
        if (existencias.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Inventario", "No hay existencias registradas."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildInventarioTable(existencias));
    }

    public CommandResult listarMovimientos(ContextoAutenticado ctx, List<String> params) {
        List<MovimientoInventario> movimientos = movimientoRepository.findAll();
        if (movimientos.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Movimientos", "No hay movimientos registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildMovimientosTable(movimientos));
    }

    public CommandResult insertarMovimiento(ContextoAutenticado ctx, List<String> params) {
        String tipo = params.get(0).trim().toUpperCase(Locale.ROOT);
        Producto producto = productoRepository.findById(parseLong(params.get(1)))
            .orElseThrow(() -> new IllegalArgumentException("El producto solicitado no existe."));
        Almacen almacen = almacenRepository.findById(parseLong(params.get(2)))
            .orElseThrow(() -> new IllegalArgumentException("El almacén solicitado no existe."));
        BigDecimal cantidad = parseBigDecimal(params.get(3));
        String observacion = params.size() >= 5 ? params.get(4) : null;

        switch (tipo) {
            case "INGRESO" -> inventarioService.registrarIngreso(
                producto, almacen, cantidad, null, ctx.getUsuario(), observacion
            );
            case "SALIDA" -> inventarioService.registrarSalida(producto, almacen, cantidad, ctx.getUsuario(), observacion);
            case "AJUSTE" -> inventarioService.registrarAjuste(producto, almacen, cantidad, ctx.getUsuario(), observacion);
            default -> throw new IllegalArgumentException("El tipo de movimiento debe ser INGRESO, SALIDA o AJUSTE.");
        }

        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Movimiento registrado", "El movimiento de inventario se registró correctamente."));
    }

    public CommandResult trasladar(ContextoAutenticado ctx, List<String> params) {
        Producto producto = productoRepository.findById(parseLong(params.get(0)))
            .orElseThrow(() -> new IllegalArgumentException("El producto solicitado no existe."));
        Almacen origen = almacenRepository.findById(parseLong(params.get(1)))
            .orElseThrow(() -> new IllegalArgumentException("El almacén de origen no existe."));
        Almacen destino = almacenRepository.findById(parseLong(params.get(2)))
            .orElseThrow(() -> new IllegalArgumentException("El almacén de destino no existe."));
        BigDecimal cantidad = parseBigDecimal(params.get(3));

        trasladoService.trasladar(producto, origen, destino, cantidad, ctx.getUsuario());
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Traslado registrado", "El traslado de stock se registró correctamente."));
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
