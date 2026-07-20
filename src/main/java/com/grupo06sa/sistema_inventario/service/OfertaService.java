package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfertaService {
    private final ProveedorProductoRepository ofertaRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final SecurityService securityService;

    public OfertaService(
        ProveedorProductoRepository ofertaRepository,
        ProductoRepository productoRepository,
        ProveedorRepository proveedorRepository,
        SecurityService securityService
    ) {
        this.ofertaRepository = ofertaRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.securityService = securityService;
    }

    public List<ProveedorProducto> listarPorProveedor(Long proveedorId) {
        return ofertaRepository.findByProveedorId(proveedorId);
    }

    @Transactional
    public ProveedorProducto crear(
        Long proveedorId,
        Long productoId,
        BigDecimal costo,
        Integer tiempoReposicionDias,
        BigDecimal cantidadMinima
    ) {
        if (ofertaRepository.existsByProveedorIdAndProductoId(proveedorId, productoId)) {
            throw new IllegalStateException("Ya existe una oferta tuya para este producto.");
        }

        Proveedor proveedor = proveedorRepository.findById(proveedorId)
            .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe."));
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));

        ProveedorProducto oferta = new ProveedorProducto();
        oferta.setProveedor(proveedor);
        oferta.setProducto(producto);
        oferta.setCostoUnitarioActual(costo);
        oferta.setTiempoReposicionDias(tiempoReposicionDias != null ? tiempoReposicionDias : 0);
        oferta.setCantidadMinimaPedido(cantidadMinima != null ? cantidadMinima : BigDecimal.ONE);
        oferta.setDisponible(true);
        oferta.setCostoActualizadoAt(LocalDateTime.now());
        return ofertaRepository.save(oferta);
    }

    @Transactional
    public ProveedorProducto actualizar(
        Long ofertaId,
        BigDecimal costo,
        Integer tiempoReposicionDias,
        BigDecimal cantidadMinima,
        Boolean disponible,
        ContextoAutenticado ctx
    ) {
        ProveedorProducto oferta = obtenerConOwnership(ofertaId, ctx);

        if (costo != null) {
            oferta.setCostoUnitarioAnterior(oferta.getCostoUnitarioActual());
            oferta.setCostoUnitarioActual(costo);
            oferta.setCostoActualizadoAt(LocalDateTime.now());
        }
        if (tiempoReposicionDias != null) {
            oferta.setTiempoReposicionDias(tiempoReposicionDias);
        }
        if (cantidadMinima != null) {
            oferta.setCantidadMinimaPedido(cantidadMinima);
        }
        if (disponible != null) {
            oferta.setDisponible(disponible);
        }
        return ofertaRepository.save(oferta);
    }

    @Transactional
    public void eliminar(Long ofertaId, ContextoAutenticado ctx) {
        ProveedorProducto oferta = obtenerConOwnership(ofertaId, ctx);
        ofertaRepository.delete(oferta);
    }

    public List<ProveedorProducto> compararOfertas(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new IllegalArgumentException("El producto no existe.");
        }

        return ofertaRepository.findByProductoId(productoId).stream()
            .sorted(Comparator.comparing(ProveedorProducto::getCostoUnitarioActual))
            .toList();
    }

    private ProveedorProducto obtenerConOwnership(Long ofertaId, ContextoAutenticado ctx) {
        ProveedorProducto oferta = ofertaRepository.findById(ofertaId)
            .orElseThrow(() -> new IllegalArgumentException("La oferta solicitada no existe."));
        securityService.assertOwnership(oferta.getProveedor().getId(), ctx);
        return oferta;
    }
}
