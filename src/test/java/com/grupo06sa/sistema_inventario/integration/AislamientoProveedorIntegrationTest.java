package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoPago;
import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AislamientoProveedorIntegrationTest extends IntegracionBaseTest {

    private String emailPropietario;
    private String emailTrabajador;

    private Proveedor proveedorA;
    private String emailUsuarioA;
    private ProveedorProducto ofertaA;
    private SolicitudCompra solicitudA;
    private Compra compraA;
    private Pago pagoA;

    private Proveedor proveedorB;
    private String emailUsuarioB;
    private ProveedorProducto ofertaB;
    private SolicitudCompra solicitudB;
    private Compra compraB;
    private Pago pagoB;

    private Producto producto;
    private Almacen almacen;

    @BeforeEach
    void sembrarDosProveedoresConDatosPropios() {
        emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);
        emailTrabajador = emailUnico("trabajador");
        crearTrabajador(emailTrabajador);

        Categoria categoria = crearCategoria("Cat Aislamiento");
        producto = crearProducto("Producto Aislamiento", new BigDecimal("60"), new BigDecimal("0"), categoria);
        almacen = crearAlmacen("Almacen Aislamiento", new BigDecimal("1000"));

        proveedorA = crearProveedor("Proveedor A", emailUnico("provA.correo"));
        emailUsuarioA = emailUnico("provA.usuario");
        crearUsuarioProveedor(emailUsuarioA, proveedorA);
        ofertaA = crearOferta(proveedorA, producto, new BigDecimal("70.00"));
        solicitudA = crearSolicitud(proveedorA);
        compraA = crearCompraConDetalle(proveedorA, ofertaA);
        pagoA = crearPago(compraA);

        proveedorB = crearProveedor("Proveedor B", emailUnico("provB.correo"));
        emailUsuarioB = emailUnico("provB.usuario");
        crearUsuarioProveedor(emailUsuarioB, proveedorB);
        ofertaB = crearOferta(proveedorB, producto, new BigDecimal("75.00"));
        solicitudB = crearSolicitud(proveedorB);
        compraB = crearCompraConDetalle(proveedorB, ofertaB);
        pagoB = crearPago(compraB);
    }

    private SolicitudCompra crearSolicitud(Proveedor proveedor) {
        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setProveedor(proveedor);
        solicitud.setProducto(producto);
        solicitud.setAlmacen(almacen);
        solicitud.setCantidad(new BigDecimal("5"));
        solicitud.setCostoEstimado(new BigDecimal("300.00"));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setCreatedAt(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    private Compra crearCompraConDetalle(Proveedor proveedor, ProveedorProducto oferta) {
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setAlmacen(almacen);
        compra.setEstado(EstadoCompra.EN_ESPERA);
        compra.setTotal(new BigDecimal("350.00"));
        compra.setFecha(LocalDateTime.now());
        compra.setInventarioIngresado(false);
        return compraRepository.save(compra);
    }

    private Pago crearPago(Compra compra) {
        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setMetodoPago("qr_pagofacil");
        pago.setMonto(compra.getTotal());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setCreatedAt(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);
        guardado.setReferencia("TJ" + compra.getId() + "-" + guardado.getId());
        return pagoRepository.save(guardado);
    }

    @Test
    void proveedorSoloVeSusPropiasOfertas() {
        CommandResult resultado = commandProcessor.process("LIS_OFERTA", emailUsuarioA);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains(String.valueOf(ofertaA.getId()));

        assertThat(ofertaRepository.findByProveedorId(proveedorA.getId())).extracting("id").containsExactly(ofertaA.getId());
    }

    @Test
    void proveedorSoloVeSusPropiasSolicitudes() {
        CommandResult resultado = commandProcessor.process("LIS_SOLICITUD", emailUsuarioA);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains(String.valueOf(solicitudA.getId()));
        assertThat(resultado.getBody()).doesNotContain("\">" + solicitudB.getId() + "</td");
        assertThat(solicitudRepository.findByProveedorId(proveedorA.getId()))
            .extracting("id").containsExactly(solicitudA.getId());
    }

    @Test
    void proveedorSoloVeSusPropiasCompras() {
        CommandResult resultado = commandProcessor.process("LIS_COMPRA", emailUsuarioA);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(compraRepository.findByProveedorId(proveedorA.getId()))
            .extracting("id").containsExactly(compraA.getId());
        assertThat(compraRepository.findByProveedorId(proveedorB.getId()))
            .extracting("id").containsExactly(compraB.getId());
    }

    @Test
    void proveedorSoloVeSusPropiosPagos() {
        CommandResult resultado = commandProcessor.process("LIS_PAGO", emailUsuarioA);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(pagoRepository.findByCompraProveedorId(proveedorA.getId()))
            .extracting("id").containsExactly(pagoA.getId());
    }

    @Test
    void proveedorNoPuedeActualizarOfertaDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "UPD_OFERTA[\"" + ofertaB.getId() + "\",\"999.00\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        ProveedorProducto ofertaBSinCambios = ofertaRepository.findById(ofertaB.getId()).orElseThrow();
        assertThat(ofertaBSinCambios.getCostoUnitarioActual()).isEqualByComparingTo("75.00");
    }

    @Test
    void proveedorNoPuedeEliminarOfertaDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "DEL_OFERTA[\"" + ofertaB.getId() + "\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        assertThat(ofertaRepository.findById(ofertaB.getId())).isPresent();
    }

    @Test
    void proveedorNoPuedeAtenderSolicitudDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "ATENDER_SOLICITUD[\"" + solicitudB.getId() + "\",\"50.00\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        SolicitudCompra sinCambios = solicitudRepository.findById(solicitudB.getId()).orElseThrow();
        assertThat(sinCambios.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void proveedorNoPuedeRechazarSolicitudDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "RECHAZAR_SOLICITUD[\"" + solicitudB.getId() + "\",\"motivo\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
    }

    @Test
    void proveedorNoPuedeVerCompraDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "GET_COMPRA[\"" + compraB.getId() + "\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
    }

    @Test
    void proveedorNoPuedeVerificarPagoDeOtroProveedor() {
        CommandResult resultado = commandProcessor.process(
            "VERIFICAR_PAGO[\"" + compraB.getId() + "\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
    }

    @Test
    void proveedorNoPuedeVerProductoQueNoOfrece() {
        Categoria otraCategoria = crearCategoria("Cat Exclusiva");
        Producto productoExclusivoDeB = crearProducto("Producto Exclusivo B", new BigDecimal("40"), new BigDecimal("0"), otraCategoria);
        crearOferta(proveedorB, productoExclusivoDeB, new BigDecimal("45.00"));

        CommandResult resultado = commandProcessor.process(
            "GET_PRODUCTO[\"" + productoExclusivoDeB.getId() + "\"]", emailUsuarioA
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
    }

    @Test
    void propietarioPuedeCompararOfertasDeAmbosProveedores() {
        CommandResult resultado = commandProcessor.process(
            "COMPARAR_OFERTAS[\"" + producto.getId() + "\"]", emailPropietario
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains("Proveedor A").contains("Proveedor B");
    }

    @Test
    void propietarioPuedeVerComprasDeAmbosProveedores() {
        CommandResult resultadoA = commandProcessor.process("GET_COMPRA[\"" + compraA.getId() + "\"]", emailPropietario);
        CommandResult resultadoB = commandProcessor.process("GET_COMPRA[\"" + compraB.getId() + "\"]", emailPropietario);
        assertThat(resultadoA.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultadoB.getTipo()).isEqualTo(CommandResult.Tipo.OK);
    }

    @Test
    void propietarioVeTodasLasCompresEnElListado() {
        CommandResult resultado = commandProcessor.process("LIS_COMPRA", emailPropietario);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(compraRepository.findAll()).extracting("id")
            .contains(compraA.getId(), compraB.getId());
    }

    @Test
    void trabajadorNoPuedePagarUnaCompra() {
        CommandResult resultado = commandProcessor.process(
            "PAGAR_COMPRA[\"" + compraA.getId() + "\"]", emailTrabajador
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
    }

    @Test
    void remitenteNoRegistradoRecibeNoRegistrado() {
        CommandResult resultado = commandProcessor.process("LIS_OFERTA", "nadie@test.local");
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.NO_REGISTRADO);
    }

    @Test
    void usuarioInactivoSeTrataComoNoAutorizado() {
        String emailInactivo = emailUnico("inactivo");
        Usuario inactivo = crearUsuario("Inactivo", emailInactivo, com.grupo06sa.sistema_inventario.security.RolNombre.PROVEEDOR, proveedorA, false);
        assertThat(inactivo.isActivo()).isFalse();

        CommandResult resultado = commandProcessor.process("LIS_OFERTA", emailInactivo);
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.NO_REGISTRADO);
    }
}
