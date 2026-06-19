package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Rol;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
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
public class UsuarioCommandService {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioCommandService.class);

    private final SecurityService securityService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioCommandService(
        SecurityService securityService,
        UsuarioRepository usuarioRepository,
        RolRepository rolRepository
    ) {
        this.securityService = securityService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    public String buildHelp(String emailRemitente) {
        String roleName = "CLIENTE";
        try {
            Usuario usuario = securityService.authenticateAndCheckRole(emailRemitente, null);
            if (usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null) {
                roleName = usuario.getRol().getNombre();
            }
        } catch (UserNotFoundException ex) {
            roleName = "CLIENTE";
        }

        return HtmlBuilderUtil.buildHelpTemplate(roleName);
    }

    public String listarUsuarios(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            return HtmlBuilderUtil.buildInfoTemplate("Usuarios", "Sin usuarios registrados.");
        }

        return HtmlBuilderUtil.buildUsuariosTable(usuarios);
    }

    public String obtenerUsuario(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del usuario.");
        }

        Long usuarioId = parseLong(params.get(0));
        if (usuarioId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del usuario no es valido.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El usuario solicitado no existe.");
        }

        Usuario usuario = usuarioOpt.get();
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(usuario.getId()).append("\n")
            .append("Nombre: ").append(safe(usuario.getNombre())).append("\n")
            .append("Apellido: ").append(safe(usuario.getApellido())).append("\n")
            .append("Email: ").append(safe(usuario.getEmail())).append("\n")
            .append("Telefono: ").append(safe(usuario.getTelefono())).append("\n")
            .append("Rol: ").append(resolveRoleName(usuario));

        return HtmlBuilderUtil.buildPlainTemplate("Usuario", detail.toString());
    }

    public String insertarUsuario(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 5) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para registrar usuario."
            );
        }

        String email = params.get(4);
        if (email != null && email.contains(" ")) {
            throw new IllegalArgumentException("El email no debe contener espacios en blanco.");
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(params.get(0));
            usuario.setApellido(params.get(1));
            usuario.setContrasena(params.get(2));
            usuario.setTelefono(params.get(3));
            usuario.setEmail(params.get(4));
            if (params.size() >= 6) {
                usuario.setFoto(params.get(5));
            }

            String rolParam = params.size() >= 7 ? params.get(6) : null;
            Optional<Rol> rol = resolveRol(rolParam);
            if (rol.isEmpty()) {
                return HtmlBuilderUtil.buildErrorTemplate(
                    "Configuracion requerida",
                    "No existe el rol solicitado."
                );
            }
            usuario.setRol(rol.get());

            usuarioRepository.save(usuario);
            return HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Usuario registrado correctamente.");
        } catch (Exception ex) {
            logger.error("Failed to insert usuario", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar el usuario.");
        }
    }

    /**
     * Permite que cualquier persona (incluso no registrada) se registre como CLIENTE.
     * El email se toma automaticamente del remitente del correo.
     * Parametros: nombre, apellido, contrasena, telefono
     */
    public String registrarseComoCliente(List<String> params, String emailRemitente) {
        if (emailRemitente == null || emailRemitente.isBlank()) {
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo determinar tu email remitente.");
        }

        if (params == null || params.size() < 4) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Debe proporcionar: nombre, apellido, contrasena, telefono."
            );
        }

        // Verificar si ya existe un usuario con ese email
        Optional<Usuario> existente = usuarioRepository.findByEmailIgnoreCase(emailRemitente);
        if (existente.isPresent()) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Ya registrado",
                "Ya existe una cuenta registrada con el email " + emailRemitente + "."
            );
        }

        Optional<Rol> rolCliente = rolRepository.findByNombreIgnoreCase("CLIENTE");
        if (rolCliente.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Configuracion requerida",
                "El rol CLIENTE no esta configurado en el sistema."
            );
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(params.get(0));
            usuario.setApellido(params.get(1));
            usuario.setContrasena(params.get(2));
            usuario.setTelefono(params.get(3));
            usuario.setEmail(emailRemitente);
            usuario.setRol(rolCliente.get());

            usuarioRepository.save(usuario);

            StringBuilder detail = new StringBuilder();
            detail.append("Bienvenido, ").append(usuario.getNombre()).append(" ").append(usuario.getApellido()).append("!\n")
                .append("Tu cuenta fue creada correctamente como CLIENTE.\n")
                .append("Email registrado: ").append(emailRemitente).append("\n\n")
                .append("Ahora puedes usar comandos como INS_PEDIDO, LIS_PRODUCTO, PAGAR_PEDIDO, etc.");

            return HtmlBuilderUtil.buildSuccessTemplate("Registro completado", detail.toString());
        } catch (Exception ex) {
            logger.error("Failed to register as cliente: {}", emailRemitente, ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo completar el registro. Intenta nuevamente.");
        }
    }


    public String actualizarUsuario(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 6) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para actualizar usuario."
            );
        }

        Long usuarioId = parseLong(params.get(0));
        if (usuarioId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del usuario no es valido.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El usuario solicitado no existe.");
        }

        try {
            Usuario usuario = usuarioOpt.get();
            usuario.setNombre(params.get(1));
            usuario.setApellido(params.get(2));
            usuario.setContrasena(params.get(3));
            usuario.setTelefono(params.get(4));
            usuario.setEmail(params.get(5));
            if (params.size() >= 7) {
                usuario.setFoto(params.get(6));
            }

            if (params.size() >= 8) {
                String rolParam = params.get(7);
                Optional<Rol> rol = resolveRol(rolParam);
                if (rol.isEmpty()) {
                    return HtmlBuilderUtil.buildErrorTemplate(
                        "Configuracion requerida",
                        "No existe el rol solicitado."
                    );
                }
                usuario.setRol(rol.get());
            }

            usuarioRepository.save(usuario);
            return HtmlBuilderUtil.buildSuccessTemplate("Actualizacion completa", "Usuario actualizado correctamente.");
        } catch (Exception ex) {
            logger.error("Failed to update usuario", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo actualizar el usuario.");
        }
    }

    public String eliminarUsuario(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del usuario.");
        }

        Long usuarioId = parseLong(params.get(0));
        if (usuarioId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del usuario no es valido.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El usuario solicitado no existe.");
        }

        try {
            usuarioRepository.delete(usuarioOpt.get());
            return HtmlBuilderUtil.buildSuccessTemplate("Eliminacion completa", "Usuario eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Operacion no permitida",
                "No se puede eliminar el usuario porque esta relacionado con otros registros."
            );
        } catch (Exception ex) {
            logger.error("Failed to delete usuario", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo eliminar el usuario.");
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

    private Optional<Rol> resolveRol(String rolParam) {
        if (rolParam == null || rolParam.isBlank()) {
            return rolRepository.findByNombreIgnoreCase("CLIENTE");
        }

        return rolRepository.findByNombreIgnoreCase(rolParam);
    }

    private String resolveRoleName(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return "";
        }

        return usuario.getRol().getNombre();
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
