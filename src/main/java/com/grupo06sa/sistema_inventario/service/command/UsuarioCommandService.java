package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Rol;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioCommandService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioCommandService(
        UsuarioRepository usuarioRepository,
        RolRepository rolRepository,
        ProveedorRepository proveedorRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.proveedorRepository = proveedorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Usuarios", "Sin usuarios registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildUsuariosTable(usuarios));
    }

    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El usuario solicitado no existe."));

        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(usuario.getId()).append("\n")
            .append("Nombre: ").append(safe(usuario.getNombre())).append("\n")
            .append("Apellido: ").append(safe(usuario.getApellido())).append("\n")
            .append("Email: ").append(safe(usuario.getEmail())).append("\n")
            .append("Telefono: ").append(safe(usuario.getTelefono())).append("\n")
            .append("Activo: ").append(usuario.isActivo() ? "SI" : "NO").append("\n")
            .append("Rol: ").append(resolveRoleName(usuario)).append("\n")
            .append("Proveedor: ").append(
                usuario.getProveedor() != null ? safe(usuario.getProveedor().getNombre()) : ""
            );

        return CommandResult.text(HtmlBuilderUtil.buildPlainTemplate("Usuario", detail.toString()));
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        String email = params.get(4);
        if (email != null && email.contains(" ")) {
            throw new IllegalArgumentException("El email no debe contener espacios en blanco.");
        }

        String rolParam = params.get(5);
        RolNombre rolNombre = parseRolNombre(rolParam);
        Long idProveedor = params.size() >= 7 ? parseLong(params.get(6)) : null;
        Proveedor proveedor = resolveProveedorObligatorio(rolNombre, idProveedor);
        Rol rol = rolRepository.findByNombreIgnoreCase(rolNombre.name())
            .orElseThrow(() -> new IllegalStateException("El rol " + rolNombre + " no está configurado."));

        Usuario usuario = new Usuario();
        usuario.setNombre(params.get(0));
        usuario.setApellido(params.get(1));
        usuario.setContrasena(passwordEncoder.encode(params.get(2)));
        usuario.setTelefono(params.get(3));
        usuario.setEmail(email);
        usuario.setActivo(true);
        usuario.setRol(rol);
        usuario.setProveedor(proveedor);

        usuarioRepository.save(usuario);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Usuario registrado correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El usuario solicitado no existe."));

        String rolParam = params.get(6);
        RolNombre rolNombre = parseRolNombre(rolParam);
        Long idProveedor = params.size() >= 8 ? parseLong(params.get(7)) : null;
        Proveedor proveedor = resolveProveedorObligatorio(rolNombre, idProveedor);
        Rol rol = rolRepository.findByNombreIgnoreCase(rolNombre.name())
            .orElseThrow(() -> new IllegalStateException("El rol " + rolNombre + " no está configurado."));

        usuario.setNombre(params.get(1));
        usuario.setApellido(params.get(2));
        usuario.setContrasena(passwordEncoder.encode(params.get(3)));
        usuario.setTelefono(params.get(4));
        usuario.setEmail(params.get(5));
        usuario.setRol(rol);
        usuario.setProveedor(proveedor);

        usuarioRepository.save(usuario);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Actualización completa", "Usuario actualizado correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El usuario solicitado no existe."));

        try {
            usuarioRepository.delete(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar el usuario porque está relacionado con otros registros.");
        }
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Eliminación completa", "Usuario eliminado correctamente."));
    }

    private RolNombre parseRolNombre(String rolParam) {
        if (rolParam == null || rolParam.isBlank()) {
            throw new IllegalArgumentException("Debe indicar un rol válido (PROPIETARIO, TRABAJADOR o PROVEEDOR).");
        }
        try {
            return RolNombre.valueOf(rolParam.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El rol debe ser PROPIETARIO, TRABAJADOR o PROVEEDOR.");
        }
    }

    private Proveedor resolveProveedorObligatorio(RolNombre rolNombre, Long idProveedor) {
        if (rolNombre != RolNombre.PROVEEDOR) {
            return null;
        }
        if (idProveedor == null) {
            throw new IllegalArgumentException("Debe indicar id_proveedor cuando el rol es PROVEEDOR.");
        }
        return proveedorRepository.findById(idProveedor)
            .orElseThrow(() -> new IllegalArgumentException("El proveedor indicado no existe."));
    }

    private String resolveRoleName(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return "";
        }
        return usuario.getRol().getNombre();
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
