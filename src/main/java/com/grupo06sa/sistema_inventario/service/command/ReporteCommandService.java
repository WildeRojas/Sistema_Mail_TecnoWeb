package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.service.ReporteService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteCommandService {
    private final ReporteService reporteService;

    public ReporteCommandService(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    public CommandResult reporteInventario(ContextoAutenticado ctx, List<String> params) {
        ReporteService.ReporteData data = reporteService.generarInventario();
        if (data.getPdf() == null || data.getCount() == 0) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Sin datos", "No hay productos para reportar."));
        }

        EmailAttachment attachment = new EmailAttachment(data.getFileName(), "application/pdf", data.getPdf());
        String body = HtmlBuilderUtil.buildSuccessTemplate("Reporte generado", "Se adjunta el reporte de inventario.");
        return CommandResult.withAttachment(body, attachment);
    }

    public CommandResult reporteCompras(ContextoAutenticado ctx, List<String> params) {
        YearMonth mes = parseMes(params.get(0));
        Long proveedorFiltro = ctx.getRol() == RolNombre.PROVEEDOR ? ctx.getProveedorId() : null;

        ReporteService.ReporteData data = reporteService.generarCompras(mes, proveedorFiltro);
        if (data.getPdf() == null || data.getCount() == 0) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Sin datos", "No hay compras registradas para " + mes + "."));
        }

        EmailAttachment attachment = new EmailAttachment(data.getFileName(), "application/pdf", data.getPdf());
        String body = HtmlBuilderUtil.buildSuccessTemplate("Reporte generado", "Se adjunta el reporte de compras de " + mes + ".");
        return CommandResult.withAttachment(body, attachment);
    }

    private YearMonth parseMes(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el mes en formato YYYY-MM.");
        }
        try {
            return YearMonth.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El mes debe tener el formato YYYY-MM.");
        }
    }
}
