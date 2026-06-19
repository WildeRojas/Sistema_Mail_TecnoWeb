package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProveedorCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProveedorCommandService.class);

    private final SecurityService securityService;
    private final ProveedorRepository proveedorRepository;

    public ProveedorCommandService(
        SecurityService securityService,
        ProveedorRepository proveedorRepository
    ) {
        this.securityService = securityService;
        this.proveedorRepository = proveedorRepository;
    }

    public String listarProveedores(String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        List<Proveedor> proveedores = proveedorRepository.findAll();
        if (proveedores.isEmpty()) {
            return HtmlBuilderUtil.buildInfoTemplate("Proveedores", "No hay proveedores registrados.");
        }

        return HtmlBuilderUtil.buildProveedoresTable(proveedores);
    }

    public String obtenerProveedor(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del proveedor.");
        }

        Long proveedorId = parseLong(params.get(0));
        if (proveedorId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del proveedor no es valido.");
        }

        Optional<Proveedor> proveedorOpt = proveedorRepository.findById(proveedorId);
        if (proveedorOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El proveedor solicitado no existe.");
        }

        Proveedor p = proveedorOpt.get();
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(p.getId()).append("\n")
            .append("Nombre: ").append(safe(p.getNombre())).append("\n")
            .append("Telefono: ").append(safe(p.getTelefono())).append("\n")
            .append("Correo: ").append(safe(p.getCorreo())).append("\n")
            .append("Direccion: ").append(safe(p.getDireccion()));

        return HtmlBuilderUtil.buildPlainTemplate("Proveedor", detail.toString());
    }

    public String insertarProveedor(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 4) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar: nombre, telefono, correo, direccion."
            );
        }

        try {
            Proveedor proveedor = new Proveedor();
            proveedor.setNombre(params.get(0));
            proveedor.setTelefono(params.get(1));
            proveedor.setCorreo(params.get(2));
            proveedor.setDireccion(params.get(3));

            proveedorRepository.save(proveedor);

            StringBuilder detail = new StringBuilder();
            detail.append("Proveedor registrado correctamente.\n")
                .append("ID: ").append(proveedor.getId()).append("\n")
                .append("Nombre: ").append(safe(proveedor.getNombre())).append("\n")
                .append("Correo: ").append(safe(proveedor.getCorreo()));

            return HtmlBuilderUtil.buildSuccessTemplate("Proveedor registrado", detail.toString());
        } catch (Exception ex) {
            logger.error("Failed to insert proveedor", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar el proveedor.");
        }
    }

    public String actualizarProveedor(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 5) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar: id, nombre, telefono, correo, direccion."
            );
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
            Proveedor proveedor = proveedorOpt.get();
            proveedor.setNombre(params.get(1));
            proveedor.setTelefono(params.get(2));
            proveedor.setCorreo(params.get(3));
            proveedor.setDireccion(params.get(4));

            proveedorRepository.save(proveedor);
            return HtmlBuilderUtil.buildSuccessTemplate("Proveedor actualizado", "Proveedor actualizado correctamente.");
        } catch (Exception ex) {
            logger.error("Failed to update proveedor", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo actualizar el proveedor.");
        }
    }

    public String eliminarProveedor(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del proveedor.");
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
            proveedorRepository.delete(proveedorOpt.get());
            return HtmlBuilderUtil.buildSuccessTemplate("Proveedor eliminado", "Proveedor eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Operacion no permitida",
                "No se puede eliminar el proveedor porque tiene productos o compras asociadas."
            );
        } catch (Exception ex) {
            logger.error("Failed to delete proveedor", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo eliminar el proveedor.");
        }
    }

    private String validateAdminAccess(String emailRemitente) {
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
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
