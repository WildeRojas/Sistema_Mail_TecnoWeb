package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OfertasYCatalogoIntegrationTest extends IntegracionBaseTest {

    @Test
    void compararOfertasOrdenaPorCostoYMarcaLaMejorDisponible() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);

        Categoria categoria = crearCategoria("Cat Comparar");
        Producto producto = crearProducto("Producto Comparado", new BigDecimal("100"), new BigDecimal("0"), categoria);

        Proveedor proveedorCaro = crearProveedor("Proveedor Caro", emailUnico("caro"));
        Proveedor proveedorBarato = crearProveedor("Proveedor Barato", emailUnico("barato"));
        Proveedor proveedorNoDisponible = crearProveedor("Proveedor No Disponible", emailUnico("nodisp"));

        crearOferta(proveedorCaro, producto, new BigDecimal("500.00"));
        crearOferta(proveedorBarato, producto, new BigDecimal("300.00"));

        ProveedorProducto ofertaNoDisponible = crearOferta(proveedorNoDisponible, producto, new BigDecimal("100.00"));
        ofertaNoDisponible.setDisponible(false);
        ofertaRepository.save(ofertaNoDisponible);

        CommandResult resultado = commandProcessor.process(
            "COMPARAR_OFERTAS[\"" + producto.getId() + "\"]", emailPropietario
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        String body = resultado.getBody();

        int idxNoDisponibleMarcadaMejor = body.indexOf("Proveedor No Disponible");
        int idxBaratoMejor = body.indexOf("Proveedor Barato");
        assertThat(idxBaratoMejor).isGreaterThanOrEqualTo(0);
        assertThat(idxNoDisponibleMarcadaMejor).isGreaterThanOrEqualTo(0);

        int idxCaro = body.indexOf("Proveedor Caro");
        assertThat(idxNoDisponibleMarcadaMejor).isLessThan(idxBaratoMejor);
        assertThat(idxBaratoMejor).isLessThan(idxCaro);
    }

    @Test
    void insDetalleCompraConOfertaQueNoPerteneceAlProveedorDeLaCompraEsRechazado() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);

        Categoria categoria = crearCategoria("Cat Ownership");
        Producto producto = crearProducto("Producto Ownership", new BigDecimal("50"), new BigDecimal("0"), categoria);
        Almacen almacen = crearAlmacen("Almacen Ownership", new BigDecimal("1000"));

        Proveedor proveedorA = crearProveedor("Proveedor A Ownership", emailUnico("provA"));
        Proveedor proveedorB = crearProveedor("Proveedor B Ownership", emailUnico("provB"));
        crearOferta(proveedorA, producto, new BigDecimal("80.00"));
        ProveedorProducto ofertaDeB = crearOferta(proveedorB, producto, new BigDecimal("90.00"));

        CommandResult rCompra = commandProcessor.process(
            "INS_COMPRA[\"" + proveedorA.getId() + "\",\"" + almacen.getId() + "\"]", emailPropietario
        );
        assertThat(rCompra.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        Compra compra = compraRepository.findByProveedorId(proveedorA.getId()).get(0);

        CommandResult rDetalle = commandProcessor.process(
            "INS_DETALLE_COMPRA[\"" + compra.getId() + "\",\"" + ofertaDeB.getId() + "\",\"5\"]",
            emailPropietario
        );

        assertThat(rDetalle.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(rDetalle.getBody()).contains("no pertenece al proveedor");
        assertThat(detalleCompraRepository.findByCompraId(compra.getId())).isEmpty();
    }

    @Test
    void insCompraRechazaProveedorSinOfertasDisponibles() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);
        Almacen almacen = crearAlmacen("Almacen Sin Ofertas", new BigDecimal("1000"));
        Proveedor proveedorSinOfertas = crearProveedor("Proveedor Sin Ofertas", emailUnico("sinofertas"));

        CommandResult resultado = commandProcessor.process(
            "INS_COMPRA[\"" + proveedorSinOfertas.getId() + "\",\"" + almacen.getId() + "\"]",
            emailPropietario
        );

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("no tiene ofertas disponibles");
        assertThat(compraRepository.findByProveedorId(proveedorSinOfertas.getId())).isEmpty();
    }

    @Test
    void comandosDeVentaEliminadosSonDesconocidosParaCommandProcessor() {
        String emailPropietario = emailUnico("propietario");
        crearPropietario(emailPropietario);

        for (String comandoEliminado : new String[] {
            "REGISTRO_CLIENTE", "INS_PEDIDO", "PAGAR_PEDIDO", "COMPROBAR_PAGO", "REP_VENTAS"
        }) {
            CommandResult resultado = commandProcessor.process(comandoEliminado, emailPropietario);
            assertThat(resultado.getTipo())
                .as("El comando %s debe ser desconocido (dominio de venta eliminado)", comandoEliminado)
                .isEqualTo(CommandResult.Tipo.COMANDO_DESCONOCIDO);
        }
    }

    @Test
    void noExisteUnUsuarioConRolClienteEnLaBaseDeDatos() {

        assertThat(rolRepository.findByNombreIgnoreCase("CLIENTE")).isEmpty();
    }
}
