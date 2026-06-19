package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
