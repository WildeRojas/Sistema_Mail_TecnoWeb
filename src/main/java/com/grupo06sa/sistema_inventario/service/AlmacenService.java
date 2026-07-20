package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlmacenService {
    private final AlmacenRepository almacenRepository;
    private final InventarioRepository inventarioRepository;

    public AlmacenService(AlmacenRepository almacenRepository, InventarioRepository inventarioRepository) {
        this.almacenRepository = almacenRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public List<Almacen> listar() {
        return almacenRepository.findAll();
    }

    public Almacen obtener(Long id) {
        return almacenRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El almacén solicitado no existe."));
    }

    public BigDecimal ocupacion(Long almacenId) {
        return inventarioRepository.findByAlmacenId(almacenId).stream()
            .map(inv -> inv.getCantidad() != null ? inv.getCantidad() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Almacen crear(String nombre, BigDecimal capacidad, String direccion, String coordenadasGps) {
        Almacen almacen = new Almacen();
        almacen.setNombre(nombre);
        almacen.setCapacidad(capacidad);
        almacen.setDireccion(direccion);
        almacen.setCoordenadasGps(coordenadasGps);
        return almacenRepository.save(almacen);
    }

    @Transactional
    public Almacen actualizar(Long id, String nombre, BigDecimal capacidad, String direccion, String coordenadasGps) {
        Almacen almacen = obtener(id);
        almacen.setNombre(nombre);
        almacen.setCapacidad(capacidad);
        almacen.setDireccion(direccion);
        almacen.setCoordenadasGps(coordenadasGps);
        return almacenRepository.save(almacen);
    }

    @Transactional
    public void eliminar(Long id) {
        Almacen almacen = obtener(id);
        if (ocupacion(id).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el almacén " + safe(almacen.getNombre()) + " porque tiene inventario existente."
            );
        }

        try {
            almacenRepository.delete(almacen);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                "No se puede eliminar el almacén porque está relacionado con otros registros."
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
