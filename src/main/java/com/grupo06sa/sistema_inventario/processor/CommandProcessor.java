package com.grupo06sa.sistema_inventario.processor;

import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.service.command.AlmacenCommandService;
import com.grupo06sa.sistema_inventario.service.command.CategoriaCommandService;
import com.grupo06sa.sistema_inventario.service.command.CompraCommandService;
import com.grupo06sa.sistema_inventario.service.command.PedidosCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProductoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProveedorCommandService;
import com.grupo06sa.sistema_inventario.service.command.ReporteCommandService;
import com.grupo06sa.sistema_inventario.service.command.TrasladoCommandService;
import com.grupo06sa.sistema_inventario.service.command.UsuarioCommandService;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

@Service
public class CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CommandProcessor.class);

    private final ProductoCommandService productoCommandService;
    private final CategoriaCommandService categoriaCommandService;
    private final UsuarioCommandService usuarioCommandService;
    private final PedidosCommandService pedidosCommandService;
    private final CompraCommandService compraCommandService;
    private final ReporteCommandService reporteCommandService;
    private final AlmacenCommandService almacenCommandService;
    private final TrasladoCommandService trasladoCommandService;
    private final ProveedorRepository proveedorRepository;
    private final ProveedorCommandService proveedorCommandService;

    public CommandProcessor(
            ProductoCommandService productoCommandService,
            CategoriaCommandService categoriaCommandService,
            UsuarioCommandService usuarioCommandService,
            PedidosCommandService pedidosCommandService,
            CompraCommandService compraCommandService,
            ReporteCommandService reporteCommandService,
            AlmacenCommandService almacenCommandService,
            TrasladoCommandService trasladoCommandService,
            ProveedorRepository proveedorRepository,
            ProveedorCommandService proveedorCommandService) {
        this.productoCommandService = productoCommandService;
        this.categoriaCommandService = categoriaCommandService;
        this.usuarioCommandService = usuarioCommandService;
        this.pedidosCommandService = pedidosCommandService;
        this.compraCommandService = compraCommandService;
        this.reporteCommandService = reporteCommandService;
        this.almacenCommandService = almacenCommandService;
        this.trasladoCommandService = trasladoCommandService;
        this.proveedorRepository = proveedorRepository;
        this.proveedorCommandService = proveedorCommandService;
    }

    public CommandResult process(CommandRequest request, String emailRemitente) {
        if (request == null || request.getCommand() == null) {
            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate("Error", "Solicitud inválida."));
        }

        String command = request.getCommand().trim();
        try {
            return switch (command) {
                case "HELP" -> CommandResult.text(usuarioCommandService.buildHelp(emailRemitente));
                case "REGISTRO_CLIENTE" -> CommandResult.text(
                    usuarioCommandService.registrarseComoCliente(request.getParams(), emailRemitente)
                );
                case "INS_USUARIO" -> CommandResult.text(
                    usuarioCommandService.insertarUsuario(request.getParams(), emailRemitente)
                );
                case "LIS_USUARIO" -> CommandResult.text(
                    usuarioCommandService.listarUsuarios(request.getParams(), emailRemitente)
                );
                case "GET_USUARIO" -> CommandResult.text(
                    usuarioCommandService.obtenerUsuario(request.getParams(), emailRemitente)
                );
                case "UPD_USUARIO" -> CommandResult.text(
                    usuarioCommandService.actualizarUsuario(request.getParams(), emailRemitente)
                );
                case "DEL_USUARIO" -> CommandResult.text(
                    usuarioCommandService.eliminarUsuario(request.getParams(), emailRemitente)
                );
                case "LIS_PROVEEDOR" -> CommandResult.text(
                    proveedorCommandService.listarProveedores(emailRemitente)
                );
                case "GET_PROVEEDOR" -> CommandResult.text(
                    proveedorCommandService.obtenerProveedor(request.getParams(), emailRemitente)
                );
                case "INS_PROVEEDOR" -> CommandResult.text(
                    proveedorCommandService.insertarProveedor(request.getParams(), emailRemitente)
                );
                case "UPD_PROVEEDOR" -> CommandResult.text(
                    proveedorCommandService.actualizarProveedor(request.getParams(), emailRemitente)
                );
                case "DEL_PROVEEDOR" -> CommandResult.text(
                    proveedorCommandService.eliminarProveedor(request.getParams(), emailRemitente)
                );
                case "LIS_CATEGORIA" -> CommandResult.text(
                    categoriaCommandService.listarCategorias(request.getParams(), emailRemitente)
                );
                case "GET_CATEGORIA" -> CommandResult.text(
                    categoriaCommandService.obtenerCategoria(request.getParams(), emailRemitente)
                );
                case "INS_CATEGORIA" -> CommandResult.text(
                    categoriaCommandService.insertarCategoria(request.getParams(), emailRemitente)
                );
                case "UPD_CATEGORIA" -> CommandResult.text(
                    categoriaCommandService.actualizarCategoria(request.getParams(), emailRemitente)
                );
                case "DEL_CATEGORIA" -> CommandResult.text(
                    categoriaCommandService.eliminarCategoria(request.getParams(), emailRemitente)
                );
                case "LIS_PRODUCTO" -> CommandResult.text(
                    productoCommandService.listarProductos(request.getParams(), emailRemitente)
                );
                case "GET_PRODUCTO" -> CommandResult.text(
                    productoCommandService.obtenerProducto(request.getParams(), emailRemitente)
                );
                case "INS_PRODUCTO" -> CommandResult.text(
                    productoCommandService.insertarProducto(request.getParams(), emailRemitente)
                );
                case "UPD_PRODUCTO" -> CommandResult.text(
                    productoCommandService.actualizarProducto(request.getParams(), emailRemitente)
                );
                case "DEL_PRODUCTO" -> CommandResult.text(
                    productoCommandService.eliminarProducto(request.getParams(), emailRemitente)
                );
                case "INS_PEDIDO" -> CommandResult.text(
                    pedidosCommandService.crearPedido(request.getParams(), emailRemitente)
                );
                case "INS_DETALLE" -> CommandResult.text(
                    pedidosCommandService.agregarDetalle(request.getParams(), emailRemitente)
                );
                case "PAGAR_PEDIDO" -> pedidosCommandService.pagarPedido(request.getParams(), emailRemitente);
                case "COMPROBAR_PAGO" -> CommandResult.text(
                    pedidosCommandService.comprobarPago(request.getParams(), emailRemitente)
                );
                case "INS_COMPRA" -> CommandResult.text(
                    compraCommandService.crearCompra(request.getParams(), emailRemitente)
                );
                case "INS_DETALLE_COMPRA" -> CommandResult.text(
                    compraCommandService.agregarDetalleCompra(request.getParams(), emailRemitente)
                );
                case "FINALIZAR_COMPRA" -> CommandResult.text(
                    compraCommandService.finalizarCompra(request.getParams(), emailRemitente)
                );
                case "REP_INVENTARIO" -> reporteCommandService.reporteInventario(
                    request.getParams(),
                    emailRemitente
                );
                case "REP_VENTAS" -> reporteCommandService.reporteVentas(
                    request.getParams(),
                    emailRemitente
                );
                case "LIS_ALMACEN" -> CommandResult.text(
                    almacenCommandService.listarAlmacenes(emailRemitente)
                );
                case "GET_ALMACEN" -> CommandResult.text(
                    almacenCommandService.obtenerAlmacen(request.getParams(), emailRemitente)
                );
                case "INS_ALMACEN" -> CommandResult.text(
                    almacenCommandService.crearAlmacen(request.getParams(), emailRemitente)
                );
                case "UPD_ALMACEN" -> CommandResult.text(
                    almacenCommandService.actualizarAlmacen(request.getParams(), emailRemitente)
                );
                case "DEL_ALMACEN" -> CommandResult.text(
                    almacenCommandService.eliminarAlmacen(request.getParams(), emailRemitente)
                );
                case "TRASLADO_STOCK" -> CommandResult.text(
                    trasladoCommandService.trasladarStock(request.getParams(), emailRemitente)
                );
                case "CONFIRMAR_ENTREGA" -> {
                    boolean esProveedor = proveedorRepository
                        .findByCorreoIgnoreCase(emailRemitente).isPresent();
                    if (!esProveedor) {
                        yield CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                            "Acceso denegado",
                            "Solo un proveedor registrado puede ejecutar este comando."
                        ));
                    }
                    yield CommandResult.text(
                        compraCommandService.confirmarEntrega(request.getParams(), emailRemitente)
                    );
                }
                case "VER_MIS_ORDENES" -> {
                    boolean esProveedor = proveedorRepository
                        .findByCorreoIgnoreCase(emailRemitente).isPresent();
                    if (!esProveedor) {
                        yield CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(
                            "Acceso denegado",
                            "Solo un proveedor registrado puede ejecutar este comando."
                        ));
                    }
                    yield CommandResult.text(
                        compraCommandService.verMisOrdenes(emailRemitente)
                    );
                }
                default -> CommandResult.text(usuarioCommandService.buildHelp(emailRemitente));
            };
        } catch (RuntimeException ex) {
            logger.error("Command failed: {}", command, ex);
            String title = "Error";
            String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "No se pudo procesar el comando.";

            Throwable current = ex;
            while (current != null) {
                if (current instanceof CannotCreateTransactionException
                    || current instanceof DataAccessResourceFailureException) {
                    title = "Base de datos no disponible";
                    message = "No se pudo conectar a la base de datos. Intenta nuevamente mas tarde.";
                    break;
                }
                if (current instanceof QueryTimeoutException) {
                    title = "Base de datos sin respuesta";
                    message = "La base de datos tardo demasiado en responder. Intenta nuevamente mas tarde.";
                    break;
                }
                if (current instanceof CannotAcquireLockException
                    || current instanceof ConcurrencyFailureException) {
                    title = "Base de datos ocupada";
                    message = "La base de datos esta procesando otra operacion. Intenta nuevamente.";
                    break;
                }
                if (current instanceof DataIntegrityViolationException) {
                    title = "Datos no validos";
                    message = "La base de datos rechazo la operacion porque los datos no cumplen una restriccion.";
                    break;
                }
                if (current instanceof DataAccessException
                    || current instanceof TransactionException) {
                    title = "Error de base de datos";
                    message = "La base de datos no pudo procesar la solicitud. Intenta nuevamente mas tarde.";
                    break;
                }

                current = current.getCause();
            }

            return CommandResult.text(HtmlBuilderUtil.buildErrorTemplate(title, message));
        }
    }
}
