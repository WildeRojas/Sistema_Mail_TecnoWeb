package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
	List<DetalleCompra> findByCompraId(Long compraId);
}
