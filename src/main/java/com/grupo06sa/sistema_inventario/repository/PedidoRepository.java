package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Estado;
import com.grupo06sa.sistema_inventario.entity.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
	List<Pedido> findByEstadoAndFechaBetween(Estado estado, LocalDateTime inicio, LocalDateTime fin);
}
