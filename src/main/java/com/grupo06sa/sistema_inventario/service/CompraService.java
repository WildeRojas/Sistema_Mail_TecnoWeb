package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.Estado;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.DetalleCompraRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {
    private static final Logger logger = LoggerFactory.getLogger(CompraService.class);

    private final SecurityService securityService;
    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final InventarioService inventarioService;
    private final EmailSenderService emailSenderService;
    private final UsuarioRepository usuarioRepository;

    public CompraService(
        SecurityService securityService,
        CompraRepository compraRepository,
        DetalleCompraRepository detalleCompraRepository,
        ProductoRepository productoRepository,
        ProveedorRepository proveedorRepository,
        InventarioService inventarioService,
        EmailSenderService emailSenderService,
        UsuarioRepository usuarioRepository
    ) {
        this.securityService = securityService;
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.inventarioService = inventarioService;
        this.emailSenderService = emailSenderService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public String crearCompra(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateAdmin(emailRemitente);
        if (usuario == null) {
            return buildAdminAccessError(emailRemitente);
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar id_proveedor.");
        }

        Long proveedorId = parseLong(params.get(0));
        if (proveedorId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del proveedor no es valido.");
        }

        Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
        if (proveedorOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El proveedor solicitado no existe.");
        }

        try {
            Compra compra = new Compra();
            compra.setFecha(LocalDateTime.now());
            compra.setEstado(Estado.PENDIENTE);
            compra.setTotal(0.0);
            compra.setUsuario(usuario);
            compra.setProveedor(proveedorOpt.get());
            compraRepository.save(compra);

            StringBuilder detail = new StringBuilder();
            detail.append("Compra creada correctamente.\n")
                .append("ID: ").append(compra.getId()).append("\n")
                .append("Estado: ").append(compra.getEstado()).append("\n")
                .append("Proveedor: ").append(safe(compra.getProveedor().getNombre()));

            return HtmlBuilderUtil.buildPlainTemplate("Compra", detail.toString());
        } catch (Exception ex) {
            logger.error("Failed to create compra", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo crear la compra.");
        }
    }

    @Transactional
    public String agregarDetalleCompra(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateAdmin(emailRemitente);
        if (usuario == null) {
            return buildAdminAccessError(emailRemitente);
        }

        if (params == null || params.size() < 4) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id_compra, id_producto, cantidad y precio_compra."
            );
        }

        Long compraId = parseLong(params.get(0));
        Long productoId = parseLong(params.get(1));
        Integer cantidad = parseInteger(params.get(2));
        Double precioCompra = parseDouble(params.get(3));

        if (compraId == null || productoId == null || cantidad == null || precioCompra == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Datos del detalle no son validos.");
        }
        if (cantidad <= 0 || precioCompra <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Cantidad o precio no valido.");
        }

        Optional<Compra> compraOpt = compraRepository.findById(compraId);
        if (compraOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "La compra solicitada no existe.");
        }

        Compra compra = compraOpt.get();
        if (compra.getEstado() != Estado.PENDIENTE) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "La compra no esta en estado PENDIENTE.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        try {
            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compra);
            detalle.setProducto(productoOpt.get());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioCompra);
            detalleCompraRepository.save(detalle);

            StringBuilder detailText = new StringBuilder();
            detailText.append("Detalle agregado correctamente.\n")
                .append("Compra: ").append(compra.getId()).append("\n")
                .append("Producto: ").append(safe(productoOpt.get().getNombre())).append("\n")
                .append("Cantidad: ").append(cantidad).append("\n")
                .append("Precio compra: ").append(precioCompra);

            return HtmlBuilderUtil.buildPlainTemplate("Detalle de compra", detailText.toString());
        } catch (Exception ex) {
            logger.error("Failed to add detalle compra", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo agregar el detalle.");
        }
    }

    @Transactional
    public String finalizarCompra(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateAdmin(emailRemitente);
        if (usuario == null) {
            return buildAdminAccessError(emailRemitente);
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar id_compra.");
        }

        Long compraId = parseLong(params.get(0));
        if (compraId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id de la compra no es valido.");
        }

        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalStateException("La compra solicitada no existe."));

        if (compra.getEstado() != Estado.PENDIENTE) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "La compra no esta en estado PENDIENTE.");
        }

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compra.getId());
        if (detalles.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "La compra no tiene detalles.");
        }

        double total = 0.0;
        for (DetalleCompra detalle : detalles) {
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            double precio = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : 0.0;
            if (cantidad <= 0 || precio <= 0.0) {
                return HtmlBuilderUtil.buildErrorTemplate(
                    "Operacion no valida",
                    "Detalle de compra con cantidad o precio invalido."
                );
            }
            total += cantidad * precio;
        }

        // Cambiar estado a EN_ESPERA y guardar total
        compra.setTotal(total);
        compra.setEstado(Estado.EN_ESPERA);
        compraRepository.save(compra);

        // Enviar correo al proveedor con el detalle de la orden
        Proveedor proveedor = compra.getProveedor();
        if (proveedor != null && proveedor.getCorreo() != null && !proveedor.getCorreo().isBlank()) {
            try {
                String htmlOrden = HtmlBuilderUtil.buildOrdenCompraTemplate(compra, detalles);
                String asunto = "Nueva orden de compra #" + compra.getId() + " - Tiendas Junior";
                emailSenderService.sendEmail(proveedor.getCorreo(), asunto, htmlOrden);
                logger.info("Orden de compra #{} enviada al proveedor {}", compra.getId(), proveedor.getCorreo());
            } catch (Exception ex) {
                logger.warn("No se pudo enviar correo al proveedor para compra #{}", compra.getId(), ex);
            }
        }

        StringBuilder detail = new StringBuilder();
        detail.append("Compra enviada al proveedor.\n")
            .append("ID: ").append(compra.getId()).append("\n")
            .append("Estado: ").append(compra.getEstado()).append("\n")
            .append("Total: ").append(compra.getTotal()).append("\n")
            .append("Proveedor notificado: ").append(proveedor != null ? safe(proveedor.getCorreo()) : "sin correo");

        return HtmlBuilderUtil.buildPlainTemplate("Compra", detail.toString());
    }

    @Transactional
    public String confirmarEntrega(List<String> params, String emailRemitente) {
        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar id_compra.");
        }

        Long compraId = parseLong(params.get(0));
        if (compraId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id de la compra no es valido.");
        }

        Optional<Compra> compraOpt = compraRepository.findById(compraId);
        if (compraOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "La compra #" + compraId + " no existe.");
        }

        Compra compra = compraOpt.get();

        if (compra.getEstado() == Estado.ENTREGADO) {
            return HtmlBuilderUtil.buildInfoTemplate(
                "Ya confirmada",
                "La compra #" + compraId + " ya fue confirmada como ENTREGADO anteriormente."
            );
        }

        if (compra.getEstado() == Estado.PENDIENTE) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Orden no finalizada",
                "La compra #" + compraId + " aun no ha sido finalizada por el administrador. Estado actual: PENDIENTE."
            );
        }

        if (compra.getEstado() != Estado.EN_ESPERA) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Estado incorrecto",
                "La compra #" + compraId + " está en estado " + compra.getEstado() + ". Solo se puede confirmar en estado EN_ESPERA."
            );
        }

        // Verificar que el proveedor coincida
        Proveedor proveedor = compra.getProveedor();
        if (proveedor == null || proveedor.getCorreo() == null
                || !proveedor.getCorreo().equalsIgnoreCase(emailRemitente)) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Acceso denegado",
                "Esta orden de compra no pertenece a tu cuenta de proveedor."
            );
        }

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compra.getId());

        // Obtener usuario admin para registrar movimientos (usar el usuario de la compra o el primero admin)
        Usuario usuarioMovimiento = compra.getUsuario();

        // Registrar entrada de inventario por cada detalle y acumular descripción
        LocalDateTime ahora = LocalDateTime.now();
        List<String> productosRecibidos = new java.util.ArrayList<>();
        for (DetalleCompra detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto == null) continue;
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            double precio = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : 0.0;
            // Actualizar stock
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
            producto.setStockActual(stockActual + cantidad);
            productoRepository.save(producto);
            // Registrar movimiento de inventario
            inventarioService.registrarEntrada(usuarioMovimiento, producto, cantidad, precio, ahora);
            productosRecibidos.add(safe(producto.getNombre()) + " x" + cantidad);
        }

        // Cambiar estado a ENTREGADO
        compra.setEstado(Estado.ENTREGADO);
        compraRepository.save(compra);

        logger.info("Compra #{} confirmada como ENTREGADO por proveedor {}", compraId, emailRemitente);

        // Notificar a los administradores
        final String htmlAdmin = HtmlBuilderUtil.buildSuccessTemplate(
            "Entrega confirmada",
            "El proveedor " + safe(proveedor.getNombre()) + " confirmó la entrega de la compra #" + compraId
                + ".\nProductos recibidos: " + String.join(", ", productosRecibidos)
        );
        usuarioRepository.findByRolNombreIgnoreCase("ADMINISTRADOR").forEach(admin -> {
            String emailAdmin = admin.getEmail();
            if (emailAdmin == null || emailAdmin.isBlank()) return;
            try {
                emailSenderService.sendEmail(emailAdmin, "Entrega confirmada - Compra #" + compraId, htmlAdmin);
            } catch (Exception ex) {
                logger.warn("No se pudo notificar al admin {} sobre entrega de compra #{}", emailAdmin, compraId, ex);
            }
        });

        return HtmlBuilderUtil.buildSuccessTemplate(
            "Entrega confirmada",
            "Has confirmado la entrega de la orden #" + compraId
                + ".\nProductos registrados en inventario: " + String.join(", ", productosRecibidos)
        );
    }

    public String verMisOrdenes(String emailRemitente) {
        List<Compra> ordenes = compraRepository.findByProveedorCorreoIgnoreCaseAndEstado(
            emailRemitente, Estado.EN_ESPERA
        );
        return HtmlBuilderUtil.buildOrdenComprasTable(ordenes);
    }

    private Usuario authenticateAdmin(String emailRemitente) {
        try {
            return securityService.authenticateAndCheckRole(emailRemitente, "ADMINISTRADOR");
        } catch (UserNotFoundException | RoleAccessDeniedException ex) {
            return null;
        }
    }

    private String buildAdminAccessError(String emailRemitente) {
        try {
            securityService.authenticateAndCheckRole(emailRemitente, "ADMINISTRADOR");
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
        } catch (RoleAccessDeniedException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Privilegios insuficientes",
                "Solo un ADMINISTRADOR puede ejecutar este comando."
            );
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
