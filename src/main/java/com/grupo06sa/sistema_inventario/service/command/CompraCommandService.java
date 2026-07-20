package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.service.CompraService;
import com.grupo06sa.sistema_inventario.service.EmailSenderService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraCommandService {
    private static final Logger logger = LoggerFactory.getLogger(CompraCommandService.class);

    private final CompraService compraService;
    private final EmailSenderService emailSenderService;
    private final UsuarioRepository usuarioRepository;

    public CompraCommandService(
        CompraService compraService,
        EmailSenderService emailSenderService,
        UsuarioRepository usuarioRepository
    ) {
        this.compraService = compraService;
        this.emailSenderService = emailSenderService;
        this.usuarioRepository = usuarioRepository;
    }

    public CommandResult crear(ContextoAutenticado ctx, List<String> params) {
        Long proveedorId = parseLong(params.get(0));
        Long almacenId = parseLong(params.get(1));
        Compra compra = compraService.crear(proveedorId, almacenId);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate(
            "Compra creada", "Se creó la compra #" + compra.getId() + " en estado PENDIENTE."
        ));
    }

    public CommandResult agregarDetalle(ContextoAutenticado ctx, List<String> params) {
        Long compraId = parseLong(params.get(0));
        Long ofertaId = parseLong(params.get(1));
        BigDecimal cantidad = parseBigDecimal(params.get(2));
        DetalleCompra detalle = compraService.agregarDetalle(compraId, ofertaId, cantidad);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate(
            "Detalle agregado",
            "Se agregó " + safe(detalle.getProducto().getNombre()) + " x" + plain(cantidad) + " a la compra #" + compraId + "."
        ));
    }

    public CommandResult finalizar(ContextoAutenticado ctx, List<String> params) {
        Long compraId = parseLong(params.get(0));
        Compra compra = compraService.finalizar(compraId);
        notificarProveedorOrden(compra);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate(
            "Compra finalizada",
            "La compra #" + compraId + " quedó en estado EN_ESPERA y se notificó al proveedor. Total: " + plain(compra.getTotal())
        ));
    }

    public CommandResult recibir(ContextoAutenticado ctx, List<String> params) {
        Long compraId = parseLong(params.get(0));
        Compra compra = compraService.recibir(compraId, ctx.getUsuario());
        notificarRecepcion(compra);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate(
            "Compra recibida",
            "La compra #" + compraId + " quedó en estado RECIBIDA y el inventario fue actualizado."
        ));
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Compra> compras = compraService.listar(ctx);
        if (compras.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Compras", "No hay compras registradas."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildComprasTable(compras));
    }

    @Transactional(readOnly = true)
    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Compra compra = compraService.obtener(id, ctx);
        List<DetalleCompra> detalles = compraService.obtenerDetalles(id);
        return CommandResult.text(HtmlBuilderUtil.buildCompraDetalleTemplate(compra, detalles));
    }

    private void notificarProveedorOrden(Compra compra) {
        Proveedor proveedor = compra.getProveedor();
        if (proveedor == null || proveedor.getCorreo() == null || proveedor.getCorreo().isBlank()) {
            return;
        }
        try {
            List<DetalleCompra> detalles = compraService.obtenerDetalles(compra.getId());
            String html = HtmlBuilderUtil.buildOrdenCompraTemplate(compra, detalles);
            emailSenderService.sendEmail(
                proveedor.getCorreo(),
                "Nueva orden de compra #" + compra.getId() + " - Tiendas Junior",
                html
            );
        } catch (RuntimeException ex) {
            logger.warn("No se pudo notificar al proveedor sobre la compra #{}", compra.getId(), ex);
        }
    }

    private void notificarRecepcion(Compra compra) {
        String html = HtmlBuilderUtil.buildSuccessTemplate(
            "Compra recibida",
            "La compra #" + compra.getId() + " del proveedor "
                + (compra.getProveedor() != null ? safe(compra.getProveedor().getNombre()) : "")
                + " fue recibida y el inventario fue actualizado."
        );
        for (Usuario propietario : usuarioRepository.findByRolNombreIgnoreCaseAndActivoTrue(RolNombre.PROPIETARIO.name())) {
            if (propietario.getEmail() == null || propietario.getEmail().isBlank()) {
                continue;
            }
            try {
                emailSenderService.sendEmail(propietario.getEmail(), "Compra recibida #" + compra.getId(), html);
            } catch (RuntimeException ex) {
                logger.warn("No se pudo notificar al propietario {} sobre la compra #{}", propietario.getEmail(), compra.getId(), ex);
            }
        }
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
