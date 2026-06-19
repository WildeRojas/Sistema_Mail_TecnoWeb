package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByCorreoIgnoreCase(String correo);
}
