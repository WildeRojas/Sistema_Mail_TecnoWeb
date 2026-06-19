package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.service.PedidoService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PedidosCommandService {
    private final PedidoService pedidoService;

    public PedidosCommandService(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    public String crearPedido(List<String> params, String emailRemitente) {
        return pedidoService.crearPedido(params, emailRemitente);
    }

    public String agregarDetalle(List<String> params, String emailRemitente) {
        return pedidoService.agregarDetalle(params, emailRemitente);
    }

    public CommandResult pagarPedido(List<String> params, String emailRemitente) {
        return pedidoService.pagarPedido(params, emailRemitente);
    }

    public String comprobarPago(List<String> params, String emailRemitente) {
        if (params == null || params.size() < 3) {
            return com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "COMPROBAR_PAGO requiere exactamente 3 parametros: id_pedido, numero_cuenta_destinatario, monto_cancelado."
            );
        }
        return pedidoService.comprobarPago(params, emailRemitente);
    }
}
