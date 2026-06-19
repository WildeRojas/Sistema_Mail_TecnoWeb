package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.Estado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByProveedorCorreoIgnoreCaseAndEstado(String correo, Estado estado);
}
