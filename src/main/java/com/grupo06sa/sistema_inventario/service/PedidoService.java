package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.DetallePedido;
import com.grupo06sa.sistema_inventario.entity.Estado;
import com.grupo06sa.sistema_inventario.entity.MetodoPago;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.entity.Pedido;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.DetallePedidoRepository;
import com.grupo06sa.sistema_inventario.repository.PagoRepository;
import com.grupo06sa.sistema_inventario.repository.PedidoRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private static final Logger logger = LoggerFactory.getLogger(PedidoService.class);

    private final SecurityService securityService;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;
    private final InventarioService inventarioService;
    private final PagoFacilService pagoFacilService;
    private final UsuarioRepository usuarioRepository;
    private final EmailSenderService emailSenderService;

    public PedidoService(
        SecurityService securityService,
        PedidoRepository pedidoRepository,
        DetallePedidoRepository detallePedidoRepository,
        ProductoRepository productoRepository,
        PagoRepository pagoRepository,
        InventarioService inventarioService,
        PagoFacilService pagoFacilService,
        UsuarioRepository usuarioRepository,
        EmailSenderService emailSenderService
    ) {
        this.securityService = securityService;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.pagoRepository = pagoRepository;
        this.inventarioService = inventarioService;
        this.pagoFacilService = pagoFacilService;
        this.usuarioRepository = usuarioRepository;
        this.emailSenderService = emailSenderService;
    }

    @Transactional
    public String crearPedido(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateCliente(emailRemitente);
        if (usuario == null) {
            return buildClienteAccessError(emailRemitente);
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar la direccion.");
        }

        String direccion = params.get(0);
        try {
            Pedido pedido = new Pedido();
            pedido.setFecha(LocalDateTime.now());
            pedido.setEstado(Estado.PENDIENTE);
            pedido.setTotal(0.0);
            pedido.setUsuario(usuario);
            pedidoRepository.save(pedido);

            StringBuilder detail = new StringBuilder();
            detail.append("Pedido creado correctamente.\n")
                .append("ID: ").append(pedido.getId()).append("\n")
                .append("Estado: ").append(pedido.getEstado()).append("\n")
                .append("Direccion: ").append(safe(direccion));

            return HtmlBuilderUtil.buildPlainTemplate("Pedido", detail.toString());
        } catch (Exception ex) {
            logger.error("Failed to create pedido", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo crear el pedido.");
        }
    }

    @Transactional
    public String agregarDetalle(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateCliente(emailRemitente);
        if (usuario == null) {
            return buildClienteAccessError(emailRemitente);
        }

        if (params == null || params.size() < 3) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id_pedido, id_producto y cantidad."
            );
        }

        Long pedidoId = parseLong(params.get(0));
        Long productoId = parseLong(params.get(1));
        Integer cantidad = parseInteger(params.get(2));
        if (pedidoId == null || productoId == null || cantidad == null || cantidad <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Datos del detalle no son validos.");
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El pedido solicitado no existe.");
        }

        Pedido pedido = pedidoOpt.get();
        if (!belongsToUser(pedido, usuario)) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no permitida", "El pedido no pertenece al cliente.");
        }

        if (pedido.getEstado() != Estado.PENDIENTE) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "El pedido no esta en estado PENDIENTE.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        Producto producto = productoOpt.get();
        if (producto.getPrecio() == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "El producto no tiene precio configurado.");
        }

        try {
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detallePedidoRepository.save(detalle);

            double subtotal = producto.getPrecio() * cantidad;
            double totalActual = pedido.getTotal() != null ? pedido.getTotal() : 0.0;
            pedido.setTotal(totalActual + subtotal);
            pedidoRepository.save(pedido);

            StringBuilder detailText = new StringBuilder();
            detailText.append("Detalle agregado correctamente.\n")
                .append("Pedido: ").append(pedido.getId()).append("\n")
                .append("Producto: ").append(safe(producto.getNombre())).append("\n")
                .append("Cantidad: ").append(cantidad).append("\n")
                .append("Subtotal: ").append(subtotal).append("\n")
                .append("Total pedido: ").append(pedido.getTotal());

            return HtmlBuilderUtil.buildPlainTemplate("Detalle de pedido", detailText.toString());
        } catch (Exception ex) {
            logger.error("Failed to add detalle", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo agregar el detalle.");
        }
    }

    @Transactional
    public CommandResult pagarPedido(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateCliente(emailRemitente);
        if (usuario == null) {
            return CommandResult.text(buildClienteAccessError(emailRemitente));
        }

        if (params == null || params.size() < 2) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id_pedido y metodo_pago (QR o TARJETA)."
            ));
        }

        Long pedidoId = parseLong(params.get(0));
        if (pedidoId == null) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del pedido no es valido."));
        }

        MetodoPago metodoPago = resolveMetodoPago(params.get(1));
        if (metodoPago == null) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                "Metodo de pago invalido",
                "El metodo de pago debe ser QR o TARJETA."
            ));
        }

        String documentIdOverride = params.size() > 2 ? safe(params.get(2)).trim() : null;
        if (documentIdOverride != null && documentIdOverride.isBlank()) {
            documentIdOverride = null;
        }

        String clientCodeOverride = params.size() > 3 ? safe(params.get(3)).trim() : null;
        if (clientCodeOverride != null && clientCodeOverride.isBlank()) {
            clientCodeOverride = null;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new IllegalStateException("El pedido solicitado no existe."));

        if (!belongsToUser(pedido, usuario)) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate("Operacion no permitida", "El pedido no pertenece al cliente."));
        }

        if (pedido.getEstado() != Estado.PENDIENTE) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "El pedido no esta en estado PENDIENTE."));
        }

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
        if (detalles.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate("Operacion no valida", "El pedido no tiene detalles."));
        }

        Map<Long, Producto> productos = new HashMap<>();
        Map<Long, Integer> cantidades = new HashMap<>();
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto == null || producto.getId() == null) {
                return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                    "Operacion no valida",
                    "Detalle de pedido sin producto asociado."
                ));
            }

            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            cantidades.merge(producto.getId(), cantidad, Integer::sum);
            productos.putIfAbsent(producto.getId(), producto);
        }

        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
            if (stockActual < entry.getValue()) {
                return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                    "Stock insuficiente",
                    "Stock insuficiente para " + safe(producto.getNombre()) + "."
                ));
            }
        }

        double total = calcularTotal(detalles);
        pedido.setTotal(total);
        pedido.setDetallesPedido(detalles);

        String qrBase64;
        try {
            qrBase64 = pagoFacilService.generarQr(pedido, documentIdOverride, clientCodeOverride);
        } catch (RuntimeException ex) {
            logger.error("Failed to generate Pago Facil QR", ex);
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                "Pago Facil",
                "No se pudo generar el QR de pago. Intente nuevamente."
            ));
        }

        pedido.setEstado(Estado.ESPERANDO_PAGO);
        pedidoRepository.save(pedido);

        String message = "Escanee el QR para completar el pago del pedido #" + pedido.getId()
            + ". Monto: " + total + ".";

        // DEBUG: incluir el valor crudo de qrBase64 para verificar cómo está llegando
        String debugInfo = "\n\n[DEBUG qrBase64 (primeros 200 chars)]: "
            + (qrBase64 != null ? qrBase64.substring(0, Math.min(200, qrBase64.length())) : "null")
            + "\n[Longitud total]: " + (qrBase64 != null ? qrBase64.length() : 0)
            + "\n[Caracteres inválidos detectados]: " + (qrBase64 != null
                ? (qrBase64.replaceAll("[A-Za-z0-9+/=]", "").length() > 0
                    ? "SÍ: " + qrBase64.replaceAll("[A-Za-z0-9+/=]", "").substring(0, Math.min(50, qrBase64.replaceAll("[A-Za-z0-9+/=]", "").length()))
                    : "NINGUNO")
                : "null");

        String htmlBody = HtmlBuilderUtil.buildQrTemplate("Pago Facil", message, qrBase64);

        // Agregar debug al final del HTML
        String htmlBodyWithDebug = htmlBody.replace("</body></html>",
            "<pre style=\"font-size:11px;background:#f0f0f0;padding:10px;margin:16px;word-break:break-all;\">"
            + HtmlBuilderUtil.escapeHtmlPublic(debugInfo) + "</pre></body></html>");

        try {
            byte[] qrImageBytes = Base64.getDecoder().decode(qrBase64);
            EmailAttachment attachment = new EmailAttachment("qr.png", "image/png", qrImageBytes);
            return CommandResult.withAttachment(htmlBodyWithDebug, attachment);
        } catch (IllegalArgumentException ex) {
            logger.error("QR base64 decode FAILED. Raw value (first 500): {}",
                qrBase64 != null ? qrBase64.substring(0, Math.min(500, qrBase64.length())) : "null", ex);
            return CommandResult.text(htmlBodyWithDebug);
        }
    }

    @Transactional
    public void procesarPagoWebhook(Long pedidoId, String metodoPagoRaw, String estadoRaw) {
        if (!isEstadoExitoso(estadoRaw)) {
            return;
        }

        if (pedidoId == null) {
            throw new IllegalArgumentException("PedidoID invalido.");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new IllegalStateException("El pedido solicitado no existe."));

        if (pedido.getEstado() == Estado.PAGADO) {
            return;
        }

        if (pedido.getEstado() != Estado.ESPERANDO_PAGO && pedido.getEstado() != Estado.PENDIENTE) {
            throw new IllegalStateException("Estado del pedido no permite confirmar pago.");
        }

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El pedido no tiene detalles.");
        }

        Map<Long, Producto> productos = new HashMap<>();
        Map<Long, Integer> cantidades = new HashMap<>();
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto == null || producto.getId() == null) {
                throw new IllegalStateException("Detalle de pedido sin producto asociado.");
            }

            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            cantidades.merge(producto.getId(), cantidad, Integer::sum);
            productos.putIfAbsent(producto.getId(), producto);
        }

        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
            if (stockActual < entry.getValue()) {
                throw new IllegalStateException("Stock insuficiente para " + safe(producto.getNombre()) + ".");
            }
        }

        double total = calcularTotal(detalles);
        LocalDateTime ahora = LocalDateTime.now();
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            double costoUnitario = detalle.getPrecioUnitario() != null
                ? detalle.getPrecioUnitario()
                : (producto.getPrecio() != null ? producto.getPrecio() : 0.0);
            inventarioService.registrarSalida(pedido.getUsuario(), producto, cantidad, costoUnitario, ahora);
            sendStockAlertIfNeeded(producto);
        }

        MetodoPago metodoPago = resolveMetodoPagoFlexible(metodoPagoRaw);
        Pago pago = new Pago();
        pago.setFecha(ahora);
        pago.setMonto(total);
        pago.setMetodoPago(metodoPago);
        pago.setPedido(pedido);
        pagoRepository.save(pago);

        pedido.setTotal(total);
        pedido.setEstado(Estado.PAGADO);
        pedidoRepository.save(pedido);

        enviarFacturaCliente(pedido, detalles, metodoPago);
    }

    @Transactional
    public String comprobarPago(List<String> params, String emailRemitente) {
        Usuario usuario = authenticateCliente(emailRemitente);
        if (usuario == null) {
            return buildClienteAccessError(emailRemitente);
        }

        if (params == null || params.size() < 3) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id_pedido, numero_cuenta_destinatario y monto_cancelado."
            );
        }

        Long pedidoId = parseLong(params.get(0));
        if (pedidoId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del pedido no es valido.");
        }

        String numeroCuenta = safe(params.get(1)).trim();

        Double monto = parseDouble(params.get(2));
        if (monto == null || monto <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Monto invalido",
                "El monto cancelado debe ser mayor a cero."
            );
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El pedido #" + pedidoId + " no existe.");
        }

        Pedido pedido = pedidoOpt.get();

        if (!belongsToUser(pedido, usuario)) {
            return HtmlBuilderUtil.buildErrorTemplate("Acceso denegado", "El pedido no pertenece a tu cuenta.");
        }

        if (pedido.getEstado() != Estado.ESPERANDO_PAGO) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Estado incorrecto",
                "El pedido #" + pedidoId + " está en estado " + pedido.getEstado()
                    + ". Solo se puede confirmar el pago de pedidos en estado ESPERANDO_PAGO."
            );
        }

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
        if (detalles.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Sin detalles", "El pedido no tiene productos asociados.");
        }

        // Verificar stock suficiente para cada producto antes de hacer cualquier cambio
        Map<Long, Producto> productos = new HashMap<>();
        Map<Long, Integer> cantidades = new HashMap<>();
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto == null || producto.getId() == null) {
                return HtmlBuilderUtil.buildErrorTemplate("Error interno", "Detalle de pedido sin producto asociado.");
            }
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            cantidades.merge(producto.getId(), cantidad, Integer::sum);
            productos.putIfAbsent(producto.getId(), producto);
        }

        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
            if (stockActual < entry.getValue()) {
                return HtmlBuilderUtil.buildErrorTemplate(
                    "Stock insuficiente",
                    "No hay stock suficiente para el producto \"" + safe(producto.getNombre())
                        + "\". Disponible: " + stockActual + ", requerido: " + entry.getValue() + "."
                );
            }
        }

        // Registrar salida de inventario y recolectar info de productos descontados
        LocalDateTime ahora = LocalDateTime.now();
        List<String> productosDescontados = new java.util.ArrayList<>();
        for (DetallePedido detalle : detalles) {
            Producto producto = detalle.getProducto();
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            double costoUnitario = detalle.getPrecioUnitario() != null
                ? detalle.getPrecioUnitario()
                : (producto.getPrecio() != null ? producto.getPrecio() : 0.0);
            inventarioService.registrarSalida(pedido.getUsuario(), producto, cantidad, costoUnitario, ahora);
            sendStockAlertIfNeeded(producto);
            productosDescontados.add(safe(producto.getNombre()) + " x" + cantidad);
        }

        // Registrar el pago
        Pago pago = new Pago();
        pago.setFecha(ahora);
        pago.setMonto(monto);
        pago.setMetodoPago(MetodoPago.QR);
        pago.setPedido(pedido);
        pagoRepository.save(pago);

        // Cambiar estado del pedido a PAGADO
        pedido.setEstado(Estado.PAGADO);
        pedidoRepository.save(pedido);

        logger.info("Pedido #{} marcado como PAGADO via COMPROBAR_PAGO por {}", pedidoId, emailRemitente);

        return HtmlBuilderUtil.buildComprobantePagoTemplate(pedidoId, monto, numeroCuenta, productosDescontados);
    }

    private Usuario authenticateCliente(String emailRemitente) {
        try {
            return securityService.authenticateAndCheckRole(emailRemitente, "CLIENTE");
        } catch (UserNotFoundException | RoleAccessDeniedException ex) {
            return null;
        }
    }

    private String buildClienteAccessError(String emailRemitente) {
        try {
            securityService.authenticateAndCheckRole(emailRemitente, "CLIENTE");
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
        } catch (RoleAccessDeniedException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Privilegios insuficientes",
                "Solo un CLIENTE puede ejecutar este comando."
            );
        }
    }

    private boolean belongsToUser(Pedido pedido, Usuario usuario) {
        if (pedido == null || usuario == null) {
            return false;
        }

        return pedido.getUsuario() != null && pedido.getUsuario().getId() != null
            && pedido.getUsuario().getId().equals(usuario.getId());
    }

    private MetodoPago resolveMetodoPago(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        if ("QR".equals(normalized)) {
            return MetodoPago.QR;
        }
        if ("TARJETA".equals(normalized)) {
            return MetodoPago.TARJETA;
        }

        return null;
    }

    private MetodoPago resolveMetodoPagoFlexible(String value) {
        if (value == null || value.isBlank()) {
            return MetodoPago.QR;
        }

        String normalized = value.trim().toUpperCase();
        try {
            return MetodoPago.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return MetodoPago.QR;
        }
    }

    private double calcularTotal(List<DetallePedido> detalles) {
        double total = 0.0;
        for (DetallePedido detalle : detalles) {
            double precio = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : 0.0;
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            total += precio * cantidad;
        }
        return total;
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
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void sendStockAlertIfNeeded(Producto producto) {
        if (producto == null) {
            return;
        }

        Integer stockActual = producto.getStockActual();
        Integer stockMinimo = producto.getStockMinimo();
        if (stockActual == null || stockMinimo == null) {
            return;
        }

        if (stockActual > stockMinimo) {
            return;
        }

        String body = HtmlBuilderUtil.buildInfoTemplate(
            "Alerta de stock",
            "ALERTA: El producto " + safe(producto.getNombre())
                + " ha llegado a su stock minimo (" + stockActual + "). Por favor, reabastecer."
        );

        usuarioRepository.findByRolNombreIgnoreCase("ADMINISTRADOR").forEach(admin -> {
            String email = admin.getEmail();
            if (email == null || email.isBlank()) {
                return;
            }
            try {
                emailSenderService.sendEmail(email, "Alerta de stock minimo", body);
            } catch (Exception ex) {
                logger.warn("Failed to send stock alert to {}", email, ex);
            }
        });
    }

    private void enviarFacturaCliente(Pedido pedido, List<DetallePedido> detalles, MetodoPago metodoPago) {
        if (pedido == null || pedido.getUsuario() == null) {
            return;
        }

        String email = pedido.getUsuario().getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        String body = HtmlBuilderUtil.buildFacturaPedido(pedido, detalles, metodoPago);
        try {
            emailSenderService.sendEmail(email, "Factura de pedido", body);
        } catch (Exception ex) {
            logger.warn("Failed to send invoice to {}", email, ex);
        }
    }

    private boolean isEstadoExitoso(String estadoRaw) {
        if (estadoRaw == null) {
            return false;
        }

        String normalized = estadoRaw.trim().toUpperCase();
        return "PAGADO".equals(normalized)
            || "OK".equals(normalized)
            || "SUCCESS".equals(normalized)
            || "EXITOSO".equals(normalized);
    }
}
