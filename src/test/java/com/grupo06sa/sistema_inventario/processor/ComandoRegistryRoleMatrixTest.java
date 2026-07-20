package com.grupo06sa.sistema_inventario.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.grupo06sa.sistema_inventario.processor.ComandoRegistry.ComandoDef;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.service.command.AlertaCommandService;
import com.grupo06sa.sistema_inventario.service.command.AlmacenCommandService;
import com.grupo06sa.sistema_inventario.service.command.CategoriaCommandService;
import com.grupo06sa.sistema_inventario.service.command.CompraCommandService;
import com.grupo06sa.sistema_inventario.service.command.InventarioCommandService;
import com.grupo06sa.sistema_inventario.service.command.OfertaCommandService;
import com.grupo06sa.sistema_inventario.service.command.PagoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProductoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProveedorCommandService;
import com.grupo06sa.sistema_inventario.service.command.ReporteCommandService;
import com.grupo06sa.sistema_inventario.service.command.SolicitudCommandService;
import com.grupo06sa.sistema_inventario.service.command.UsuarioCommandService;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ComandoRegistryRoleMatrixTest {

    private static final Set<RolNombre> P = EnumSet.of(RolNombre.PROPIETARIO);
    private static final Set<RolNombre> PT = EnumSet.of(RolNombre.PROPIETARIO, RolNombre.TRABAJADOR);
    private static final Set<RolNombre> PTV = EnumSet.allOf(RolNombre.class);
    private static final Set<RolNombre> V = EnumSet.of(RolNombre.PROVEEDOR);
    private static final Set<RolNombre> PV = EnumSet.of(RolNombre.PROPIETARIO, RolNombre.PROVEEDOR);

    private ComandoRegistry registry;

    @BeforeEach
    void construirRegistro() {
        registry = new ComandoRegistry(
            mock(UsuarioCommandService.class),
            mock(ProveedorCommandService.class),
            mock(CategoriaCommandService.class),
            mock(ProductoCommandService.class),
            mock(AlmacenCommandService.class),
            mock(InventarioCommandService.class),
            mock(AlertaCommandService.class),
            mock(OfertaCommandService.class),
            mock(SolicitudCommandService.class),
            mock(CompraCommandService.class),
            mock(PagoCommandService.class),
            mock(ReporteCommandService.class)
        );
    }

    static Stream<Arguments> catalogoCompleto() {
        return Stream.of(
            Arguments.of("LIS_USUARIO", P, 0, 0, false),
            Arguments.of("GET_USUARIO", P, 1, 1, false),
            Arguments.of("INS_USUARIO", P, 6, 7, false),
            Arguments.of("UPD_USUARIO", P, 7, 8, false),
            Arguments.of("DEL_USUARIO", P, 1, 1, false),

            Arguments.of("LIS_PROVEEDOR", PT, 0, 0, false),
            Arguments.of("GET_PROVEEDOR", PT, 1, 1, false),
            Arguments.of("INS_PROVEEDOR", P, 4, 5, false),
            Arguments.of("UPD_PROVEEDOR", P, 5, 6, false),
            Arguments.of("DEL_PROVEEDOR", P, 1, 1, false),

            Arguments.of("LIS_CATEGORIA", PT, 0, 0, false),
            Arguments.of("GET_CATEGORIA", PT, 1, 1, false),
            Arguments.of("INS_CATEGORIA", P, 1, 2, false),
            Arguments.of("UPD_CATEGORIA", P, 2, 3, false),
            Arguments.of("DEL_CATEGORIA", P, 1, 1, false),

            Arguments.of("LIS_PRODUCTO", PTV, 0, 0, true),
            Arguments.of("GET_PRODUCTO", PTV, 1, 1, true),
            Arguments.of("INS_PRODUCTO", P, 5, 6, false),
            Arguments.of("UPD_PRODUCTO", P, 6, 7, false),
            Arguments.of("DEL_PRODUCTO", P, 1, 1, false),

            Arguments.of("LIS_ALMACEN", PT, 0, 0, false),
            Arguments.of("GET_ALMACEN", PT, 1, 1, false),
            Arguments.of("INS_ALMACEN", P, 4, 4, false),
            Arguments.of("UPD_ALMACEN", P, 5, 5, false),
            Arguments.of("DEL_ALMACEN", P, 1, 1, false),

            Arguments.of("LIS_INVENTARIO", PT, 0, 0, false),
            Arguments.of("INS_MOVIMIENTO", PT, 4, 5, false),
            Arguments.of("LIS_MOVIMIENTO", PT, 0, 0, false),
            Arguments.of("TRASLADO_STOCK", PT, 4, 4, false),

            Arguments.of("LIS_ALERTA", PT, 0, 0, false),
            Arguments.of("ATENDER_ALERTA", PT, 1, 1, false),

            Arguments.of("LIS_OFERTA", V, 0, 0, true),
            Arguments.of("INS_OFERTA", V, 2, 4, true),
            Arguments.of("UPD_OFERTA", V, 2, 5, true),
            Arguments.of("DEL_OFERTA", V, 1, 1, true),

            Arguments.of("COMPARAR_OFERTAS", P, 1, 1, false),

            Arguments.of("INS_SOLICITUD", P, 4, 4, false),
            Arguments.of("LIS_SOLICITUD", PV, 0, 0, true),
            Arguments.of("ATENDER_SOLICITUD", V, 2, 2, true),
            Arguments.of("RECHAZAR_SOLICITUD", V, 2, 2, true),

            Arguments.of("INS_COMPRA", P, 2, 2, false),
            Arguments.of("INS_DETALLE_COMPRA", P, 3, 3, false),
            Arguments.of("FINALIZAR_COMPRA", P, 1, 1, false),
            Arguments.of("RECIBIR_COMPRA", P, 1, 1, false),
            Arguments.of("LIS_COMPRA", PV, 0, 0, true),
            Arguments.of("GET_COMPRA", PV, 1, 1, true),

            Arguments.of("PAGAR_COMPRA", P, 1, 1, false),
            Arguments.of("VERIFICAR_PAGO", PV, 1, 1, true),
            Arguments.of("LIS_PAGO", PV, 0, 0, true),

            Arguments.of("REP_INVENTARIO", PT, 0, 0, false),
            Arguments.of("REP_COMPRAS", PV, 1, 1, true)
        );
    }

    @ParameterizedTest(name = "{0}: roles={1} aridad=[{2},{3}] scoped={4}")
    @MethodSource("catalogoCompleto")
    void comandoTieneLosRolesYAridadDeclaradosEnLaSpec(
        String nombre, Set<RolNombre> rolesEsperados, int aridadMin, int aridadMax, boolean scoped
    ) {
        ComandoDef def = registry.buscar(nombre);
        assertThat(def).as("El comando %s debe existir en el catalogo", nombre).isNotNull();
        assertThat(def.roles()).as("roles de %s", nombre).isEqualTo(rolesEsperados);
        assertThat(def.aridadMin()).as("aridadMin de %s", nombre).isEqualTo(aridadMin);
        assertThat(def.aridadMax()).as("aridadMax de %s", nombre).isEqualTo(aridadMax);
        assertThat(def.scopedPorProveedor()).as("scopedPorProveedor de %s", nombre).isEqualTo(scoped);
    }

    @Test
    void elCatalogoTieneExactamenteLosComandosEsperados() {
        long total = catalogoCompleto().count();
        assertThat(total).isEqualTo(51);
    }

    @Test
    void trabajadorNoPuedeInsertarCompras() {
        assertThat(registry.buscar("INS_COMPRA").roles()).doesNotContain(RolNombre.TRABAJADOR);
    }

    @Test
    void trabajadorNoPuedePagarCompras() {
        assertThat(registry.buscar("PAGAR_COMPRA").roles()).doesNotContain(RolNombre.TRABAJADOR);
    }

    @Test
    void trabajadorNoPuedeInsertarUsuarios() {
        assertThat(registry.buscar("INS_USUARIO").roles()).doesNotContain(RolNombre.TRABAJADOR);
    }

    @Test
    void proveedorNoPuedeInsertarProductosDelCatalogoInterno() {
        assertThat(registry.buscar("INS_PRODUCTO").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("UPD_PRODUCTO").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("DEL_PRODUCTO").roles()).doesNotContain(RolNombre.PROVEEDOR);
    }

    @Test
    void proveedorNoPuedeGestionarInventarioInternoNiAlmacenes() {
        assertThat(registry.buscar("INS_MOVIMIENTO").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("TRASLADO_STOCK").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("LIS_INVENTARIO").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("INS_ALMACEN").roles()).doesNotContain(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("LIS_ALMACEN").roles()).doesNotContain(RolNombre.PROVEEDOR);
    }

    @Test
    void soloElPropietarioPuedeOrdenarElPago() {
        assertThat(registry.buscar("PAGAR_COMPRA").roles()).containsExactly(RolNombre.PROPIETARIO);
    }

    @Test
    void soloElPropietarioPuedeCompararOfertasYGestionarSolicitudesDeCompra() {
        assertThat(registry.buscar("COMPARAR_OFERTAS").roles()).containsExactly(RolNombre.PROPIETARIO);
        assertThat(registry.buscar("INS_SOLICITUD").roles()).containsExactly(RolNombre.PROPIETARIO);
    }

    @Test
    void ofertasSonExclusivasDelProveedorConAislamiento() {
        for (String comando : new String[] {"LIS_OFERTA", "INS_OFERTA", "UPD_OFERTA", "DEL_OFERTA"}) {
            ComandoDef def = registry.buscar(comando);
            assertThat(def.roles()).as(comando).containsExactly(RolNombre.PROVEEDOR);
            assertThat(def.scopedPorProveedor()).as(comando + " scoped").isTrue();
        }
    }

    @Test
    void atenderYRechazarSolicitudSonExclusivosDelProveedorConOwnership() {
        assertThat(registry.buscar("ATENDER_SOLICITUD").roles()).containsExactly(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("RECHAZAR_SOLICITUD").roles()).containsExactly(RolNombre.PROVEEDOR);
        assertThat(registry.buscar("ATENDER_SOLICITUD").scopedPorProveedor()).isTrue();
        assertThat(registry.buscar("RECHAZAR_SOLICITUD").scopedPorProveedor()).isTrue();
    }

    @Test
    void comandosDeVentaEliminadosNoExistenEnElCatalogo() {
        for (String comandoEliminado : new String[] {
            "REGISTRO_CLIENTE", "INS_PEDIDO", "INS_DETALLE", "PAGAR_PEDIDO",
            "COMPROBAR_PAGO", "REP_VENTAS", "CONFIRMAR_ENTREGA", "VER_MIS_ORDENES"
        }) {
            assertThat(registry.buscar(comandoEliminado))
                .as("El comando %s no debe existir (dominio de venta eliminado)", comandoEliminado)
                .isNull();
        }
    }

    @Test
    void noExisteElRolCliente() {
        for (RolNombre rol : RolNombre.values()) {
            assertThat(rol.name()).isNotEqualTo("CLIENTE");
        }
        assertThat(RolNombre.values()).containsExactlyInAnyOrder(
            RolNombre.PROPIETARIO, RolNombre.TRABAJADOR, RolNombre.PROVEEDOR
        );
    }

    @Test
    void comandosParaRolDevuelveSoloLosAutorizadosParaCadaRol() {
        Set<String> usosPropietario = registry.comandosParaRol(RolNombre.PROPIETARIO);
        Set<String> usosTrabajador = registry.comandosParaRol(RolNombre.TRABAJADOR);
        Set<String> usosProveedor = registry.comandosParaRol(RolNombre.PROVEEDOR);

        assertThat(usosPropietario).anyMatch(uso -> uso.startsWith("PAGAR_COMPRA"));
        assertThat(usosTrabajador).noneMatch(uso -> uso.startsWith("PAGAR_COMPRA"));
        assertThat(usosProveedor).noneMatch(uso -> uso.startsWith("PAGAR_COMPRA"));

        assertThat(usosProveedor).anyMatch(uso -> uso.startsWith("LIS_OFERTA"));
        assertThat(usosPropietario).noneMatch(uso -> uso.startsWith("INS_OFERTA"));
        assertThat(usosTrabajador).noneMatch(uso -> uso.startsWith("INS_OFERTA"));
    }
}
