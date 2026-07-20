package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoId(Long productoId);

    List<MovimientoInventario> findByCompraId(Long compraId);
}
