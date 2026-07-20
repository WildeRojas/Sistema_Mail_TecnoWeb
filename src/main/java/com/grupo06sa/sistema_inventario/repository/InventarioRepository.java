package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Inventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByProductoIdAndAlmacenId(Long productoId, Long almacenId);

    List<Inventario> findByProductoId(Long productoId);

    List<Inventario> findByAlmacenId(Long almacenId);
}
