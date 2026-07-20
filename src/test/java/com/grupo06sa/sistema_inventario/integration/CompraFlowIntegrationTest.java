package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoPago;
import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompraFlowIntegrationTest extends IntegracionBaseTest {

    @Test
    void flujoCompletoDeSolicitudHastaPagoConfirmado() throws Exception {
        String emailPropietario = emailUnico("propietario");
        Usuario propietario = crearPropietario(emailPropietario);

        String correoProveedor = emailUnico("proveedor.correo");
        Proveedor proveedor = crearProveedor("Proveedor Flujo Completo", correoProveedor);
        String emailUsuarioProveedor = emailUnico("proveedor.usuario");
        crearUsuarioProveedor(emailUsuarioProveedor, proveedor);

        Producto producto = crearProducto("Producto Flujo", new BigDecimal("100.00"), new BigDecimal("5"), crearCategoria("Cat Flujo"));
        Almacen almacenDestino = crearAlmacen("Almacen Flujo", new BigDecimal("1000"));
        ProveedorProducto oferta = crearOferta(proveedor, producto, new BigDecimal("120.00"));

        CommandResult rSolicitud = commandProcessor.process(
            "INS_SOLICITUD[\"" + proveedor.getId() + "\",\"" + producto.getId() + "\",\"10\",\"" + almacenDestino.getId() + "\"]",
            emailPropietario
        );
        assertThat(rSolicitud.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        List<SolicitudCompra> solicitudes = solicitudRepository.findByProveedorId(proveedor.getId());
        assertThat(solicitudes).hasSize(1);
        SolicitudCompra solicitud = solicitudes.get(0);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        boolean llegoNotificacion = GREEN_MAIL.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1);
        assertThat(llegoNotificacion).isTrue();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(correoProveedor)).hasSize(1);
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(correoProveedor)[0].getSubject())
            .contains("Nueva solicitud de compra");
        GREEN_MAIL.purgeEmailFromAllMailboxes();

        CommandResult rAtender = commandProcessor.process(
            "ATENDER_SOLICITUD[\"" + solicitud.getId() + "\",\"115.00\"]",
            emailUsuarioProveedor
        );
        assertThat(rAtender.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        SolicitudCompra solicitudAtendida = solicitudRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(solicitudAtendida.getEstado()).isEqualTo(EstadoSolicitud.ATENDIDA);
        assertThat(solicitudAtendida.getCostoOfrecido()).isEqualByComparingTo("115.00");

        CommandResult rCompra = commandProcessor.process(
            "INS_COMPRA[\"" + proveedor.getId() + "\",\"" + almacenDestino.getId() + "\"]",
            emailPropietario
        );
        assertThat(rCompra.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        List<Compra> comprasProveedor = compraRepository.findByProveedorId(proveedor.getId());
        assertThat(comprasProveedor).hasSize(1);
        Compra compra = comprasProveedor.get(0);
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(compra.getSolicitud()).isNotNull();
        assertThat(compra.getSolicitud().getId()).isEqualTo(solicitud.getId());

        SolicitudCompra solicitudConvertida = solicitudRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(solicitudConvertida.getEstado()).isEqualTo(EstadoSolicitud.CONVERTIDA);

        CommandResult rDetalle = commandProcessor.process(
            "INS_DETALLE_COMPRA[\"" + compra.getId() + "\",\"" + oferta.getId() + "\",\"10\"]",
            emailPropietario
        );
        assertThat(rDetalle.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compra.getId());
        assertThat(detalles).hasSize(1);
        DetalleCompra detalle = detalles.get(0);
        assertThat(detalle.getCostoUnitario()).isEqualByComparingTo("120.00");
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("1200.00");

        CommandResult rActualizaOferta = commandProcessor.process(
            "UPD_OFERTA[\"" + oferta.getId() + "\",\"999.00\"]",
            emailUsuarioProveedor
        );
        assertThat(rActualizaOferta.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        DetalleCompra detalleTrasCambioDeOferta = detalleCompraRepository.findById(detalle.getId()).orElseThrow();
        assertThat(detalleTrasCambioDeOferta.getCostoUnitario()).isEqualByComparingTo("120.00");

        CommandResult rFinalizar = commandProcessor.process(
            "FINALIZAR_COMPRA[\"" + compra.getId() + "\"]",
            emailPropietario
        );
        assertThat(rFinalizar.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        Compra compraFinalizada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraFinalizada.getEstado()).isEqualTo(EstadoCompra.EN_ESPERA);
        assertThat(compraFinalizada.getTotal()).isEqualByComparingTo("1200.00");

        boolean llegoOrden = GREEN_MAIL.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1);
        assertThat(llegoOrden).isTrue();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(correoProveedor)[0].getSubject())
            .contains("Nueva orden de compra");
        GREEN_MAIL.purgeEmailFromAllMailboxes();

        BigDecimal stockAntes = stockDe(producto, almacenDestino);
        CommandResult rRecibir = commandProcessor.process(
            "RECIBIR_COMPRA[\"" + compra.getId() + "\"]",
            emailPropietario
        );
        assertThat(rRecibir.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        Compra compraRecibida = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraRecibida.getEstado()).isEqualTo(EstadoCompra.RECIBIDA);
        assertThat(compraRecibida.isInventarioIngresado()).isTrue();

        BigDecimal stockDespues = stockDe(producto, almacenDestino);
        assertThat(stockDespues).isEqualByComparingTo(stockAntes.add(new BigDecimal("10")));

        long movimientosIngresoAntes = movimientoRepository.findByCompraId(compra.getId()).stream()
            .filter(m -> esTipo(m.getTipoOperacion(), "INGRESO"))
            .count();
        assertThat(movimientosIngresoAntes).isEqualTo(1);

        GREEN_MAIL.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1);
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(propietario.getEmail())).hasSizeGreaterThanOrEqualTo(1);
        GREEN_MAIL.purgeEmailFromAllMailboxes();

        CommandResult rRecibirDeNuevo = commandProcessor.process(
            "RECIBIR_COMPRA[\"" + compra.getId() + "\"]",
            emailPropietario
        );
        assertThat(rRecibirDeNuevo.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        BigDecimal stockTrasSegundaRecepcion = stockDe(producto, almacenDestino);
        assertThat(stockTrasSegundaRecepcion).isEqualByComparingTo(stockDespues);

        long movimientosIngresoDespues = movimientoRepository.findByCompraId(compra.getId()).stream()
            .filter(m -> esTipo(m.getTipoOperacion(), "INGRESO"))
            .count();
        assertThat(movimientosIngresoDespues).isEqualTo(1);

        CommandResult rPagar = commandProcessor.process(
            "PAGAR_COMPRA[\"" + compra.getId() + "\"]",
            emailPropietario
        );
        assertThat(rPagar.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(rPagar.getAttachment()).isNotNull();
        assertThat(rPagar.getAttachment().getFileName()).isEqualTo("qr.png");

        List<Pago> pagosDeLaCompra = pagoRepository.findByCompraId(compra.getId());
        assertThat(pagosDeLaCompra).hasSize(1);
        Pago pago = pagosDeLaCompra.get(0);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getReferencia()).isEqualTo("TJ" + compra.getId() + "-" + pago.getId());
        assertThat(pago.getTransactionId()).isNotBlank();
        assertThat(pago.getQrImagen()).isNotBlank();

        String payload = "{\"Estado\":2,\"PedidoID\":\"" + pago.getReferencia() + "\"}";
        RespuestaHttp respuesta = postJson("/pagos/pagofacil/callback", payload);
        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(
            "{\"error\":0,\"status\":1,\"message\":\"Notificación recibida\",\"values\":true}"
        );

        Pago pagoConfirmado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoConfirmado.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pagoConfirmado.getFechaPago()).isNotNull();

        Compra compraPagada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraPagada.getEstado()).isEqualTo(EstadoCompra.PAGADA);
    }

    private BigDecimal stockDe(Producto producto, Almacen almacen) {
        return inventarioRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
            .map(Inventario::getCantidad)
            .orElse(BigDecimal.ZERO);
    }

    private boolean esTipo(TipoOperacion tipoOperacion, String nombre) {
        return tipoOperacion != null && nombre.equalsIgnoreCase(tipoOperacion.getNombre());
    }
}
