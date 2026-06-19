package com.grupo06sa.sistema_inventario.security;

import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private final UsuarioRepository usuarioRepository;

    public SecurityService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario authenticateAndCheckRole(String email, String requiredRole) {
        if (email == null || email.isBlank()) {
            throw new UserNotFoundException("Email remitente vacío");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UserNotFoundException("Usuario no registrado"));

        if (requiredRole != null && !requiredRole.isBlank()) {
            String roleName = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
            if (roleName == null || !roleName.equalsIgnoreCase(requiredRole)) {
                throw new RoleAccessDeniedException("Rol requerido: " + requiredRole);
            }
        }

        return usuario;
    }
}
