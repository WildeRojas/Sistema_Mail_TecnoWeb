package com.grupo06sa.sistema_inventario.repository;

import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {
    List<SolicitudCompra> findByProveedorId(Long proveedorId);

    List<SolicitudCompra> findByProveedorIdAndEstado(Long proveedorId, EstadoSolicitud estado);

    List<SolicitudCompra> findByEstado(EstadoSolicitud estado);

    Optional<SolicitudCompra> findByIdAndProveedorId(Long id, Long proveedorId);
}
