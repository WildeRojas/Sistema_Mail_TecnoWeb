package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.entity.EstadoAlerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {
    List<AlertaStock> findByEstado(EstadoAlerta estado);

    boolean existsByProductoIdAndEstado(Long productoId, EstadoAlerta estado);

    List<AlertaStock> findByProductoIdAndAlmacenIdAndEstado(Long productoId, Long almacenId, EstadoAlerta estado);

    List<AlertaStock> findByProductoIdAndEstado(Long productoId, EstadoAlerta estado);
}
