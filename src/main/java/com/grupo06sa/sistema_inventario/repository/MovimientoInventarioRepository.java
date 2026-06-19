package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
}
