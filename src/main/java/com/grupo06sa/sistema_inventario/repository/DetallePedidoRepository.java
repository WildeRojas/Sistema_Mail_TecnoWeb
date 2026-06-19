package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
	List<DetallePedido> findByPedidoId(Long pedidoId);
}
