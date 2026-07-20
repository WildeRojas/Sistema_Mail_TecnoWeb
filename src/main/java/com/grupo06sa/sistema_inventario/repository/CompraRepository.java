package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByProveedorId(Long proveedorId);

    Optional<Compra> findByIdAndProveedorId(Long id, Long proveedorId);

    List<Compra> findByEstado(EstadoCompra estado);

    List<Compra> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Compra> findByProveedorIdAndFechaBetween(Long proveedorId, LocalDateTime inicio, LocalDateTime fin);
}
