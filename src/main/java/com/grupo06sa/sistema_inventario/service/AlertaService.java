package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.entity.EstadoAlerta;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlertaStockRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaService {
    private final AlertaStockRepository alertaStockRepository;

    public AlertaService(AlertaStockRepository alertaStockRepository) {
        this.alertaStockRepository = alertaStockRepository;
    }

    public List<AlertaStock> listarPendientes() {
        return alertaStockRepository.findByEstado(EstadoAlerta.PENDIENTE);
    }

    @Transactional
    public AlertaStock atender(Long id, Usuario usuario) {
        AlertaStock alerta = alertaStockRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La alerta solicitada no existe."));

        if (alerta.getEstado() == EstadoAlerta.ATENDIDA) {
            throw new IllegalStateException("La alerta ya fue atendida.");
        }

        alerta.setEstado(EstadoAlerta.ATENDIDA);
        alerta.setAtendidaPor(usuario);
        alerta.setAtendidaAt(LocalDateTime.now());
        return alertaStockRepository.save(alerta);
    }
}
