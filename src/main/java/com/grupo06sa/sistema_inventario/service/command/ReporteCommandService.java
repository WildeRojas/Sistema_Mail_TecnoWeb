package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.service.ReporteService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteCommandService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReporteService reporteService;
    private final SecurityService securityService;

    public ReporteCommandService(ReporteService reporteService, SecurityService securityService) {
        this.reporteService = reporteService;
        this.securityService = securityService;
    }

    public CommandResult reporteInventario(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return CommandResult.text(accessError);
        }

        if (params != null && !params.isEmpty() && !"*".equals(params.get(0))) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Use REP_INVENTARIO[\"*\"].")
            );
        }

        ReporteService.ReporteData data = reporteService.generarInventario();
        if (data.getPdf() == null || data.getCount() == 0) {
            return CommandResult.text(
                HtmlBuilderUtil.buildInfoTemplate("Sin datos", "No hay productos para reportar.")
            );
        }

        EmailAttachment attachment = new EmailAttachment(
            data.getFileName(),
            "application/pdf",
            data.getPdf()
        );
        String body = HtmlBuilderUtil.buildSuccessTemplate(
            "Reporte generado",
            "Se adjunta el reporte de inventario."
        );
        return CommandResult.withAttachment(body, attachment);
    }

    public CommandResult reporteVentas(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return CommandResult.text(accessError);
        }

        if (params == null || params.isEmpty()) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar YYYY-MM.")
            );
        }

        YearMonth month;
        try {
            month = YearMonth.parse(params.get(0), MONTH_FORMAT);
        } catch (DateTimeParseException ex) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Use YYYY-MM (ej: 2026-05).")
            );
        }

        ReporteService.ReporteData data = reporteService.generarVentas(month);
        if (data.getPdf() == null || data.getCount() == 0) {
            return CommandResult.text(
                HtmlBuilderUtil.buildInfoTemplate(
                    "Sin ventas",
                    "No hay pedidos PAGADOS para el mes solicitado."
                )
            );
        }

        EmailAttachment attachment = new EmailAttachment(
            data.getFileName(),
            "application/pdf",
            data.getPdf()
        );
        String body = HtmlBuilderUtil.buildSuccessTemplate(
            "Reporte generado",
            "Se adjunta el reporte de ventas del mes " + month + "."
        );
        return CommandResult.withAttachment(body, attachment);
    }

    private String validateAdminAccess(String emailRemitente) {
        try {
            securityService.authenticateAndCheckRole(emailRemitente, "ADMINISTRADOR");
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
        } catch (RoleAccessDeniedException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Privilegios insuficientes",
                "Solo un ADMINISTRADOR puede ejecutar este comando."
            );
        }
    }
}
