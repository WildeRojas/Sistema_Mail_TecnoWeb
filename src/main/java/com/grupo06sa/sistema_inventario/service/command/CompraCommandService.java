package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.service.CompraService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CompraCommandService {
    private final CompraService compraService;

    public CompraCommandService(CompraService compraService) {
        this.compraService = compraService;
    }

    public String crearCompra(List<String> params, String emailRemitente) {
        return compraService.crearCompra(params, emailRemitente);
    }

    public String agregarDetalleCompra(List<String> params, String emailRemitente) {
        return compraService.agregarDetalleCompra(params, emailRemitente);
    }

    public String finalizarCompra(List<String> params, String emailRemitente) {
        return compraService.finalizarCompra(params, emailRemitente);
    }

    public String confirmarEntrega(List<String> params, String emailRemitente) {
        if (params == null || params.isEmpty()) {
            return com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "CONFIRMAR_ENTREGA requiere el id_compra."
            );
        }
        return compraService.confirmarEntrega(params, emailRemitente);
    }

    public String verMisOrdenes(String emailRemitente) {
        return compraService.verMisOrdenes(emailRemitente);
    }
}
