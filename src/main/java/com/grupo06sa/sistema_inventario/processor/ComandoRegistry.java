package com.grupo06sa.sistema_inventario.processor;

import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
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
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class ComandoRegistry {
    private static final Set<RolNombre> SOLO_PROPIETARIO = EnumSet.of(RolNombre.PROPIETARIO);
    private static final Set<RolNombre> PROPIETARIO_TRABAJADOR =
        EnumSet.of(RolNombre.PROPIETARIO, RolNombre.TRABAJADOR);
    private static final Set<RolNombre> PROPIETARIO_TRABAJADOR_PROVEEDOR = EnumSet.allOf(RolNombre.class);
    private static final Set<RolNombre> SOLO_PROVEEDOR = EnumSet.of(RolNombre.PROVEEDOR);
    private static final Set<RolNombre> PROPIETARIO_PROVEEDOR =
        EnumSet.of(RolNombre.PROPIETARIO, RolNombre.PROVEEDOR);

    public record ComandoDef(
        String nombre,
        Set<RolNombre> roles,
        int aridadMin,
        int aridadMax,
        boolean scopedPorProveedor,
        String uso,
        BiFunction<ContextoAutenticado, List<String>, CommandResult> handler
    ) {
    }

    private final Map<String, ComandoDef> comandos = new LinkedHashMap<>();

    public ComandoRegistry(
        UsuarioCommandService usuarioCommandService,
        ProveedorCommandService proveedorCommandService,
        CategoriaCommandService categoriaCommandService,
        ProductoCommandService productoCommandService,
        AlmacenCommandService almacenCommandService,
        InventarioCommandService inventarioCommandService,
        AlertaCommandService alertaCommandService,
        OfertaCommandService ofertaCommandService,
        SolicitudCommandService solicitudCommandService,
        CompraCommandService compraCommandService,
        PagoCommandService pagoCommandService,
        ReporteCommandService reporteCommandService
    ) {
        registrar("LIS_USUARIO", SOLO_PROPIETARIO, 0, 0, false, "LIS_USUARIO[\"\"]", usuarioCommandService::listar);
        registrar("GET_USUARIO", SOLO_PROPIETARIO, 1, 1, false, "GET_USUARIO[\"id\"]", usuarioCommandService::obtener);
        registrar("INS_USUARIO", SOLO_PROPIETARIO, 6, 7, false,
            "INS_USUARIO[\"nombre\",\"apellido\",\"contrasena\",\"telefono\",\"email\",\"rol\",\"id_proveedor?\"]",
            usuarioCommandService::insertar);
        registrar("UPD_USUARIO", SOLO_PROPIETARIO, 7, 8, false,
            "UPD_USUARIO[\"id\",\"nombre\",\"apellido\",\"contrasena\",\"telefono\",\"email\",\"rol\",\"id_proveedor?\"]",
            usuarioCommandService::actualizar);
        registrar("DEL_USUARIO", SOLO_PROPIETARIO, 1, 1, false, "DEL_USUARIO[\"id\"]", usuarioCommandService::eliminar);

        registrar("LIS_PROVEEDOR", PROPIETARIO_TRABAJADOR, 0, 0, false, "LIS_PROVEEDOR[\"\"]", proveedorCommandService::listar);
        registrar("GET_PROVEEDOR", PROPIETARIO_TRABAJADOR, 1, 1, false, "GET_PROVEEDOR[\"id\"]", proveedorCommandService::obtener);
        registrar("INS_PROVEEDOR", SOLO_PROPIETARIO, 4, 5, false,
            "INS_PROVEEDOR[\"nombre\",\"telefono\",\"correo\",\"direccion\",\"nit?\"]", proveedorCommandService::insertar);
        registrar("UPD_PROVEEDOR", SOLO_PROPIETARIO, 5, 6, false,
            "UPD_PROVEEDOR[\"id\",\"nombre\",\"telefono\",\"correo\",\"direccion\",\"nit?\"]", proveedorCommandService::actualizar);
        registrar("DEL_PROVEEDOR", SOLO_PROPIETARIO, 1, 1, false, "DEL_PROVEEDOR[\"id\"]", proveedorCommandService::eliminar);

        registrar("LIS_CATEGORIA", PROPIETARIO_TRABAJADOR, 0, 0, false, "LIS_CATEGORIA[\"\"]", categoriaCommandService::listar);
        registrar("GET_CATEGORIA", PROPIETARIO_TRABAJADOR, 1, 1, false, "GET_CATEGORIA[\"id\"]", categoriaCommandService::obtener);
        registrar("INS_CATEGORIA", SOLO_PROPIETARIO, 1, 2, false,
            "INS_CATEGORIA[\"nombre\",\"imagen?\"]", categoriaCommandService::insertar);
        registrar("UPD_CATEGORIA", SOLO_PROPIETARIO, 2, 3, false,
            "UPD_CATEGORIA[\"id\",\"nombre\",\"imagen?\"]", categoriaCommandService::actualizar);
        registrar("DEL_CATEGORIA", SOLO_PROPIETARIO, 1, 1, false, "DEL_CATEGORIA[\"id\"]", categoriaCommandService::eliminar);

        registrar("LIS_PRODUCTO", PROPIETARIO_TRABAJADOR_PROVEEDOR, 0, 0, true,
            "LIS_PRODUCTO[\"\"]", productoCommandService::listar);
        registrar("GET_PRODUCTO", PROPIETARIO_TRABAJADOR_PROVEEDOR, 1, 1, true,
            "GET_PRODUCTO[\"id\"]", productoCommandService::obtener);
        registrar("INS_PRODUCTO", SOLO_PROPIETARIO, 5, 6, false,
            "INS_PRODUCTO[\"codigo\",\"nombre\",\"costo_unitario\",\"stock_minimo\",\"id_categoria\",\"imagen?\"]",
            productoCommandService::insertar);
        registrar("UPD_PRODUCTO", SOLO_PROPIETARIO, 6, 7, false,
            "UPD_PRODUCTO[\"id\",\"codigo\",\"nombre\",\"costo_unitario\",\"stock_minimo\",\"id_categoria\",\"imagen?\"]",
            productoCommandService::actualizar);
        registrar("DEL_PRODUCTO", SOLO_PROPIETARIO, 1, 1, false, "DEL_PRODUCTO[\"id\"]", productoCommandService::eliminar);

        registrar("LIS_ALMACEN", PROPIETARIO_TRABAJADOR, 0, 0, false, "LIS_ALMACEN[\"\"]", almacenCommandService::listar);
        registrar("GET_ALMACEN", PROPIETARIO_TRABAJADOR, 1, 1, false, "GET_ALMACEN[\"id\"]", almacenCommandService::obtener);
        registrar("INS_ALMACEN", SOLO_PROPIETARIO, 4, 4, false,
            "INS_ALMACEN[\"nombre\",\"capacidad\",\"direccion\",\"coordenadas_gps\"]", almacenCommandService::crear);
        registrar("UPD_ALMACEN", SOLO_PROPIETARIO, 5, 5, false,
            "UPD_ALMACEN[\"id\",\"nombre\",\"capacidad\",\"direccion\",\"coordenadas_gps\"]", almacenCommandService::actualizar);
        registrar("DEL_ALMACEN", SOLO_PROPIETARIO, 1, 1, false, "DEL_ALMACEN[\"id\"]", almacenCommandService::eliminar);

        registrar("LIS_INVENTARIO", PROPIETARIO_TRABAJADOR, 0, 0, false,
            "LIS_INVENTARIO[\"\"]", inventarioCommandService::listarInventario);
        registrar("INS_MOVIMIENTO", PROPIETARIO_TRABAJADOR, 4, 5, false,
            "INS_MOVIMIENTO[\"tipo(INGRESO|SALIDA|AJUSTE)\",\"id_producto\",\"id_almacen\",\"cantidad\",\"observacion?\"]",
            inventarioCommandService::insertarMovimiento);
        registrar("LIS_MOVIMIENTO", PROPIETARIO_TRABAJADOR, 0, 0, false,
            "LIS_MOVIMIENTO[\"\"]", inventarioCommandService::listarMovimientos);
        registrar("TRASLADO_STOCK", PROPIETARIO_TRABAJADOR, 4, 4, false,
            "TRASLADO_STOCK[\"id_producto\",\"id_almacen_origen\",\"id_almacen_destino\",\"cantidad\"]",
            inventarioCommandService::trasladar);

        registrar("LIS_ALERTA", PROPIETARIO_TRABAJADOR, 0, 0, false, "LIS_ALERTA[\"\"]", alertaCommandService::listar);
        registrar("ATENDER_ALERTA", PROPIETARIO_TRABAJADOR, 1, 1, false,
            "ATENDER_ALERTA[\"id_alerta\"]", alertaCommandService::atender);

        registrar("LIS_OFERTA", SOLO_PROVEEDOR, 0, 0, true, "LIS_OFERTA[\"\"]", ofertaCommandService::listar);
        registrar("INS_OFERTA", SOLO_PROVEEDOR, 2, 4, true,
            "INS_OFERTA[\"id_producto\",\"costo_unitario\",\"tiempo_reposicion_dias?\",\"cantidad_minima?\"]",
            ofertaCommandService::insertar);
        registrar("UPD_OFERTA", SOLO_PROVEEDOR, 2, 5, true,
            "UPD_OFERTA[\"id_oferta\",\"costo_unitario\",\"tiempo_reposicion_dias?\",\"cantidad_minima?\",\"disponible(SI|NO)?\"]",
            ofertaCommandService::actualizar);
        registrar("DEL_OFERTA", SOLO_PROVEEDOR, 1, 1, true, "DEL_OFERTA[\"id_oferta\"]", ofertaCommandService::eliminar);

        registrar("COMPARAR_OFERTAS", SOLO_PROPIETARIO, 1, 1, false,
            "COMPARAR_OFERTAS[\"id_producto\"]", ofertaCommandService::comparar);

        registrar("INS_SOLICITUD", SOLO_PROPIETARIO, 4, 4, false,
            "INS_SOLICITUD[\"id_proveedor\",\"id_producto\",\"cantidad\",\"id_almacen\"]", solicitudCommandService::insertar);
        registrar("LIS_SOLICITUD", PROPIETARIO_PROVEEDOR, 0, 0, true, "LIS_SOLICITUD[\"\"]", solicitudCommandService::listar);
        registrar("ATENDER_SOLICITUD", SOLO_PROVEEDOR, 2, 2, true,
            "ATENDER_SOLICITUD[\"id_solicitud\",\"costo_ofrecido\"]", solicitudCommandService::atender);
        registrar("RECHAZAR_SOLICITUD", SOLO_PROVEEDOR, 2, 2, true,
            "RECHAZAR_SOLICITUD[\"id_solicitud\",\"motivo\"]", solicitudCommandService::rechazar);

        registrar("INS_COMPRA", SOLO_PROPIETARIO, 2, 2, false,
            "INS_COMPRA[\"id_proveedor\",\"id_almacen\"]", compraCommandService::crear);
        registrar("INS_DETALLE_COMPRA", SOLO_PROPIETARIO, 3, 3, false,
            "INS_DETALLE_COMPRA[\"id_compra\",\"id_oferta\",\"cantidad\"]", compraCommandService::agregarDetalle);
        registrar("FINALIZAR_COMPRA", SOLO_PROPIETARIO, 1, 1, false,
            "FINALIZAR_COMPRA[\"id_compra\"]", compraCommandService::finalizar);
        registrar("RECIBIR_COMPRA", SOLO_PROPIETARIO, 1, 1, false,
            "RECIBIR_COMPRA[\"id_compra\"]", compraCommandService::recibir);
        registrar("LIS_COMPRA", PROPIETARIO_PROVEEDOR, 0, 0, true, "LIS_COMPRA[\"\"]", compraCommandService::listar);
        registrar("GET_COMPRA", PROPIETARIO_PROVEEDOR, 1, 1, true, "GET_COMPRA[\"id\"]", compraCommandService::obtener);

        registrar("PAGAR_COMPRA", SOLO_PROPIETARIO, 1, 1, false,
            "PAGAR_COMPRA[\"id_compra\"]", pagoCommandService::pagar);
        registrar("VERIFICAR_PAGO", PROPIETARIO_PROVEEDOR, 1, 1, true,
            "VERIFICAR_PAGO[\"id_compra\"]", pagoCommandService::verificar);
        registrar("LIS_PAGO", PROPIETARIO_PROVEEDOR, 0, 0, true, "LIS_PAGO[\"\"]", pagoCommandService::listar);

        registrar("REP_INVENTARIO", PROPIETARIO_TRABAJADOR, 0, 0, false,
            "REP_INVENTARIO[\"\"]", reporteCommandService::reporteInventario);
        registrar("REP_COMPRAS", PROPIETARIO_PROVEEDOR, 1, 1, true,
            "REP_COMPRAS[\"YYYY-MM\"]", reporteCommandService::reporteCompras);
    }

    private void registrar(
        String nombre,
        Set<RolNombre> roles,
        int aridadMin,
        int aridadMax,
        boolean scopedPorProveedor,
        String uso,
        BiFunction<ContextoAutenticado, List<String>, CommandResult> handler
    ) {
        comandos.put(nombre, new ComandoDef(nombre, roles, aridadMin, aridadMax, scopedPorProveedor, uso, handler));
    }

    public ComandoDef buscar(String nombre) {
        return comandos.get(nombre);
    }

    public List<ComandoDef> definicionesParaRol(RolNombre rol) {
        List<ComandoDef> resultado = new ArrayList<>();
        for (ComandoDef def : comandos.values()) {
            if (def.roles().contains(rol)) {
                resultado.add(def);
            }
        }
        return resultado;
    }

    public Set<String> comandosParaRol(RolNombre rol) {
        Set<String> resultado = new TreeSet<>();
        for (ComandoDef def : definicionesParaRol(rol)) {
            resultado.add(def.uso());
        }
        return resultado;
    }
}
