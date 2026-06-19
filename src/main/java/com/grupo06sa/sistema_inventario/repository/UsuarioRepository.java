package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByEmailIgnoreCase(String email);
	List<Usuario> findByRolNombreIgnoreCase(String nombre);
}
