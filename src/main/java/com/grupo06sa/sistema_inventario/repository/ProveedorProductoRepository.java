package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorProductoRepository extends JpaRepository<ProveedorProducto, Long> {
    List<ProveedorProducto> findByProveedorId(Long proveedorId);

    Optional<ProveedorProducto> findByProveedorIdAndProductoId(Long proveedorId, Long productoId);

    List<ProveedorProducto> findByProductoId(Long productoId);

    Optional<ProveedorProducto> findByIdAndProveedorId(Long id, Long proveedorId);

    boolean existsByProveedorIdAndProductoId(Long proveedorId, Long productoId);
}
