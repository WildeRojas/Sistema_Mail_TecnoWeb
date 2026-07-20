package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.Pago;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByReferencia(String referencia);

    Optional<Pago> findByTransactionId(String transactionId);

    List<Pago> findByCompraId(Long compraId);

    List<Pago> findByCompraProveedorId(Long proveedorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.referencia = :referencia")
    Optional<Pago> findWithLockByReferencia(@Param("referencia") String referencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.transactionId = :transactionId")
    Optional<Pago> findWithLockByTransactionId(@Param("transactionId") String transactionId);
}
