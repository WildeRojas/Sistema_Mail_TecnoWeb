package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.MovimientoInventarioRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class InventarioService {
    private final TipoOperacionRepository tipoOperacionRepository;
    private final AlmacenRepository almacenRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoRepository productoRepository;

    public InventarioService(
        TipoOperacionRepository tipoOperacionRepository,
        AlmacenRepository almacenRepository,
        MovimientoInventarioRepository movimientoInventarioRepository,
        ProductoRepository productoRepository
    ) {
        this.tipoOperacionRepository = tipoOperacionRepository;
        this.almacenRepository = almacenRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.productoRepository = productoRepository;
    }

    public void registrarEntrada(
        Usuario usuario,
        Producto producto,
        int cantidad,
        double costoUnitario,
        LocalDateTime fecha
    ) {
        validarMovimiento(producto, cantidad);
        TipoOperacion tipo = resolveTipoOperacion("INGRESO");
        Almacen almacen = resolveAlmacen();

        int stockActual = safeStock(producto.getStockActual());
        producto.setStockActual(stockActual + cantidad);
        productoRepository.save(producto);

        guardarMovimiento(usuario, producto, cantidad, costoUnitario, fecha, tipo, almacen);
    }

    public void registrarSalida(
        Usuario usuario,
        Producto producto,
        int cantidad,
        double costoUnitario,
        LocalDateTime fecha
    ) {
        validarMovimiento(producto, cantidad);
        TipoOperacion tipo = resolveTipoOperacion("SALIDA");
        Almacen almacen = resolveAlmacen();

        int stockActual = safeStock(producto.getStockActual());
        if (stockActual < cantidad) {
            throw new IllegalStateException("Stock insuficiente para " + safe(producto.getNombre()) + ".");
        }

        producto.setStockActual(stockActual - cantidad);
        productoRepository.save(producto);

        guardarMovimiento(usuario, producto, cantidad, costoUnitario, fecha, tipo, almacen);
    }

    public void registrarTraslado(
        Usuario usuario,
        Producto producto,
        Almacen origen,
        Almacen destino,
        int cantidad,
        double costoUnitario,
        LocalDateTime fecha
    ) {
        validarMovimiento(producto, cantidad);
        if (origen == null || destino == null || origen.getId() == null || destino.getId() == null) {
            throw new IllegalStateException("Almacen invalido para traslado.");
        }

        TipoOperacion tipo = resolveTipoOperacion("TRASLADO");
        // Registra dos movimientos para rastrear origen y destino del traslado.
        guardarMovimiento(usuario, producto, -cantidad, costoUnitario, fecha, tipo, origen);
        guardarMovimiento(usuario, producto, cantidad, costoUnitario, fecha, tipo, destino);
    }

    private void validarMovimiento(Producto producto, int cantidad) {
        if (producto == null || producto.getId() == null) {
            throw new IllegalStateException("Producto invalido para movimiento de inventario.");
        }
        if (cantidad <= 0) {
            throw new IllegalStateException("Cantidad invalida para movimiento de inventario.");
        }
    }

    private TipoOperacion resolveTipoOperacion(String nombre) {
        return tipoOperacionRepository.findByNombreIgnoreCase(nombre)
            .orElseThrow(() -> new IllegalStateException(
                "No existe tipo de operacion " + nombre + "."
            ));
    }

    private Almacen resolveAlmacen() {
        return almacenRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay almacen configurado."));
    }

    private void guardarMovimiento(
        Usuario usuario,
        Producto producto,
        int cantidad,
        double costoUnitario,
        LocalDateTime fecha,
        TipoOperacion tipoOperacion,
        Almacen almacen
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setFechaMovimiento(fecha);
        movimiento.setCostoUnitario(costoUnitario);
        movimiento.setCantidad(cantidad);
        movimiento.setUsuario(usuario);
        movimiento.setProducto(producto);
        movimiento.setAlmacen(almacen);
        movimiento.setTipoOperacion(tipoOperacion);
        movimientoInventarioRepository.save(movimiento);
    }

    private int safeStock(Integer value) {
        return value != null ? value : 0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
