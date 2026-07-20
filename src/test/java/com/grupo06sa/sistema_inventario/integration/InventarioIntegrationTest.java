package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.EstadoAlerta;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InventarioIntegrationTest extends IntegracionBaseTest {

    @Test
    void movimientoDeSalidaConStockInsuficienteEsRechazadoYNoDejaStockNegativo() {
        String email = emailUnico("propietario");
        crearPropietario(email);
        Producto producto = crearProducto("Producto Salida", new BigDecimal("50"), new BigDecimal("0"), crearCategoria("Cat Salida"));
        Almacen almacen = crearAlmacen("Almacen Salida", new BigDecimal("100"));
        crearInventario(producto, almacen, new BigDecimal("5"));

        CommandResult resultado = commandProcessor.process(
            "INS_MOVIMIENTO[\"SALIDA\",\"" + producto.getId() + "\",\"" + almacen.getId() + "\",\"10\"]",
            email
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("Stock insuficiente");

        BigDecimal stockFinal = inventarioRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
            .orElseThrow().getCantidad();
        assertThat(stockFinal).isEqualByComparingTo("5");
        assertThat(stockFinal).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void movimientoDeSalidaConStockSuficienteDescuentaCorrectamente() {
        String email = emailUnico("propietario");
        crearPropietario(email);
        Producto producto = crearProducto("Producto Salida OK", new BigDecimal("50"), new BigDecimal("0"), crearCategoria("Cat Salida OK"));
        Almacen almacen = crearAlmacen("Almacen Salida OK", new BigDecimal("100"));
        crearInventario(producto, almacen, new BigDecimal("20"));

        CommandResult resultado = commandProcessor.process(
            "INS_MOVIMIENTO[\"SALIDA\",\"" + producto.getId() + "\",\"" + almacen.getId() + "\",\"7\"]",
            email
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        BigDecimal stockFinal = inventarioRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
            .orElseThrow().getCantidad();
        assertThat(stockFinal).isEqualByComparingTo("13");
    }

    @Test
    void trasladoStockMueveExistenciasRealesEntreAlmacenes() {
        String email = emailUnico("propietario");
        crearPropietario(email);
        Producto producto = crearProducto("Producto Traslado", new BigDecimal("30"), new BigDecimal("0"), crearCategoria("Cat Traslado"));
        Almacen origen = crearAlmacen("Almacen Origen", new BigDecimal("1000"));
        Almacen destino = crearAlmacen("Almacen Destino", new BigDecimal("1000"));
        crearInventario(producto, origen, new BigDecimal("40"));

        CommandResult resultado = commandProcessor.process(
            "TRASLADO_STOCK[\"" + producto.getId() + "\",\"" + origen.getId() + "\",\"" + destino.getId() + "\",\"15\"]",
            email
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(stockDe(producto, origen)).isEqualByComparingTo("25");
        assertThat(stockDe(producto, destino)).isEqualByComparingTo("15");
    }

    @Test
    void trasladoStockRechazaMismoAlmacenDeOrigenYDestino() {
        String email = emailUnico("propietario");
        crearPropietario(email);
        Producto producto = crearProducto("Producto Traslado Mismo", new BigDecimal("30"), new BigDecimal("0"), crearCategoria("Cat Traslado Mismo"));
        Almacen almacen = crearAlmacen("Almacen Unico", new BigDecimal("1000"));
        crearInventario(producto, almacen, new BigDecimal("40"));

        CommandResult resultado = commandProcessor.process(
            "TRASLADO_STOCK[\"" + producto.getId() + "\",\"" + almacen.getId() + "\",\"" + almacen.getId() + "\",\"5\"]",
            email
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("diferentes");
    }

    @Test
    void trasladoStockRechazaSiExcedeCapacidadDelAlmacenDestino() {
        String email = emailUnico("propietario");
        crearPropietario(email);
        Producto producto = crearProducto("Producto Traslado Cap", new BigDecimal("30"), new BigDecimal("0"), crearCategoria("Cat Traslado Cap"));
        Almacen origen = crearAlmacen("Almacen Origen Cap", new BigDecimal("1000"));
        Almacen destinoPequeno = crearAlmacen("Almacen Destino Pequeno", new BigDecimal("10"));
        crearInventario(producto, origen, new BigDecimal("50"));
        crearInventario(producto, destinoPequeno, new BigDecimal("8"));

        CommandResult resultado = commandProcessor.process(
            "TRASLADO_STOCK[\"" + producto.getId() + "\",\"" + origen.getId() + "\",\"" + destinoPequeno.getId() + "\",\"5\"]",
            email
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("capacidad");
        assertThat(stockDe(producto, destinoPequeno)).isEqualByComparingTo("8");
    }

    @Test
    void bajarStockPorDebajoDelMinimoGeneraUnaSolaAlertaPendienteSinDuplicar() {
        String email = emailUnico("propietario");
        Usuario propietario = crearPropietario(email);
        Producto producto = crearProducto("Producto Alerta", new BigDecimal("30"), new BigDecimal("10"), crearCategoria("Cat Alerta"));
        Almacen almacen = crearAlmacen("Almacen Alerta", new BigDecimal("1000"));
        crearInventario(producto, almacen, new BigDecimal("15"));

        CommandResult salida1 = commandProcessor.process(
            "INS_MOVIMIENTO[\"SALIDA\",\"" + producto.getId() + "\",\"" + almacen.getId() + "\",\"8\"]",
            email
        );
        assertThat(salida1.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        java.util.List<AlertaStock> alertasTrasPrimeraSalida =
            alertaStockRepository.findByProductoIdAndEstado(producto.getId(), EstadoAlerta.PENDIENTE);
        assertThat(alertasTrasPrimeraSalida).hasSize(1);

        CommandResult salida2 = commandProcessor.process(
            "INS_MOVIMIENTO[\"SALIDA\",\"" + producto.getId() + "\",\"" + almacen.getId() + "\",\"2\"]",
            email
        );
        assertThat(salida2.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        java.util.List<AlertaStock> alertasTrasSegundaSalida =
            alertaStockRepository.findByProductoIdAndEstado(producto.getId(), EstadoAlerta.PENDIENTE);
        assertThat(alertasTrasSegundaSalida).hasSize(1);
        Long idAlerta = alertasTrasSegundaSalida.get(0).getId();

        CommandResult listado = commandProcessor.process("LIS_ALERTA", email);
        assertThat(listado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(listado.getBody()).contains("Producto Alerta");

        CommandResult atender = commandProcessor.process("ATENDER_ALERTA[\"" + idAlerta + "\"]", email);
        assertThat(atender.getTipo()).isEqualTo(CommandResult.Tipo.OK);

        AlertaStock alertaAtendida = alertaStockRepository.findById(idAlerta).orElseThrow();
        assertThat(alertaAtendida.getEstado()).isEqualTo(EstadoAlerta.ATENDIDA);
        assertThat(alertaAtendida.getAtendidaPor()).isNotNull();
        assertThat(alertaAtendida.getAtendidaPor().getId()).isEqualTo(propietario.getId());
    }

    private BigDecimal stockDe(Producto producto, Almacen almacen) {
        return inventarioRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
            .map(Inventario::getCantidad)
            .orElse(BigDecimal.ZERO);
    }
}
