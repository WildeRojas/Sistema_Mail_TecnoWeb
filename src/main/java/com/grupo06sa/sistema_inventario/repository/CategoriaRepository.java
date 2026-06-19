package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
