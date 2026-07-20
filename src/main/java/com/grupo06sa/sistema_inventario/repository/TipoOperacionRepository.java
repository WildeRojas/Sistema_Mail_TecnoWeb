package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoOperacionRepository extends JpaRepository<TipoOperacion, Long> {
    Optional<TipoOperacion> findByNombreIgnoreCase(String nombre);
}
