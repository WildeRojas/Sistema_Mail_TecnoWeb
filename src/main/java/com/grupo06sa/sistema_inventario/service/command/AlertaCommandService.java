package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.service.AlertaService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertaCommandService {
    private final AlertaService alertaService;

    public AlertaCommandService(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<AlertaStock> alertas = alertaService.listarPendientes();
        if (alertas.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Alertas de stock", "No hay alertas pendientes."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildAlertasTable(alertas));
    }

    public CommandResult atender(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        alertaService.atender(id, ctx.getUsuario());
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Alerta atendida", "La alerta se marcó como atendida."));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar un id numérico válido.");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El id proporcionado no es válido.");
        }
    }
}
