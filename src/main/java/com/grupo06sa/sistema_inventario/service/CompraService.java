package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.DetalleCompraRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.SolicitudCompraRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {
    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final AlmacenRepository almacenRepository;
    private final ProveedorProductoRepository ofertaRepository;
    private final SolicitudCompraRepository solicitudRepository;
    private final InventarioService inventarioService;
    private final SecurityService securityService;

    public CompraService(
        CompraRepository compraRepository,
        DetalleCompraRepository detalleCompraRepository,
        ProveedorRepository proveedorRepository,
        AlmacenRepository almacenRepository,
        ProveedorProductoRepository ofertaRepository,
        SolicitudCompraRepository solicitudRepository,
        InventarioService inventarioService,
        SecurityService securityService
    ) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.almacenRepository = almacenRepository;
        this.ofertaRepository = ofertaRepository;
        this.solicitudRepository = solicitudRepository;
        this.inventarioService = inventarioService;
        this.securityService = securityService;
    }

    @Transactional
    public Compra crear(Long proveedorId, Long almacenId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
            .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe."));
        Almacen almacen = almacenRepository.findById(almacenId)
            .orElseThrow(() -> new IllegalArgumentException("El almacén no existe."));

        boolean tieneOfertas = ofertaRepository.findByProveedorId(proveedorId).stream()
            .anyMatch(ProveedorProducto::isDisponible);
        if (!tieneOfertas) {
            throw new IllegalStateException("El proveedor no tiene ofertas disponibles.");
        }

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setAlmacen(almacen);
        compra.setEstado(EstadoCompra.PENDIENTE);
        compra.setTotal(BigDecimal.ZERO);
        compra.setFecha(LocalDateTime.now());
        compra.setInventarioIngresado(false);

        List<SolicitudCompra> atendidas =
            solicitudRepository.findByProveedorIdAndEstado(proveedorId, EstadoSolicitud.ATENDIDA);
        if (atendidas.size() == 1) {
            compra.setSolicitud(atendidas.get(0));
        }

        Compra guardada = compraRepository.save(compra);
        if (guardada.getSolicitud() != null) {
            SolicitudCompra solicitud = guardada.getSolicitud();
            solicitud.setEstado(EstadoSolicitud.CONVERTIDA);
            solicitudRepository.save(solicitud);
        }
        return guardada;
    }

    @Transactional
    public DetalleCompra agregarDetalle(Long compraId, Long ofertaId, BigDecimal cantidad) {
        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalStateException("La compra no está en estado PENDIENTE.");
        }

        ProveedorProducto oferta = ofertaRepository.findById(ofertaId)
            .orElseThrow(() -> new IllegalArgumentException("La oferta solicitada no existe."));
        if (!oferta.getProveedor().getId().equals(compra.getProveedor().getId())) {
            throw new IllegalStateException("La oferta no pertenece al proveedor de esta compra.");
        }
        if (!oferta.isDisponible()) {
            throw new IllegalStateException("La oferta seleccionada no está disponible.");
        }

        DetalleCompra detalle = new DetalleCompra();
        detalle.setCompra(compra);
        detalle.setProducto(oferta.getProducto());
        detalle.setProveedorProducto(oferta);
        detalle.setCantidad(cantidad);
        detalle.setCostoUnitario(oferta.getCostoUnitarioActual());
        detalle.setSubtotal(oferta.getCostoUnitarioActual().multiply(cantidad));
        return detalleCompraRepository.save(detalle);
    }

    @Transactional
    public Compra finalizar(Long compraId) {
        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalStateException("La compra no está en estado PENDIENTE.");
        }

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compraId);
        if (detalles.isEmpty()) {
            throw new IllegalStateException("La compra no tiene detalles.");
        }

        BigDecimal total = detalles.stream()
            .map(DetalleCompra::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        compra.setTotal(total);
        compra.setEstado(EstadoCompra.EN_ESPERA);
        return compraRepository.save(compra);
    }

    @Transactional
    public Compra recibir(Long compraId, Usuario usuario) {
        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));

        if (compra.isInventarioIngresado()) {
            return compra;
        }
        if (compra.getEstado() != EstadoCompra.EN_ESPERA) {
            throw new IllegalStateException("Solo se puede recibir una compra en estado EN_ESPERA.");
        }

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compraId);
        for (DetalleCompra detalle : detalles) {
            inventarioService.registrarIngreso(
                detalle.getProducto(),
                compra.getAlmacen(),
                detalle.getCantidad(),
                compra,
                usuario,
                "Recepción de compra #" + compraId
            );
        }

        compra.setInventarioIngresado(true);
        compra.setEstado(EstadoCompra.RECIBIDA);
        return compraRepository.save(compra);
    }

    public List<Compra> listar(ContextoAutenticado ctx) {
        if (ctx.getRol() == RolNombre.PROVEEDOR) {
            return compraRepository.findByProveedorId(ctx.getProveedorId());
        }
        return compraRepository.findAll();
    }

    public Compra obtener(Long id, ContextoAutenticado ctx) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));
        securityService.assertOwnership(compra.getProveedor().getId(), ctx);
        return compra;
    }

    public List<DetalleCompra> obtenerDetalles(Long compraId) {
        return detalleCompraRepository.findByCompraId(compraId);
    }
}
