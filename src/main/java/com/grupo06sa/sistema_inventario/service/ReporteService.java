package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Estado;
import com.grupo06sa.sistema_inventario.entity.Pedido;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.repository.PedidoRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;

    public ReporteService(ProductoRepository productoRepository, PedidoRepository pedidoRepository) {
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public ReporteData generarInventario() {
        List<Producto> productos = productoRepository.findAll();
        if (productos.isEmpty()) {
            return ReporteData.empty();
        }

        byte[] pdf = buildInventarioPdf(productos);
        String fileName = "reporte_inventario_" + FILE_DATE.format(LocalDate.now()) + ".pdf";
        return new ReporteData(pdf, productos.size(), 0.0, fileName);
    }

    public ReporteData generarVentas(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);

        List<Pedido> pedidos = pedidoRepository.findByEstadoAndFechaBetween(Estado.PAGADO, start, end);
        if (pedidos.isEmpty()) {
            return ReporteData.empty();
        }

        double total = pedidos.stream()
            .map(Pedido::getTotal)
            .filter(value -> value != null)
            .mapToDouble(Double::doubleValue)
            .sum();

        byte[] pdf = buildVentasPdf(pedidos, month, total);
        String fileName = "reporte_ventas_" + month + ".pdf";
        return new ReporteData(pdf, pedidos.size(), total, fileName);
    }

    private byte[] buildInventarioPdf(List<Producto> productos) {
        Document document = new Document(PageSize.A4);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title("Reporte de Inventario"));
            document.add(new Paragraph("Fecha: " + DATE_FORMAT.format(LocalDate.now())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {3f, 1.2f, 1.2f, 2f});
            addHeader(table, "Producto", "Stock Actual", "Stock Minimo", "Categoria");

            for (Producto producto : productos) {
                table.addCell(cell(safe(producto.getNombre())));
                table.addCell(cell(String.valueOf(safeInt(producto.getStockActual()))));
                table.addCell(cell(String.valueOf(safeInt(producto.getStockMinimo()))));
                String categoria = producto.getCategoria() != null
                    ? safe(producto.getCategoria().getNombre())
                    : "";
                table.addCell(cell(categoria));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de inventario.", ex);
        }
    }

    private byte[] buildVentasPdf(List<Pedido> pedidos, YearMonth month, double total) {
        Document document = new Document(PageSize.A4);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title("Reporte de Ventas"));
            document.add(new Paragraph("Mes: " + month));
            document.add(new Paragraph("Total ingresos: " + total));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {1.2f, 2f, 3f, 1.4f});
            addHeader(table, "Pedido", "Fecha", "Cliente", "Total");

            for (Pedido pedido : pedidos) {
                table.addCell(cell(String.valueOf(pedido.getId())));
                LocalDateTime fecha = pedido.getFecha();
                table.addCell(cell(fecha != null ? fecha.toLocalDate().toString() : ""));
                String cliente = "";
                if (pedido.getUsuario() != null) {
                    cliente = safe(pedido.getUsuario().getNombre())
                        + " "
                        + safe(pedido.getUsuario().getApellido());
                }
                table.addCell(cell(cliente.trim()));
                table.addCell(cell(String.valueOf(pedido.getTotal() != null ? pedido.getTotal() : 0.0)));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de ventas.", ex);
        }
    }

    private Paragraph title(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(8f);
        return paragraph;
    }

    private void addHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value));
        cell.setPadding(6f);
        return cell;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class ReporteData {
        private final byte[] pdf;
        private final int count;
        private final double total;
        private final String fileName;

        public ReporteData(byte[] pdf, int count, double total, String fileName) {
            this.pdf = pdf;
            this.count = count;
            this.total = total;
            this.fileName = fileName;
        }

        public static ReporteData empty() {
            return new ReporteData(null, 0, 0.0, "");
        }

        public byte[] getPdf() {
            return pdf;
        }

        public int getCount() {
            return count;
        }

        public double getTotal() {
            return total;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
