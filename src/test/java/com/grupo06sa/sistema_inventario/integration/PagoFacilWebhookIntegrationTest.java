package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoPago;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PagoFacilWebhookIntegrationTest extends IntegracionBaseTest {

    private static final String RESPUESTA_EXACTA =
        "{\"error\":0,\"status\":1,\"message\":\"Notificación recibida\",\"values\":true}";

    private Compra crearCompraConTotal(BigDecimal total) {
        Proveedor proveedor = crearProveedor("Proveedor Callback", emailUnico("callback.proveedor"));
        Categoria categoria = crearCategoria("Cat Callback");
        Producto producto = crearProducto("Producto Callback", new BigDecimal("10"), new BigDecimal("0"), categoria);
        Almacen almacen = crearAlmacen("Almacen Callback", new BigDecimal("1000"));
        crearOferta(proveedor, producto, new BigDecimal("10"));

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setAlmacen(almacen);
        compra.setEstado(EstadoCompra.EN_ESPERA);
        compra.setTotal(total);
        compra.setFecha(LocalDateTime.now());
        compra.setInventarioIngresado(false);
        return compraRepository.save(compra);
    }

    private Pago crearPagoPendiente(Compra compra, String referencia, String transactionId) {
        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setMetodoPago("qr_pagofacil");
        pago.setMonto(compra.getTotal());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setReferencia(referencia);
        pago.setTransactionId(transactionId);
        pago.setCreatedAt(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    @Test
    void callbackConEstado2YReferenciaValidaMarcaPagoYCompraPagados() throws Exception {
        Compra compra = crearCompraConTotal(new BigDecimal("500.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-ref1", "TX-ref1");

        RespuestaHttp respuesta = postJson(
            "/pagos/pagofacil/callback",
            "{\"Estado\":2,\"PedidoID\":\"" + pago.getReferencia() + "\"}"
        );

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoActualizado.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pagoActualizado.getFechaPago()).isNotNull();
        assertThat(pagoActualizado.getDatosRespuesta()).doesNotContain("tokenSecret").doesNotContain("password");

        Compra compraActualizada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraActualizada.getEstado()).isEqualTo(EstadoCompra.PAGADA);
    }

    @Test
    void callbackConEstadoDistintoDeDosNoTieneEfecto() throws Exception {
        Compra compra = crearCompraConTotal(new BigDecimal("200.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-ref2", "TX-ref2");

        RespuestaHttp respuesta = postJson(
            "/pagos/pagofacil/callback",
            "{\"Estado\":3,\"PedidoID\":\"" + pago.getReferencia() + "\"}"
        );

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);

        Pago pagoSinCambios = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoSinCambios.getEstado()).isEqualTo(EstadoPago.PENDIENTE);

        Compra compraSinCambios = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraSinCambios.getEstado()).isEqualTo(EstadoCompra.EN_ESPERA);
    }

    @Test
    void callbackConReferenciaInexistenteRespondeIgualSinEfecto() throws Exception {
        RespuestaHttp respuesta = postJson(
            "/pagos/pagofacil/callback",
            "{\"Estado\":2,\"PedidoID\":\"TJ999999-999999-no-existe\"}"
        );

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);
    }

    @Test
    void callbackSinCuerpoRespondeIgualSinFallar() throws Exception {
        RespuestaHttp respuesta = postJson("/pagos/pagofacil/callback", "{}");
        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);
    }

    @Test
    void callbackDuplicadoConElMismoPedidoIdNoVuelveAPagar() throws Exception {
        Compra compra = crearCompraConTotal(new BigDecimal("300.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-refdup", "TX-refdup");
        String payload = "{\"Estado\":2,\"PedidoID\":\"" + pago.getReferencia() + "\"}";

        RespuestaHttp primera = postJson("/pagos/pagofacil/callback", payload);
        assertThat(primera.status()).isEqualTo(200);
        Pago pagoTrasPrimera = pagoRepository.findById(pago.getId()).orElseThrow();
        LocalDateTime fechaPagoPrimera = pagoTrasPrimera.getFechaPago();
        assertThat(pagoTrasPrimera.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(fechaPagoPrimera).isNotNull();

        RespuestaHttp segunda = postJson("/pagos/pagofacil/callback", payload);
        assertThat(segunda.status()).isEqualTo(200);
        assertThat(segunda.body()).isEqualTo(RESPUESTA_EXACTA);

        Pago pagoTrasSegunda = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoTrasSegunda.getEstado()).isEqualTo(EstadoPago.PAGADO);

        assertThat(pagoTrasSegunda.getFechaPago()).isEqualTo(fechaPagoPrimera);

        long cantidadPagados = pagoRepository.findByCompraId(compra.getId()).stream()
            .filter(p -> p.getEstado() == EstadoPago.PAGADO)
            .count();
        assertThat(cantidadPagados).isEqualTo(1);
    }

    @Test
    void dosCallbacksConcurrentesParaElMismoPagoResultanEnUnaSolaTransicionAPagado() throws Exception {
        Compra compra = crearCompraConTotal(new BigDecimal("400.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-refconc", "TX-refconc");
        String payload = "{\"Estado\":2,\"PedidoID\":\"" + pago.getReferencia() + "\"}";

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(2);
        try {
            java.util.List<Future<RespuestaHttp>> futuros = IntStream.range(0, 2)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        return postJson("/pagos/pagofacil/callback", payload);
                    } finally {
                        salida.countDown();
                    }
                }))
                .toList();

            assertThat(salida.await(15, TimeUnit.SECONDS)).isTrue();
            for (Future<RespuestaHttp> futuro : futuros) {
                RespuestaHttp respuesta = futuro.get(5, TimeUnit.SECONDS);
                assertThat(respuesta.status()).isEqualTo(200);
                assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);
            }
        } finally {
            executor.shutdown();
        }

        Pago pagoFinal = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoFinal.getEstado()).isEqualTo(EstadoPago.PAGADO);

        long cantidadPagados = pagoRepository.findByCompraId(compra.getId()).stream()
            .filter(p -> p.getEstado() == EstadoPago.PAGADO)
            .count();
        assertThat(cantidadPagados).isEqualTo(1);

        Compra compraFinal = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(compraFinal.getEstado()).isEqualTo(EstadoCompra.PAGADA);
    }

    @Test
    void callbackSinPedidoIdUsaTransaccionComoFallback() throws Exception {
        Compra compra = crearCompraConTotal(new BigDecimal("250.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-reftx", "TX-solo-transaccion");

        RespuestaHttp respuesta = postJson(
            "/pagos/pagofacil/callback",
            "{\"Estado\":2,\"transaccion\":\"" + pago.getTransactionId() + "\"}"
        );

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(RESPUESTA_EXACTA);

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoActualizado.getEstado()).isEqualTo(EstadoPago.PAGADO);
    }

    @Test
    void verificarPagoConsultaWireMockYConfirmaPagoCuandoPaymentStatusEsDos() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);
        Compra compra = crearCompraConTotal(new BigDecimal("150.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-verif", "TX-verif");

        WIRE_MOCK.setScenarioState("pago", "PAGADO");

        CommandResult resultado = commandProcessor.process(
            "VERIFICAR_PAGO[\"" + compra.getId() + "\"]", emailPropietario
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains("PAGADO");

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoActualizado.getEstado()).isEqualTo(EstadoPago.PAGADO);
    }

    @Test
    void verificarPagoNoConfirmaCuandoPaymentStatusEsExpirado() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);
        Compra compra = crearCompraConTotal(new BigDecimal("150.00"));
        Pago pago = crearPagoPendiente(compra, "TJ" + compra.getId() + "-verifexp", "TX-verifexp");

        WIRE_MOCK.setScenarioState("pago", "Started");

        CommandResult resultado = commandProcessor.process(
            "VERIFICAR_PAGO[\"" + compra.getId() + "\"]", emailPropietario
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains("PENDIENTE");

        Pago pagoSinCambios = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoSinCambios.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
    }
}
