package com.grupo06sa.sistema_inventario.security;

public class RoleAccessDeniedException extends RuntimeException {
    public RoleAccessDeniedException(String message) {
        super(message);
    }
}
