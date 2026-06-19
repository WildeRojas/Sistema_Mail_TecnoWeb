package com.grupo06sa.sistema_inventario.util;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.DetallePedido;
import com.grupo06sa.sistema_inventario.entity.MetodoPago;
import com.grupo06sa.sistema_inventario.entity.Pedido;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import java.util.List;

public final class HtmlBuilderUtil {
    private HtmlBuilderUtil() {
    }

    public static String buildSuccessTemplate(String title, String message) {
        String content = alertBox("#d1f4d6", "#6fcf97", "#145c2f", title, message);
        return buildPage("Operación exitosa", content);
    }

    public static String buildErrorTemplate(String title, String message) {
        String content = alertBox("#f8d7da", "#f5a5b0", "#7a1f2b", title, message);
        return buildPage("Aviso", content);
    }

    public static String buildInfoTemplate(String title, String message) {
        String content = alertBox("#dbeafe", "#93c5fd", "#1e3a8a", title, message);
        return buildPage("Información", content);
    }

    public static String buildQrTemplate(String title, String message, String qrBase64) {
        return buildQrTemplate(title, message, qrBase64, null);
    }

    public static String buildComprobantePagoTemplate(Long pedidoId, double monto,
            String numeroCuenta, List<String> productosDescontados) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-size:14px;color:#374151;line-height:1.7;\">")
            .append("<div style=\"margin-bottom:6px;\"><strong>Pedido #")
            .append(pedidoId)
            .append("</strong> confirmado como <strong>PAGADO</strong>.</div>")
            .append("<div style=\"margin-bottom:4px;\">Monto cancelado: <strong>Bs. ")
            .append(String.format("%.2f", monto))
            .append("</strong></div>")
            .append("<div style=\"margin-bottom:10px;color:#6b7280;\">Cuenta destino: ")
            .append(escapeHtml(numeroCuenta))
            .append("</div>");

        if (productosDescontados != null && !productosDescontados.isEmpty()) {
            body.append("<div style=\"margin-bottom:4px;font-size:13px;color:#6b7280;\">Productos descontados del inventario:</div>")
                .append("<ul style=\"margin:0;padding-left:18px;font-size:13px;color:#4b5563;\">" );
            for (String p : productosDescontados) {
                body.append("<li>").append(escapeHtml(p)).append("</li>");
            }
            body.append("</ul>");
        }

