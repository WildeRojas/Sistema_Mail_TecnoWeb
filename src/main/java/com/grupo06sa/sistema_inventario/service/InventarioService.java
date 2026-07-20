package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.EstadoAlerta;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlertaStockRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
import com.grupo06sa.sistema_inventario.repository.MovimientoInventarioRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TipoOperacionRepository tipoOperacionRepository;
    private final AlertaStockRepository alertaStockRepository;

    public InventarioService(
        InventarioRepository inventarioRepository,
        MovimientoInventarioRepository movimientoRepository,
        TipoOperacionRepository tipoOperacionRepository,
        AlertaStockRepository alertaStockRepository
    ) {
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoOperacionRepository = tipoOperacionRepository;
        this.alertaStockRepository = alertaStockRepository;
    }

    public BigDecimal stockTotal(Producto producto) {
        return inventarioRepository.findByProductoId(producto.getId()).stream()
            .map(Inventario::getCantidad)
            .filter(cantidad -> cantidad != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal ocupacion(Almacen almacen) {
        return inventarioRepository.findByAlmacenId(almacen.getId()).stream()
            .map(Inventario::getCantidad)
            .filter(cantidad -> cantidad != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public MovimientoInventario registrarIngreso(
        Producto producto,
        Almacen almacen,
        BigDecimal cantidad,
        Compra compra,
        Usuario usuario,
        String observacion
    ) {
        validarPositiva(cantidad);
        validarCapacidad(almacen, cantidad);

        Inventario inventario = obtenerOCrear(producto, almacen);
        inventario.setCantidad(inventario.getCantidad().add(cantidad));
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = nuevoMovimiento(producto, cantidad, usuario, observacion);
        movimiento.setTipoOperacion(resolveTipoOperacion("INGRESO"));
        movimiento.setAlmacenDestino(almacen);
        movimiento.setCompra(compra);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario registrarSalida(
        Producto producto,
        Almacen almacen,
        BigDecimal cantidad,
        Usuario usuario,
        String observacion
    ) {
        validarPositiva(cantidad);

        Inventario inventario = obtenerOCrear(producto, almacen);
        if (inventario.getCantidad().compareTo(cantidad) < 0) {
            throw new IllegalStateException(
                "Stock insuficiente de " + safe(producto.getNombre()) + " en " + safe(almacen.getNombre()) + "."
            );
        }

        inventario.setCantidad(inventario.getCantidad().subtract(cantidad));
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = nuevoMovimiento(producto, cantidad, usuario, observacion);
        movimiento.setTipoOperacion(resolveTipoOperacion("SALIDA"));
        movimiento.setAlmacenOrigen(almacen);
        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        evaluarAlerta(producto, almacen);
        return guardado;
    }

    @Transactional
    public MovimientoInventario registrarAjuste(
        Producto producto,
        Almacen almacen,
        BigDecimal delta,
        Usuario usuario,
        String observacion
    ) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("El ajuste debe tener una cantidad distinta de cero.");
        }

        Inventario inventario = obtenerOCrear(producto, almacen);
        BigDecimal resultante = inventario.getCantidad().add(delta);
        if (resultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El ajuste dejaría stock negativo para " + safe(producto.getNombre()) + ".");
        }
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            validarCapacidad(almacen, delta);
        }

        inventario.setCantidad(resultante);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = nuevoMovimiento(producto, delta.abs(), usuario, observacion);
        movimiento.setTipoOperacion(resolveTipoOperacion("AJUSTE"));
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            movimiento.setAlmacenDestino(almacen);
        } else {
            movimiento.setAlmacenOrigen(almacen);
        }
        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            evaluarAlerta(producto, almacen);
        }
        return guardado;
    }

    @Transactional
    public MovimientoInventario trasladar(
        Producto producto,
        Almacen origen,
        Almacen destino,
        BigDecimal cantidad,
        Usuario usuario
    ) {
        if (origen == null || destino == null || origen.getId() == null || destino.getId() == null) {
            throw new IllegalStateException("Debe indicar almacén de origen y destino válidos.");
        }
        if (origen.getId().equals(destino.getId())) {
            throw new IllegalStateException("El almacén de origen y destino deben ser diferentes.");
        }
        validarPositiva(cantidad);

        Inventario inventarioOrigen = obtenerOCrear(producto, origen);
        if (inventarioOrigen.getCantidad().compareTo(cantidad) < 0) {
            throw new IllegalStateException("Stock insuficiente en " + safe(origen.getNombre()) + " para trasladar.");
        }
        validarCapacidad(destino, cantidad);

        inventarioOrigen.setCantidad(inventarioOrigen.getCantidad().subtract(cantidad));
        inventarioRepository.save(inventarioOrigen);

        Inventario inventarioDestino = obtenerOCrear(producto, destino);
        inventarioDestino.setCantidad(inventarioDestino.getCantidad().add(cantidad));
        inventarioRepository.save(inventarioDestino);

        MovimientoInventario movimiento = nuevoMovimiento(producto, cantidad, usuario, null);
        movimiento.setTipoOperacion(resolveTipoOperacion("TRASLADO"));
        movimiento.setAlmacenOrigen(origen);
        movimiento.setAlmacenDestino(destino);
        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        evaluarAlerta(producto, origen);
        return guardado;
    }

    private void evaluarAlerta(Producto producto, Almacen almacenContexto) {
        BigDecimal stockMinimo = producto.getStockMinimo();
        if (stockMinimo == null) {
            return;
        }

        BigDecimal total = stockTotal(producto);
        if (total.compareTo(stockMinimo) > 0) {
            return;
        }

        List<AlertaStock> pendientes = alertaStockRepository
            .findByProductoIdAndEstado(producto.getId(), EstadoAlerta.PENDIENTE);
        if (!pendientes.isEmpty()) {
            AlertaStock existente = pendientes.get(0);
            existente.setCantidadActual(total);
            alertaStockRepository.save(existente);
            return;
        }

        AlertaStock alerta = new AlertaStock();
        alerta.setProducto(producto);
        alerta.setAlmacen(almacenContexto);
        alerta.setCantidadActual(total);
        alerta.setStockMinimo(stockMinimo);
        alerta.setEstado(EstadoAlerta.PENDIENTE);
        alerta.setCreatedAt(LocalDateTime.now());
        alertaStockRepository.save(alerta);
    }

    private void validarCapacidad(Almacen almacen, BigDecimal cantidadAdicional) {
        BigDecimal capacidad = almacen.getCapacidad();
        if (capacidad == null || capacidad.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal ocupacionActual = ocupacion(almacen);
        if (ocupacionActual.add(cantidadAdicional).compareTo(capacidad) > 0) {
            throw new IllegalStateException(
                "La operación excede la capacidad del almacén " + safe(almacen.getNombre()) + "."
            );
        }
    }

    private Inventario obtenerOCrear(Producto producto, Almacen almacen) {
        return inventarioRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
            .orElseGet(() -> {
                Inventario nuevo = new Inventario();
                nuevo.setProducto(producto);
                nuevo.setAlmacen(almacen);
                nuevo.setCantidad(BigDecimal.ZERO);
                return nuevo;
            });
    }

    private MovimientoInventario nuevoMovimiento(
        Producto producto,
        BigDecimal cantidad,
        Usuario usuario,
        String observacion
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUsuario(usuario);
        movimiento.setObservacion(observacion);
        return movimiento;
    }

    private TipoOperacion resolveTipoOperacion(String nombre) {
        return tipoOperacionRepository.findByNombreIgnoreCase(nombre)
            .orElseThrow(() -> new IllegalStateException("No existe el tipo de operación " + nombre + "."));
    }

    private void validarPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La cantidad debe ser mayor a 0.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
