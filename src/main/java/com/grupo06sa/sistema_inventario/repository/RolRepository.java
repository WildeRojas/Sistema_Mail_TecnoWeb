package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {
	Optional<Rol> findByNombreIgnoreCase(String nombre);
}
