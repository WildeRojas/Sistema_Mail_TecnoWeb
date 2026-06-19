package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlmacenService {
    private static final Logger logger = LoggerFactory.getLogger(AlmacenService.class);

    private final SecurityService securityService;
    private final AlmacenRepository almacenRepository;

    public AlmacenService(SecurityService securityService, AlmacenRepository almacenRepository) {
        this.securityService = securityService;
        this.almacenRepository = almacenRepository;
    }

    public String listarAlmacenes(String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        List<Almacen> almacenes = almacenRepository.findAll();
        if (almacenes.isEmpty()) {
            return HtmlBuilderUtil.buildInfoTemplate("Almacenes", "Sin almacenes registrados.");
        }

        return HtmlBuilderUtil.buildAlmacenesTable(almacenes);
    }

    public String obtenerAlmacen(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del almacen.");
        }

        Long almacenId = parseLong(params.get(0));
        if (almacenId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del almacen no es valido.");
        }

        Optional<Almacen> almacenOpt = almacenRepository.findById(almacenId);
        if (almacenOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El almacen solicitado no existe.");
        }

        Almacen almacen = almacenOpt.get();
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(almacen.getId()).append("\n")
            .append("Nombre: ").append(safe(almacen.getNombre())).append("\n")
            .append("Capacidad: ").append(safeNumber(almacen.getCapacidad())).append("\n")
            .append("Direccion: ").append(safe(almacen.getDireccion())).append("\n")
            .append("Coordenadas: ").append(safe(almacen.getCoordenadasGps()));

        return HtmlBuilderUtil.buildPlainTemplate("Almacen", detail.toString());
    }

    @Transactional
    public String crearAlmacen(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 4) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar nombre, capacidad, direccion y coordenadas_gps."
            );
        }

        String nombre = params.get(0);
        Integer capacidad = parseInteger(params.get(1));
        String direccion = params.get(2);
        String coordenadas = params.get(3);

        if (capacidad == null || capacidad <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "La capacidad debe ser mayor a 0.");
        }

        try {
            Almacen almacen = new Almacen();
            almacen.setNombre(nombre);
            almacen.setCapacidad(capacidad);
            almacen.setDireccion(direccion);
            almacen.setCoordenadasGps(coordenadas);
            almacenRepository.save(almacen);

            return HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Almacen registrado.");
        } catch (Exception ex) {
            logger.error("Failed to create almacen", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar el almacen.");
        }
    }

    @Transactional
    public String actualizarAlmacen(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 5) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar id, nombre, capacidad, direccion y coordenadas_gps."
            );
        }

        Long almacenId = parseLong(params.get(0));
        if (almacenId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del almacen no es valido.");
        }

        Optional<Almacen> almacenOpt = almacenRepository.findById(almacenId);
        if (almacenOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El almacen solicitado no existe.");
        }

        Integer capacidad = parseInteger(params.get(2));
        if (capacidad == null || capacidad <= 0) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "La capacidad debe ser mayor a 0.");
        }

        try {
            Almacen almacen = almacenOpt.get();
            almacen.setNombre(params.get(1));
            almacen.setCapacidad(capacidad);
            almacen.setDireccion(params.get(3));
            almacen.setCoordenadasGps(params.get(4));
            almacenRepository.save(almacen);

            return HtmlBuilderUtil.buildSuccessTemplate("Actualizacion completa", "Almacen actualizado.");
        } catch (Exception ex) {
            logger.error("Failed to update almacen", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo actualizar el almacen.");
        }
    }

    @Transactional
    public String eliminarAlmacen(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del almacen.");
        }

        Long almacenId = parseLong(params.get(0));
        if (almacenId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del almacen no es valido.");
        }

        Optional<Almacen> almacenOpt = almacenRepository.findById(almacenId);
        if (almacenOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El almacen solicitado no existe.");
        }

        try {
            almacenRepository.delete(almacenOpt.get());
            return HtmlBuilderUtil.buildSuccessTemplate("Eliminacion completa", "Almacen eliminado.");
        } catch (Exception ex) {
            logger.error("Failed to delete almacen", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo eliminar el almacen.");
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

    private String safeNumber(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }
}
