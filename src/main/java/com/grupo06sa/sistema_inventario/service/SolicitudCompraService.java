package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.SolicitudCompraRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudCompraService {
    private static final Logger logger = LoggerFactory.getLogger(SolicitudCompraService.class);

    private final SolicitudCompraRepository solicitudRepository;
    private final ProveedorProductoRepository ofertaRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final AlmacenRepository almacenRepository;
    private final SecurityService securityService;
    private final EmailSenderService emailSenderService;

    public SolicitudCompraService(
        SolicitudCompraRepository solicitudRepository,
        ProveedorProductoRepository ofertaRepository,
        ProductoRepository productoRepository,
        ProveedorRepository proveedorRepository,
        AlmacenRepository almacenRepository,
        SecurityService securityService,
        EmailSenderService emailSenderService
    ) {
        this.solicitudRepository = solicitudRepository;
        this.ofertaRepository = ofertaRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.almacenRepository = almacenRepository;
        this.securityService = securityService;
        this.emailSenderService = emailSenderService;
    }

    public SolicitudCompra crear(Long proveedorId, Long productoId, BigDecimal cantidad, Long almacenId, Usuario solicitante) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
            .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe."));
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));
        Almacen almacen = almacenRepository.findById(almacenId)
            .orElseThrow(() -> new IllegalArgumentException("El almacén no existe."));

        ProveedorProducto oferta = ofertaRepository.findByProveedorIdAndProductoId(proveedorId, productoId)
            .filter(ProveedorProducto::isDisponible)
            .orElseThrow(() -> new IllegalStateException(
                "El proveedor no tiene una oferta disponible para este producto."
            ));

        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setProveedor(proveedor);
        solicitud.setProducto(producto);
        solicitud.setAlmacen(almacen);
        solicitud.setCantidad(cantidad);
        solicitud.setCostoEstimado(oferta.getCostoUnitarioActual().multiply(cantidad));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setSolicitante(solicitante);
        solicitud.setCreatedAt(LocalDateTime.now());

        SolicitudCompra guardada = solicitudRepository.save(solicitud);
        notificarProveedor(guardada);
        return guardada;
    }

    private void notificarProveedor(SolicitudCompra solicitud) {
        Proveedor proveedor = solicitud.getProveedor();
        if (proveedor == null || proveedor.getCorreo() == null || proveedor.getCorreo().isBlank()) {
            return;
        }

        try {
            String html = HtmlBuilderUtil.buildSolicitudCompraTemplate(solicitud);
            emailSenderService.sendEmail(
                proveedor.getCorreo(),
                "Nueva solicitud de compra #" + solicitud.getId(),
                html
            );
        } catch (RuntimeException ex) {
            logger.warn("No se pudo notificar al proveedor sobre la solicitud #{}", solicitud.getId(), ex);
        }
    }

    public List<SolicitudCompra> listar(ContextoAutenticado ctx) {
        if (ctx.getRol() == RolNombre.PROVEEDOR) {
            return solicitudRepository.findByProveedorId(ctx.getProveedorId());
        }
        return solicitudRepository.findAll();
    }

    @Transactional
    public SolicitudCompra atender(Long id, BigDecimal costoOfrecido, ContextoAutenticado ctx) {
        SolicitudCompra solicitud = obtenerConOwnership(id, ctx);
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue procesada.");
        }

        solicitud.setCostoOfrecido(costoOfrecido);
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitud.setAtendidaPor(ctx.getUsuario());
        solicitud.setAtendidaAt(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public SolicitudCompra rechazar(Long id, String motivo, ContextoAutenticado ctx) {
        SolicitudCompra solicitud = obtenerConOwnership(id, ctx);
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue procesada.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setObservacion(motivo);
        solicitud.setAtendidaPor(ctx.getUsuario());
        solicitud.setAtendidaAt(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    private SolicitudCompra obtenerConOwnership(Long id, ContextoAutenticado ctx) {
        SolicitudCompra solicitud = solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La solicitud solicitada no existe."));
        securityService.assertOwnership(solicitud.getProveedor().getId(), ctx);
        return solicitud;
    }
}
