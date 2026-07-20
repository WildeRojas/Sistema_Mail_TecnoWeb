package com.grupo06sa.sistema_inventario.util;

import com.grupo06sa.sistema_inventario.entity.AlertaStock;
import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

public final class HtmlBuilderUtil {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    public static String buildQrTemplate(String title, String message, String qrBase64, String qrUrl) {
        StringBuilder body = new StringBuilder();

        body.append("<div style=\"margin-bottom:20px;font-size:15px;color:#333;\">")
            .append(escapeHtml(message))
            .append("</div>");

        String imgSrc = (qrUrl != null && !qrUrl.isBlank()) ? qrUrl : "cid:qr.png";

        body.append("<div style=\"text-align:center;background:#f0f7ff;border:2px solid #1a3a5c;")
            .append("border-radius:10px;padding:24px;margin:16px 0;\">")
            .append("<p style=\"font-size:15px;font-weight:bold;color:#1a3a5c;margin:0 0 16px\">")
            .append("Escanea este código QR para pagar</p>")
            .append("<img alt=\"QR Pago Facil\" width=\"280\" height=\"280\" ")
            .append("style=\"border:1px solid #ddd;padding:10px;border-radius:8px;")
            .append("background:white;display:block;margin:0 auto 16px;\" ")
            .append("src=\"").append(imgSrc).append("\"/>")
            .append("<div style=\"text-align:left;background:white;border-radius:8px;")
            .append("padding:16px;margin-top:8px;\">")
            .append("<p style=\"margin:0 0 8px;font-weight:bold;font-size:13px;color:#555\">")
            .append("Cómo pagar</p>")
            .append("<ol style=\"margin:0;padding-left:18px;font-size:13px;color:#444;line-height:1.8\">")
            .append("<li>Abre tu aplicación bancaria</li>")
            .append("<li>Selecciona Pagar con QR</li>")
            .append("<li>Apunta la cámara al código QR</li>")
            .append("<li>Confirma el pago en tu aplicación</li>")
            .append("</ol></div>")
            .append("<p style=\"margin:16px 0 0;font-size:11px;color:#999;\">")
            .append("Este QR es de uso único y expira en pocos minutos.</p>")
            .append("</div>");

        return buildPage(title, body.toString());
    }

    public static String buildComandoDesconocidoTemplate(String comandoIngresado, List<String> sugerenciasUso) {
        StringBuilder inner = new StringBuilder();
        inner.append("<div style=\"font-size:16px;font-weight:700;margin-bottom:8px;\">Comando no reconocido</div>");
        inner.append("<div style=\"font-size:14px;line-height:1.6;\">");
        inner.append("No se reconoció el comando \"").append(escapeHtml(safe(comandoIngresado))).append("\".");

        if (sugerenciasUso != null && !sugerenciasUso.isEmpty()) {
            inner.append("<br/><br/>Quizás quiso escribir (sintaxis de uso):");
            inner.append("<ul style=\"margin:6px 0 0;padding-left:18px;\">");
            for (String uso : sugerenciasUso) {
                inner.append("<li style=\"font-family:monospace;\">").append(escapeHtml(uso)).append("</li>");
            }
            inner.append("</ul>");
        }

        inner.append("<br/>Envíe HELP para ver la lista completa de comandos disponibles para su rol.");
        inner.append("</div>");

        String body = "<div style=\"background:#f8d7da;border:1px solid #f5a5b0;color:#7a1f2b;padding:16px;"
            + "border-radius:8px;\">" + inner + "</div>";
        return buildPage("Aviso", body);
    }

    public static String buildAccessDeniedTemplate() {
        return buildRegistroRequeridoTemplate();
    }

    public static String buildRegistroRequeridoTemplate() {
        String body = "<div style=\"background:#fff3cd;border:1px solid #ffc107;color:#856404;padding:16px;"
            + "border-radius:8px;\">"
            + "<div style=\"font-size:16px;font-weight:700;margin-bottom:8px;\">No estás registrado en el sistema</div>"
            + "<div style=\"font-size:14px;line-height:1.6;\">"
            + "Tu dirección de correo no está registrada o tu cuenta está inactiva. "
            + "Contacta al propietario del negocio para que te registre en el sistema."
            + "</div></div>";
        return buildPage("Registro requerido", body);
    }

