package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrasladoService {
    private static final Logger logger = LoggerFactory.getLogger(TrasladoService.class);

    private final SecurityService securityService;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final InventarioService inventarioService;

    public TrasladoService(
        SecurityService securityService,
        ProductoRepository productoRepository,
        AlmacenRepository almacenRepository,
        InventarioService inventarioService
    ) {
        this.securityService = securityService;
        this.productoRepository = productoRepository;
        this.almacenRepository = almacenRepository;
        this.inventarioService = inventarioService;
    }

    @Transactional
    public String trasladarStock(java.util.List<String> params, String emailRemitente) {
        Usuario usuario = authenticateAdmin(emailRemitente);
        if (usuario == null) {
            return buildAdminAccessError(emailRemitente);
        }

        if (params == null || params.size() < 4) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id_producto, id_almacen_origen, id_almacen_destino y cantidad."
            );
        }

        Long productoId = parseLong(params.get(0));
        Long almacenOrigenId = parseLong(params.get(1));
        Long almacenDestinoId = parseLong(params.get(2));
        Integer cantidad = parseInteger(params.get(3));

        if (productoId == null || almacenOrigenId == null || almacenDestinoId == null || cantidad == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Datos del traslado no son validos.");
        }

        if (cantidad <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "La cantidad debe ser mayor a 0.");
        }

        if (almacenOrigenId.equals(almacenDestinoId)) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Operacion no valida",
                "El almacen de origen y destino deben ser diferentes."
            );
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        Optional<Almacen> origenOpt = almacenRepository.findById(almacenOrigenId);
        Optional<Almacen> destinoOpt = almacenRepository.findById(almacenDestinoId);
        if (origenOpt.isEmpty() || destinoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El almacen solicitado no existe.");
        }

        Producto producto = productoOpt.get();
        int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
        if (stockActual < cantidad) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Stock insuficiente",
                "Stock insuficiente para realizar el traslado."
            );
        }

        try {
            double costoUnitario = producto.getPrecio() != null ? producto.getPrecio() : 0.0;
            inventarioService.registrarTraslado(
                usuario,
                producto,
                origenOpt.get(),
                destinoOpt.get(),
                cantidad,
                costoUnitario,
                LocalDateTime.now()
            );

            StringBuilder detail = new StringBuilder();
            detail.append("Traslado registrado correctamente.\n")
                .append("Producto: ").append(safe(producto.getNombre())).append("\n")
                .append("Cantidad: ").append(cantidad).append("\n")
                .append("Origen: ").append(safe(origenOpt.get().getNombre())).append("\n")
                .append("Destino: ").append(safe(destinoOpt.get().getNombre()));

            return HtmlBuilderUtil.buildPlainTemplate("Traslado", detail.toString());
        } catch (Exception ex) {
            logger.error("Failed to register traslado", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar el traslado.");
        }
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