        body.append("</div>");
        return buildPage("Pago confirmado", body.toString());
    }

    public static String buildQrTemplate(String title, String message, String qrBase64, String qrUrl) {
        StringBuilder body = new StringBuilder();
        
        body.append("<div style=\"margin-bottom:20px;font-size:15px;color:#333;\">")
            .append(escapeHtml(message))
            .append("</div>");

        String imgSrc = (qrUrl != null && !qrUrl.isBlank()) 
            ? qrUrl 
            : "cid:qr.png";

        body.append("<div style=\"text-align:center;background:#f0f7ff;border:2px solid #1a3a5c;")
            .append("border-radius:10px;padding:24px;margin:16px 0;\">")
            .append("<p style=\"font-size:15px;font-weight:bold;color:#1a3a5c;margin:0 0 16px\">")
            .append("&#128241; Escanea este código QR para pagar</p>")
            .append("<img alt=\"QR Pago Facil\" width=\"280\" height=\"280\" ")
            .append("style=\"border:1px solid #ddd;padding:10px;border-radius:8px;")
            .append("background:white;display:block;margin:0 auto 16px;\" ")
            .append("src=\"").append(imgSrc).append("\"/>")
            .append("<div style=\"text-align:left;background:white;border-radius:8px;")
            .append("padding:16px;margin-top:8px;\">")
            .append("<p style=\"margin:0 0 8px;font-weight:bold;font-size:13px;color:#555\">")
            .append("¿Cómo pagar?</p>")
            .append("<ol style=\"margin:0;padding-left:18px;font-size:13px;color:#444;line-height:1.8\">")
            .append("<li>Abre tu <strong>app bancaria</strong> (BNB, Bancosol, Tigo Money, etc.)</li>")
            .append("<li>Selecciona <strong>Pagar con QR</strong></li>")
            .append("<li>Apunta la cámara al código QR</li>")
            .append("<li>Confirma el pago en tu app</li>")
            .append("</ol></div>")
            .append("<p style=\"margin:16px 0 0;font-size:11px;color:#999;\">")
            .append("&#9888;&#65039; Este QR es de uso único y expira en pocos minutos.</p>")
            .append("</div>");

        return buildPage(title, body.toString());
    }

    public static String buildAccessDeniedTemplate() {
        return buildRegistroRequeridoTemplate();
    }

    public static String buildRegistroRequeridoTemplate() {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"background:#fff3cd;border:1px solid #ffc107;color:#856404;padding:16px;border-radius:8px;margin-bottom:16px;\">")
            .append("<div style=\"font-size:16px;font-weight:700;margin-bottom:8px;\">&#128274; No estás registrado en el sistema</div>")
            .append("<div style=\"font-size:14px;line-height:1.6;\">")
            .append("Para acceder al sistema de inventario, debes registrarte como cliente enviando un correo con el siguiente comando en el <strong>asunto</strong>:")
            .append("</div></div>")
            .append("<div style=\"background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin-bottom:12px;\">")
            .append("<div style=\"font-size:13px;color:#6b7280;margin-bottom:8px;\">Comando para registrarse:</div>")
            .append("<div style=\"font-size:15px;font-weight:600;color:#0b2a4a;font-family:monospace;\">")
            .append("REGISTRO_CLIENTE[\"nombre\",\"apellido\",\"contrasena\",\"telefono\"]")
            .append("</div></div>")
            .append("<div style=\"font-size:13px;color:#6b7280;margin-top:8px;\">")
            .append("&#128203; <strong>Ejemplo:</strong> ")
            .append("<span style=\"font-family:monospace;color:#0b2a4a;\">")
            .append("REGISTRO_CLIENTE[\"Juan\",\"Perez\",\"MiClave123\",\"70123456\"]")
            .append("</span><br>")
            .append("Tu email se registrara automaticamente como cuenta de acceso.")
            .append("</div>");
        return buildPage("Registro requerido", body.toString());
    }

    public static String buildHelpTemplate(String roleName) {
        boolean isAdmin = "ADMINISTRADOR".equalsIgnoreCase(roleName);
        StringBuilder content = new StringBuilder();

        // Comandos generales (cliente y admin)
        content.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Comandos disponibles</div>")
            .append("<ul style=\"padding-left:18px;margin:0;\">")
            .append(commandItem("REGISTRO_CLIENTE[\"nombre\",\"apellido\",\"contrasena\",\"telefono\"]", "Registrarse como cliente (email tomado automaticamente)"))
            .append(commandItem("LIS_PRODUCTO[\"*\"]", "Listar productos"))
            .append(commandItem("GET_PRODUCTO[\"id\"]", "Ver producto"))
            .append(commandItem("LIS_CATEGORIA[\"*\"]", "Listar categorias"))
            .append(commandItem("GET_CATEGORIA[\"id\"]", "Ver categoria"))
            .append(commandItem("INS_PEDIDO[\"direccion\"]", "Crear pedido"))

            .append(commandItem("INS_DETALLE[\"id_pedido\",\"id_producto\",\"cantidad\"]", "Agregar detalle al pedido"))
            .append(commandItem(
                "PAGAR_PEDIDO[\"id_pedido\",\"metodo_pago\"]",
                "Iniciar pago del pedido (genera QR)"
            ))
            .append(commandItem(
                "COMPROBAR_PAGO[\"id_pedido\",\"numero_cuenta_destinatario\",\"monto_cancelado\"]",
                "Confirmar pago QR y actualizar inventario"
            ));

        if (isAdmin) {
            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Gestion de Proveedores</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("LIS_PROVEEDOR[\"*\"]", "Listar proveedores"))
                .append(commandItem("GET_PROVEEDOR[\"id\"]", "Ver proveedor"))
                .append(commandItem("INS_PROVEEDOR[\"nombre\",\"telefono\",\"correo\",\"direccion\"]", "Registrar proveedor"))
                .append(commandItem("UPD_PROVEEDOR[\"id\",\"nombre\",\"telefono\",\"correo\",\"direccion\"]", "Actualizar proveedor"))
                .append(commandItem("DEL_PROVEEDOR[\"id\"]", "Eliminar proveedor"));

            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Compras y Stock</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("INS_COMPRA[\"id_proveedor\"]", "Crear compra"))
                .append(commandItem("INS_DETALLE_COMPRA[\"id_compra\",\"id_producto\",\"cantidad\",\"precio_compra\"]", "Agregar detalle de compra"))
                .append(commandItem("FINALIZAR_COMPRA[\"id_compra\"]", "Finalizar compra y notificar proveedor"))
                .append(commandItem("TRASLADO_STOCK[\"id_producto\",\"id_almacen_origen\",\"id_almacen_destino\",\"cantidad\"]", "Trasladar stock"));

            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Almacenes</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("LIS_ALMACEN[\"*\"]", "Listar almacenes"))
                .append(commandItem("GET_ALMACEN[\"id\"]", "Ver almacen"))
                .append(commandItem("INS_ALMACEN[\"nombre\",\"capacidad\",\"direccion\",\"coordenadas_gps\"]", "Registrar almacen"))
                .append(commandItem("UPD_ALMACEN[\"id\",\"nombre\",\"capacidad\",\"direccion\",\"coordenadas_gps\"]", "Actualizar almacen"))
                .append(commandItem("DEL_ALMACEN[\"id\"]", "Eliminar almacen"));

            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Reportes</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("REP_INVENTARIO[\"*\"]", "Reporte PDF de inventario"))
                .append(commandItem("REP_VENTAS[\"YYYY-MM\"]", "Reporte PDF de ventas"));

            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Gestion de Productos y Categorias</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("INS_PRODUCTO[\"codigo\",\"nombre\",\"precio\",\"stockMinimo\",\"categoriaId\",\"proveedorId\",\"imagen\"]", "Registrar producto"))
                .append(commandItem("UPD_PRODUCTO[\"id\",\"codigo\",\"nombre\",\"precio\",\"stockMinimo\",\"categoriaId\",\"proveedorId\",\"imagen\"]", "Actualizar producto"))
                .append(commandItem("DEL_PRODUCTO[\"id\"]", "Eliminar producto"))
                .append(commandItem("INS_CATEGORIA[\"nombre\",\"imagen\"]", "Registrar categoria"))
                .append(commandItem("UPD_CATEGORIA[\"id\",\"nombre\",\"imagen\"]", "Actualizar categoria"))
                .append(commandItem("DEL_CATEGORIA[\"id\"]", "Eliminar categoria"));

            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Gestion de Usuarios</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("INS_USUARIO[\"nombre\",\"apellido\",\"contrasena\",\"telefono\",\"email\",\"foto\",\"rol\"]", "Registrar usuario con rol especifico"))
                .append(commandItem("UPD_USUARIO[\"id\",\"nombre\",\"apellido\",\"contrasena\",\"telefono\",\"email\",\"foto\",\"rol\"]", "Actualizar usuario"))
                .append(commandItem("DEL_USUARIO[\"id\"]", "Eliminar usuario"))
                .append(commandItem("LIS_USUARIO[\"*\"]", "Listar todos los usuarios"))
                .append(commandItem("GET_USUARIO[\"id\"]", "Ver detalle de usuario"));
        }

        boolean isProveedor = "PROVEEDOR".equalsIgnoreCase(roleName);
        if (isProveedor) {
            content.append("</ul>")
                .append("<div style=\"margin-top:18px;margin-bottom:8px;color:#4b5563;font-weight:600;\">Comandos de proveedor</div>")
                .append("<ul style=\"padding-left:18px;margin:0;\">")
                .append(commandItem("VER_MIS_ORDENES[\"*\"]", "Ver mis ordenes de compra en espera"))
                .append(commandItem("CONFIRMAR_ENTREGA[\"id_compra\"]", "Confirmar entrega de una orden de compra"));
        }

        content.append(commandItem("HELP[\"*\"]", "Mostrar esta ayuda"));
        content.append("</ul>");

        return buildPage("Centro de ayuda", content.toString());
    }

    public static String buildOrdenCompraTemplate(Compra compra, List<DetalleCompra> detalles) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-size:14px;color:#374151;line-height:1.8;\">");
        body.append("<div style=\"margin-bottom:6px;\"><strong>Orden de compra #")
            .append(compra.getId())
            .append("</strong></div>");
        body.append("<div style=\"margin-bottom:10px;color:#6b7280;\">Fecha: ")
            .append(compra.getFecha() != null ? compra.getFecha().toString().replace("T", " ").substring(0, 16) : "")
            .append("</div>");

        body.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;margin-bottom:12px;\">")
            .append("<tr>")
            .append(th("Producto")).append(th("Cantidad")).append(th("Precio unitario")).append(th("Subtotal"))
            .append("</tr>");

        double total = 0.0;
        int idx = 0;
        for (DetalleCompra d : detalles) {
            String rowStyle = idx % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            int cantidad = d.getCantidad() != null ? d.getCantidad() : 0;
            double precio = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : 0.0;
            double subtotal = cantidad * precio;
            total += subtotal;
            String nombre = d.getProducto() != null ? safe(d.getProducto().getNombre()) : "";
            body.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(escapeHtml(nombre)))
                .append(td(String.valueOf(cantidad)))
                .append(td(String.format("%.2f", precio)))
                .append(td(String.format("%.2f", subtotal)))
                .append("</tr>");
            idx++;
        }

        body.append("</table>");
        body.append("<div style=\"font-size:14px;font-weight:600;color:#0b2a4a;\">Total: Bs. ")
            .append(String.format("%.2f", total)).append("</div>");
        body.append("</div>");
        return buildPage("Nueva orden de compra", body.toString());
    }

    public static String buildOrdenComprasTable(List<Compra> compras) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Mis ordenes en espera</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Fecha")).append(th("Total")).append(th("Estado"))
            .append("</tr>");

        if (compras.isEmpty()) {
            table.append("<tr><td colspan=\"4\" style=\"padding:10px;color:#6b7280;\">No tienes ordenes en espera.</td></tr>");
        }

        int idx = 0;
        for (Compra c : compras) {
            String rowStyle = idx % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String fecha = c.getFecha() != null ? c.getFecha().toString().replace("T", " ").substring(0, 16) : "";
            String total = c.getTotal() != null ? String.format("%.2f", c.getTotal()) : "0.00";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(c.getId())))
                .append(td(fecha))
                .append(td("Bs. " + total))
                .append(td(c.getEstado() != null ? c.getEstado().name() : ""))
                .append("</tr>");
            idx++;
        }

        table.append("</table>");
        return buildPage("Mis ordenes", table.toString());
    }

    public static String buildCategoriasTable(List<Categoria> categorias) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de categorías</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID"))
            .append(th("Nombre"))
            .append(th("Imagen"))
            .append("</tr>");

        int index = 0;
        for (Categoria categoria : categorias) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(categoria.getId())))
                .append(td(escapeHtml(safe(categoria.getNombre()))))
                .append(td(escapeHtml(safe(categoria.getImagen()))))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Categorías", table.toString());
    }

    public static String buildAlmacenesTable(List<com.grupo06sa.sistema_inventario.entity.Almacen> almacenes) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de almacenes</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID"))
            .append(th("Nombre"))
            .append(th("Capacidad"))
            .append(th("Direccion"))
            .append("</tr>");

        int index = 0;
        for (com.grupo06sa.sistema_inventario.entity.Almacen almacen : almacenes) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(almacen.getId())))
                .append(td(escapeHtml(safe(almacen.getNombre()))))
                .append(td(escapeHtml(safeNumber(almacen.getCapacidad()))))
                .append(td(escapeHtml(safe(almacen.getDireccion()))))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Almacenes", table.toString());
    }

    public static String buildProductosTable(List<Producto> productos) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Catálogo de productos</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID"))
            .append(th("Código"))
            .append(th("Producto"))
            .append(th("Precio"))
            .append(th("Stock"))
            .append(th("Categoría"))
            .append("</tr>");

        int index = 0;
        for (Producto producto : productos) {
            String categoriaNombre = "";
            if (producto.getCategoria() != null) {
                categoriaNombre = safe(producto.getCategoria().getNombre());
            }

            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(producto.getId())))
                .append(td(escapeHtml(safe(producto.getCodigo()))))
                .append(td(escapeHtml(safe(producto.getNombre()))))
                .append(td(escapeHtml(safeNumber(producto.getPrecio()))))
                .append(td(escapeHtml(safeNumber(producto.getStockActual()))))
                .append(td(escapeHtml(categoriaNombre)))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Productos", table.toString());
    }

    public static String buildProveedoresTable(List<Proveedor> proveedores) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de proveedores</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID"))
            .append(th("Nombre"))
            .append(th("Telefono"))
            .append(th("Correo"))
            .append(th("Direccion"))
            .append("</tr>");

        int index = 0;
        for (Proveedor p : proveedores) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(p.getId())))
                .append(td(escapeHtml(safe(p.getNombre()))))
                .append(td(escapeHtml(safe(p.getTelefono()))))
                .append(td(escapeHtml(safe(p.getCorreo()))))
                .append(td(escapeHtml(safe(p.getDireccion()))))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Proveedores", table.toString());
    }

    public static String buildUsuariosTable(List<Usuario> usuarios) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de usuarios</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID"))
            .append(th("Nombre"))
            .append(th("Email"))
            .append(th("Telefono"))
            .append(th("Rol"))
            .append("</tr>");

        int index = 0;
        for (Usuario usuario : usuarios) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String nombreCompleto = safe(usuario.getNombre()) + " " + safe(usuario.getApellido());
            String rolNombre = usuario.getRol() != null ? safe(usuario.getRol().getNombre()) : "";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(usuario.getId())))
                .append(td(escapeHtml(nombreCompleto.trim())))
                .append(td(escapeHtml(safe(usuario.getEmail()))))
                .append(td(escapeHtml(safe(usuario.getTelefono()))))
                .append(td(escapeHtml(rolNombre)))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Usuarios", table.toString());
    }

    public static String buildFacturaPedido(Pedido pedido, List<DetallePedido> detalles, MetodoPago metodoPago) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Factura de pedido</div>")
            .append("<div style=\"margin-bottom:16px;font-size:14px;\">")
            .append("<div><strong>Pedido:</strong> ").append(safeNumber(pedido.getId())).append("</div>")
            .append("<div><strong>Estado:</strong> ")
            .append(escapeHtml(safeEnum(pedido.getEstado()))).append("</div>")
            .append("<div><strong>Metodo de pago:</strong> ")
            .append(escapeHtml(safeEnum(metodoPago))).append("</div>");

        if (pedido.getUsuario() != null) {
            String cliente = safe(pedido.getUsuario().getNombre()) + " " + safe(pedido.getUsuario().getApellido());
            body.append("<div><strong>Cliente:</strong> ")
                .append(escapeHtml(cliente.trim()))
                .append("</div>");
        }

        body.append("</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("Producto"))
            .append(th("Cantidad"))
            .append(th("Precio"))
            .append(th("Subtotal"))
            .append("</tr>");

        int index = 0;
        for (DetallePedido detalle : detalles) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String productoNombre = "";
            if (detalle.getProducto() != null) {
                productoNombre = safe(detalle.getProducto().getNombre());
            }
            double precio = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : 0.0;
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            double subtotal = precio * cantidad;
            tableRow(body, rowStyle, productoNombre, cantidad, precio, subtotal);
            index++;
        }

        body.append("</table>")
            .append("<div style=\"margin-top:16px;font-size:15px;font-weight:700;\">Total: ")
            .append(escapeHtml(safeNumber(pedido.getTotal())))
            .append("</div>");

        return buildPage("Factura", body.toString());
    }

    public static String buildPlainTemplate(String title, String content) {
        String body = "<pre style=\"white-space:pre-wrap;background:#f8fafc;border:1px solid #e5e7eb;"
            + "padding:12px;border-radius:8px;font-size:13px;\">"
            + escapeHtml(content)
            + "</pre>";
        return buildPage(title, body);
    }

    private static String alertBox(String background, String border, String text, String title, String message) {
        return "<div style=\"background:" + background + ";border:1px solid " + border
            + ";color:" + text + ";padding:16px;border-radius:8px;\">"
            + "<div style=\"font-size:16px;font-weight:700;margin-bottom:6px;\">" + escapeHtml(title) + "</div>"
            + "<div style=\"font-size:14px;line-height:1.4;\">" + escapeHtml(message) + "</div>"
            + "</div>";
    }

    private static String buildPage(String title, String bodyHtml) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style=\"margin:0;padding:0;background:#f4f6f9;font-family:Arial, sans-serif;\">")
            .append("<div style=\"max-width:900px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;\">")
            .append("<div style=\"background:#0b2a4a;color:#ffffff;padding:18px 24px;\">")
            .append("<div style=\"font-size:18px;font-weight:700;\">Tiendas Junior - Sistema de Inventarios</div>");

        if (title != null && !title.isBlank()) {
            builder.append("<div style=\"font-size:13px;opacity:0.85;margin-top:4px;\">")
                .append(escapeHtml(title))
                .append("</div>");
        }

        builder.append("</div>")
            .append("<div style=\"padding:24px;\">")
            .append(bodyHtml)
            .append("</div>")
            .append("</div>")
            .append("</body></html>");

        return builder.toString();
    }

    private static String commandItem(String command, String description) {
        return "<li style=\"margin-bottom:8px;\"><span style=\"font-weight:600;color:#0b2a4a;\">"
            + escapeHtml(command)
            + "</span><span style=\"color:#4b5563;\"> - " + escapeHtml(description) + "</span></li>";
    }

    private static String th(String text) {
        return "<th style=\"text-align:left;padding:10px;border:1px solid #d0d7de;background:#0b2a4a;color:#ffffff;\">"
            + escapeHtml(text)
            + "</th>";
    }

    private static String td(String text) {
        return "<td style=\"padding:8px;border:1px solid #d0d7de;\">" + text + "</td>";
    }

    public static String escapeHtmlPublic(String value) {
        return escapeHtml(value);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeNumber(Number value) {
        return value == null ? "" : value.toString();
    }

    private static String safeEnum(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static void tableRow(
        StringBuilder table,
        String rowStyle,
        String productoNombre,
        int cantidad,
        double precio,
        double subtotal
    ) {
        table.append("<tr style=\"").append(rowStyle).append("\">")
            .append(td(escapeHtml(productoNombre)))
            .append(td(escapeHtml(String.valueOf(cantidad))))
            .append(td(escapeHtml(String.valueOf(precio))))
            .append(td(escapeHtml(String.valueOf(subtotal))))
            .append("</tr>");
    }
}
