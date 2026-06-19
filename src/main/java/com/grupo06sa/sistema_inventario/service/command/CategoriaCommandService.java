package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
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
public class CategoriaCommandService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaCommandService.class);

    private final SecurityService securityService;
    private final CategoriaRepository categoriaRepository;

    public CategoriaCommandService(SecurityService securityService, CategoriaRepository categoriaRepository) {
        this.securityService = securityService;
        this.categoriaRepository = categoriaRepository;
    }

    public String listarCategorias(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        List<Categoria> categorias = categoriaRepository.findAll();
        if (categorias.isEmpty()) {
            return HtmlBuilderUtil.buildInfoTemplate("Catalogo", "No hay categorias registradas.");
        }

        return HtmlBuilderUtil.buildCategoriasTable(categorias);
    }

    public String obtenerCategoria(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id de la categoria.");
        }

        Long categoriaId = parseLong(params.get(0));
        if (categoriaId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id de la categoria no es valido.");
        }

        Optional<Categoria> categoriaOpt = categoriaRepository.findById(categoriaId);
        if (categoriaOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "La categoria solicitada no existe.");
        }

        Categoria categoria = categoriaOpt.get();
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(categoria.getId()).append("\n")
            .append("Nombre: ").append(safe(categoria.getNombre())).append("\n")
            .append("Imagen: ").append(safe(categoria.getImagen()));

        return HtmlBuilderUtil.buildPlainTemplate("Categoria", detail.toString());
    }

    public String insertarCategoria(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 2) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para registrar categoria."
            );
        }

        try {
            Categoria categoria = new Categoria();
            categoria.setNombre(params.get(0));
            categoria.setImagen(params.get(1));
            categoriaRepository.save(categoria);
            return HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Categoria registrada correctamente.");
        } catch (Exception ex) {
            logger.error("Failed to insert categoria", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar la categoria.");
        }
    }

    public String actualizarCategoria(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 3) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para actualizar categoria."
            );
        }

        Long categoriaId = parseLong(params.get(0));
        if (categoriaId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id de la categoria no es valido.");
        }

        Optional<Categoria> categoriaOpt = categoriaRepository.findById(categoriaId);
        if (categoriaOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "La categoria solicitada no existe.");
        }

        try {
            Categoria categoria = categoriaOpt.get();
            categoria.setNombre(params.get(1));
            categoria.setImagen(params.get(2));
            categoriaRepository.save(categoria);
            return HtmlBuilderUtil.buildSuccessTemplate("Actualizacion completa", "Categoria actualizada correctamente.");
        } catch (Exception ex) {
            logger.error("Failed to update categoria", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo actualizar la categoria.");
        }
    }

    public String eliminarCategoria(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id de la categoria.");
        }

        Long categoriaId = parseLong(params.get(0));
        if (categoriaId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id de la categoria no es valido.");
        }

        Optional<Categoria> categoriaOpt = categoriaRepository.findById(categoriaId);
        if (categoriaOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "La categoria solicitada no existe.");
        }

        try {
            categoriaRepository.delete(categoriaOpt.get());
            return HtmlBuilderUtil.buildSuccessTemplate("Eliminacion completa", "Categoria eliminada correctamente.");
        } catch (DataIntegrityViolationException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Operacion no permitida",
                "No se puede eliminar la categoria porque esta relacionada con otros registros."
            );
        } catch (Exception ex) {
            logger.error("Failed to delete categoria", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo eliminar la categoria.");
        }
    }

    private String validateReadAccess(String emailRemitente) {
        try {
            Usuario usuario = securityService.authenticateAndCheckRole(emailRemitente, null);
            if (!isAdminOrCliente(usuario)) {
                return HtmlBuilderUtil.buildErrorTemplate(
                    "Privilegios insuficientes",
                    "Solo un ADMINISTRADOR o CLIENTE puede ejecutar este comando."
                );
            }
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
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

    private boolean isAdminOrCliente(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return false;
        }

        String roleName = usuario.getRol().getNombre();
        return "ADMINISTRADOR".equalsIgnoreCase(roleName) || "CLIENTE".equalsIgnoreCase(roleName);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
