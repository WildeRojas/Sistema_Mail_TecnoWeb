package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}
