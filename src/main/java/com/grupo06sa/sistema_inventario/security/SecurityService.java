package com.grupo06sa.sistema_inventario.security;

import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private static final String MENSAJE_NO_REGISTRADO =
        "El remitente no está registrado o su cuenta no está activa.";

    private final UsuarioRepository usuarioRepository;

    public SecurityService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public ContextoAutenticado autenticar(String email) {
        if (email == null || email.isBlank()) {
            throw new UserNotFoundException(MENSAJE_NO_REGISTRADO);
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new UserNotFoundException(MENSAJE_NO_REGISTRADO));

        if (!usuario.isActivo()) {
            throw new UserNotFoundException(MENSAJE_NO_REGISTRADO);
        }

        RolNombre rol = resolveRol(usuario);
        Long proveedorId = usuario.getProveedor() != null ? usuario.getProveedor().getId() : null;
        return new ContextoAutenticado(usuario, rol, proveedorId);
    }

    public void assertOwnership(Long proveedorIdEntidad, ContextoAutenticado ctx) {
        if (ctx == null || ctx.getRol() != RolNombre.PROVEEDOR) {
            return;
        }

        if (proveedorIdEntidad == null
            || ctx.getProveedorId() == null
            || !proveedorIdEntidad.equals(ctx.getProveedorId())) {
            throw new RoleAccessDeniedException("No tiene acceso a este recurso.");
        }
    }

    private RolNombre resolveRol(Usuario usuario) {
        String nombre = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        if (nombre == null || nombre.isBlank()) {
            throw new UserNotFoundException(MENSAJE_NO_REGISTRADO);
        }

        try {
            return RolNombre.valueOf(nombre.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UserNotFoundException(MENSAJE_NO_REGISTRADO);
        }
    }
}
