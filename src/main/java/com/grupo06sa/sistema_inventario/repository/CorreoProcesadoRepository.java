package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.CorreoProcesado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreoProcesadoRepository extends JpaRepository<CorreoProcesado, Long> {
    boolean existsByMessageId(String messageId);

    Optional<CorreoProcesado> findByMessageId(String messageId);
}
