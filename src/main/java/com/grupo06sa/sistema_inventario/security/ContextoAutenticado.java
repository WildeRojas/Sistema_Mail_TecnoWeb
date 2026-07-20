package com.grupo06sa.sistema_inventario.security;

import com.grupo06sa.sistema_inventario.entity.Usuario;

public class ContextoAutenticado {
    private final Usuario usuario;
    private final RolNombre rol;
    private final Long proveedorId;

    public ContextoAutenticado(Usuario usuario, RolNombre rol, Long proveedorId) {
        this.usuario = usuario;
        this.rol = rol;
        this.proveedorId = proveedorId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public RolNombre getRol() {
        return rol;
    }

    public Long getProveedorId() {
        return proveedorId;
    }
}