    public static String buildHelpTemplate(RolNombre rol, Collection<String> comandosAutorizados) {
        StringBuilder content = new StringBuilder();
        content.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Comandos disponibles para tu rol (")
            .append(escapeHtml(rol.name()))
            .append(")</div>")
            .append("<ul style=\"padding-left:18px;margin:0;\">")
            .append(commandItem("HELP", "Mostrar esta ayuda"));

        for (String uso : comandosAutorizados) {
            content.append(commandItem(uso, ""));
        }

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
            .append(compra.getFecha() != null ? FECHA_HORA.format(compra.getFecha()) : "")
            .append("</div>");

        body.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;margin-bottom:12px;\">")
            .append("<tr>")
            .append(th("Producto")).append(th("Cantidad")).append(th("Costo unitario")).append(th("Subtotal"))
            .append("</tr>");

        int idx = 0;
        for (DetalleCompra d : detalles) {
            String rowStyle = idx % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String nombre = d.getProducto() != null ? safe(d.getProducto().getNombre()) : "";
            body.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(escapeHtml(nombre)))
                .append(td(plain(d.getCantidad())))
                .append(td(plain(d.getCostoUnitario())))
                .append(td(plain(d.getSubtotal())))
                .append("</tr>");
            idx++;
        }

        body.append("</table>");
        body.append("<div style=\"font-size:14px;font-weight:600;color:#0b2a4a;\">Total: Bs. ")
            .append(plain(compra.getTotal())).append("</div>");
        body.append("</div>");
        return buildPage("Nueva orden de compra", body.toString());
    }

    public static String buildSolicitudCompraTemplate(SolicitudCompra solicitud) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-size:14px;color:#374151;line-height:1.8;\">")
            .append("<div style=\"margin-bottom:6px;\"><strong>Solicitud de compra #")
            .append(solicitud.getId()).append("</strong></div>")
            .append("<div>Producto: ").append(escapeHtml(solicitud.getProducto() != null ? safe(solicitud.getProducto().getNombre()) : "")).append("</div>")
            .append("<div>Cantidad solicitada: ").append(plain(solicitud.getCantidad())).append("</div>")
            .append("<div>Almacén destino: ").append(escapeHtml(solicitud.getAlmacen() != null ? safe(solicitud.getAlmacen().getNombre()) : "")).append("</div>")
            .append("<div>Costo estimado: Bs. ").append(plain(solicitud.getCostoEstimado())).append("</div>")
            .append("<div style=\"margin-top:10px;color:#6b7280;\">Responde con ATENDER_SOLICITUD o RECHAZAR_SOLICITUD.</div>")
            .append("</div>");
        return buildPage("Nueva solicitud de compra", body.toString());
    }

    public static String buildCompraDetalleTemplate(Compra compra, List<DetalleCompra> detalles) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-size:14px;color:#374151;line-height:1.8;\">")
            .append("<div>ID: ").append(compra.getId()).append("</div>")
            .append("<div>Proveedor: ").append(escapeHtml(compra.getProveedor() != null ? safe(compra.getProveedor().getNombre()) : "")).append("</div>")
            .append("<div>Almacén destino: ").append(escapeHtml(compra.getAlmacen() != null ? safe(compra.getAlmacen().getNombre()) : "")).append("</div>")
            .append("<div>Estado: ").append(compra.getEstado() != null ? compra.getEstado().name() : "").append("</div>")
            .append("<div>Fecha: ").append(compra.getFecha() != null ? FECHA_HORA.format(compra.getFecha()) : "").append("</div>")
            .append("<div>Total: Bs. ").append(plain(compra.getTotal())).append("</div>")
            .append("<div>Pagado: Bs. ").append(plain(compra.totalPagado())).append("</div>")
            .append("<div>Saldo: Bs. ").append(plain(compra.saldo())).append("</div>")
            .append("</div>");

        body.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;margin-top:12px;\">")
            .append("<tr>")
            .append(th("Producto")).append(th("Cantidad")).append(th("Costo unitario")).append(th("Subtotal"))
            .append("</tr>");
        int idx = 0;
        for (DetalleCompra d : detalles) {
            String rowStyle = idx % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            body.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(escapeHtml(d.getProducto() != null ? safe(d.getProducto().getNombre()) : "")))
                .append(td(plain(d.getCantidad())))
                .append(td(plain(d.getCostoUnitario())))
                .append(td(plain(d.getSubtotal())))
                .append("</tr>");
            idx++;
        }
        body.append("</table>");
        return buildPage("Detalle de compra #" + compra.getId(), body.toString());
    }

    public static String buildComprasTable(List<Compra> compras) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de compras</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Proveedor")).append(th("Fecha")).append(th("Estado")).append(th("Total"))
            .append("</tr>");

        int idx = 0;
        for (Compra c : compras) {
            String rowStyle = idx % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String fecha = c.getFecha() != null ? FECHA_HORA.format(c.getFecha()) : "";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(c.getId())))
                .append(td(escapeHtml(c.getProveedor() != null ? safe(c.getProveedor().getNombre()) : "")))
                .append(td(fecha))
                .append(td(c.getEstado() != null ? c.getEstado().name() : ""))
                .append(td("Bs. " + plain(c.getTotal())))
                .append("</tr>");
            idx++;
        }

        table.append("</table>");
        return buildPage("Compras", table.toString());
    }

    public static String buildCategoriasTable(List<Categoria> categorias) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de categorías</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Nombre")).append(th("Imagen"))
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

    public static String buildAlmacenesTable(List<Almacen> almacenes) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Listado de almacenes</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Nombre")).append(th("Capacidad")).append(th("Direccion"))
            .append("</tr>");

        int index = 0;
        for (Almacen almacen : almacenes) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(almacen.getId())))
                .append(td(escapeHtml(safe(almacen.getNombre()))))
                .append(td(escapeHtml(plain(almacen.getCapacidad()))))
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
            .append(th("ID")).append(th("Código")).append(th("Producto"))
            .append(th("Costo unitario")).append(th("Stock mínimo")).append(th("Categoría"))
            .append("</tr>");

        int index = 0;
        for (Producto producto : productos) {
            String categoriaNombre = producto.getCategoria() != null ? safe(producto.getCategoria().getNombre()) : "";
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(producto.getId())))
                .append(td(escapeHtml(safe(producto.getCodigo()))))
                .append(td(escapeHtml(safe(producto.getNombre()))))
                .append(td(escapeHtml(plain(producto.getCostoUnitario()))))
                .append(td(escapeHtml(plain(producto.getStockMinimo()))))
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
            .append(th("ID")).append(th("Nombre")).append(th("Telefono")).append(th("Correo"))
            .append(th("Direccion")).append(th("NIT"))
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
                .append(td(escapeHtml(safe(p.getNit()))))
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
            .append(th("ID")).append(th("Nombre")).append(th("Email")).append(th("Telefono"))
            .append(th("Rol")).append(th("Activo"))
            .append("</tr>");

        int index = 0;
        for (Usuario usuario : usuarios) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            String nombreCompleto = (safe(usuario.getNombre()) + " " + safe(usuario.getApellido())).trim();
            String rolNombre = usuario.getRol() != null ? safe(usuario.getRol().getNombre()) : "";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(usuario.getId())))
                .append(td(escapeHtml(nombreCompleto)))
                .append(td(escapeHtml(safe(usuario.getEmail()))))
                .append(td(escapeHtml(safe(usuario.getTelefono()))))
                .append(td(escapeHtml(rolNombre)))
                .append(td(usuario.isActivo() ? "SI" : "NO"))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Usuarios", table.toString());
    }

    public static String buildInventarioTable(List<Inventario> existencias) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Stock por producto y almacén</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("Producto")).append(th("Almacén")).append(th("Cantidad"))
            .append("</tr>");

        int index = 0;
        for (Inventario inv : existencias) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(escapeHtml(inv.getProducto() != null ? safe(inv.getProducto().getNombre()) : "")))
                .append(td(escapeHtml(inv.getAlmacen() != null ? safe(inv.getAlmacen().getNombre()) : "")))
                .append(td(plain(inv.getCantidad())))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Inventario", table.toString());
    }

    public static String buildMovimientosTable(List<MovimientoInventario> movimientos) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Movimientos de inventario</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">")
            .append("<tr>")
            .append(th("Fecha")).append(th("Tipo")).append(th("Producto")).append(th("Cantidad"))
            .append(th("Origen")).append(th("Destino")).append(th("Observación"))
            .append("</tr>");

        int index = 0;
        for (MovimientoInventario m : movimientos) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(m.getFecha() != null ? FECHA_HORA.format(m.getFecha()) : ""))
                .append(td(m.getTipoOperacion() != null ? escapeHtml(safe(m.getTipoOperacion().getNombre())) : ""))
                .append(td(escapeHtml(m.getProducto() != null ? safe(m.getProducto().getNombre()) : "")))
                .append(td(plain(m.getCantidad())))
                .append(td(escapeHtml(m.getAlmacenOrigen() != null ? safe(m.getAlmacenOrigen().getNombre()) : "")))
                .append(td(escapeHtml(m.getAlmacenDestino() != null ? safe(m.getAlmacenDestino().getNombre()) : "")))
                .append(td(escapeHtml(safe(m.getObservacion()))))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Movimientos de inventario", table.toString());
    }

    public static String buildAlertasTable(List<AlertaStock> alertas) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Alertas de stock pendientes</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Producto")).append(th("Almacén")).append(th("Stock actual")).append(th("Stock mínimo"))
            .append("</tr>");

        int index = 0;
        for (AlertaStock a : alertas) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(a.getId())))
                .append(td(escapeHtml(a.getProducto() != null ? safe(a.getProducto().getNombre()) : "")))
                .append(td(escapeHtml(a.getAlmacen() != null ? safe(a.getAlmacen().getNombre()) : "")))
                .append(td(plain(a.getCantidadActual())))
                .append(td(plain(a.getStockMinimo())))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Alertas de stock", table.toString());
    }

    public static String buildOfertasTable(List<ProveedorProducto> ofertas) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Mis ofertas</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Producto")).append(th("Costo actual")).append(th("Costo anterior"))
            .append(th("Tiempo reposición (días)")).append(th("Cant. mínima")).append(th("Disponible"))
            .append("</tr>");

        int index = 0;
        for (ProveedorProducto oferta : ofertas) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(oferta.getId())))
                .append(td(escapeHtml(oferta.getProducto() != null ? safe(oferta.getProducto().getNombre()) : "")))
                .append(td(plain(oferta.getCostoUnitarioActual())))
                .append(td(plain(oferta.getCostoUnitarioAnterior())))
                .append(td(String.valueOf(oferta.getTiempoReposicionDias())))
                .append(td(plain(oferta.getCantidadMinimaPedido())))
                .append(td(oferta.isDisponible() ? "SI" : "NO"))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Ofertas de proveedor", table.toString());
    }

    public static String buildComparacionOfertasTable(List<ProveedorProducto> ofertas) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Comparación de ofertas por producto</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("Proveedor")).append(th("Costo")).append(th("Disponible")).append(th("Mejor oferta"))
            .append("</tr>");

        boolean mejorAsignada = false;
        int index = 0;
        for (ProveedorProducto oferta : ofertas) {
            boolean esMejor = !mejorAsignada && oferta.isDisponible();
            if (esMejor) {
                mejorAsignada = true;
            }
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(escapeHtml(oferta.getProveedor() != null ? safe(oferta.getProveedor().getNombre()) : "")))
                .append(td(plain(oferta.getCostoUnitarioActual())))
                .append(td(oferta.isDisponible() ? "SI" : "NO"))
                .append(td(esMejor ? "SI" : ""))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Comparación de ofertas", table.toString());
    }

    public static String buildSolicitudesTable(List<SolicitudCompra> solicitudes) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Solicitudes de compra</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Proveedor")).append(th("Producto")).append(th("Cantidad"))
            .append(th("Estado")).append(th("Costo estimado")).append(th("Costo ofrecido"))
            .append("</tr>");

        int index = 0;
        for (SolicitudCompra s : solicitudes) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(s.getId())))
                .append(td(escapeHtml(s.getProveedor() != null ? safe(s.getProveedor().getNombre()) : "")))
                .append(td(escapeHtml(s.getProducto() != null ? safe(s.getProducto().getNombre()) : "")))
                .append(td(plain(s.getCantidad())))
                .append(td(s.getEstado() != null ? s.getEstado().name() : ""))
                .append(td(plain(s.getCostoEstimado())))
                .append(td(plain(s.getCostoOfrecido())))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Solicitudes de compra", table.toString());
    }

    public static String buildPagosTable(List<Pago> pagos) {
        StringBuilder table = new StringBuilder();
        table.append("<div style=\"margin-bottom:12px;color:#4b5563;\">Pagos</div>")
            .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">")
            .append("<tr>")
            .append(th("ID")).append(th("Compra")).append(th("Monto")).append(th("Estado"))
            .append(th("Referencia")).append(th("Fecha de pago"))
            .append("</tr>");

        int index = 0;
        for (Pago p : pagos) {
            String rowStyle = index % 2 == 0 ? "background:#f7f9fc;" : "background:#ffffff;";
            table.append("<tr style=\"").append(rowStyle).append("\">")
                .append(td(String.valueOf(p.getId())))
                .append(td(p.getCompra() != null ? String.valueOf(p.getCompra().getId()) : ""))
                .append(td(plain(p.getMonto())))
                .append(td(p.getEstado() != null ? p.getEstado().name() : ""))
                .append(td(escapeHtml(safe(p.getReferencia()))))
                .append(td(p.getFechaPago() != null ? FECHA_HORA.format(p.getFechaPago()) : ""))
                .append("</tr>");
            index++;
        }

        table.append("</table>");
        return buildPage("Pagos", table.toString());
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

    private static String commandItem(String uso, String description) {
        String descripcion = description == null || description.isBlank() ? "" : " - " + escapeHtml(description);
        return "<li style=\"margin-bottom:8px;\"><span style=\"font-weight:600;color:#0b2a4a;font-family:monospace;\">"
            + escapeHtml(uso)
            + "</span><span style=\"color:#4b5563;\">" + descripcion + "</span></li>";
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

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
