package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final CompraRepository compraRepository;

    public ReporteService(
        ProductoRepository productoRepository,
        InventarioRepository inventarioRepository,
        CompraRepository compraRepository
    ) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.compraRepository = compraRepository;
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

    public ReporteData generarCompras(YearMonth mes, Long proveedorIdOpt) {
        LocalDateTimeRange rango = rangoDelMes(mes);

        List<Compra> compras = proveedorIdOpt != null
            ? compraRepository.findByProveedorIdAndFechaBetween(proveedorIdOpt, rango.inicio(), rango.fin())
            : compraRepository.findByFechaBetween(rango.inicio(), rango.fin());
        if (compras.isEmpty()) {
            return ReporteData.empty();
        }

        byte[] pdf = buildComprasPdf(compras, mes);
        String fileName = "reporte_compras_" + mes + ".pdf";
        BigDecimal total = compras.stream()
            .map(c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteData(pdf, compras.size(), total.doubleValue(), fileName);
    }

    private LocalDateTimeRange rangoDelMes(YearMonth mes) {
        return new LocalDateTimeRange(
            mes.atDay(1).atStartOfDay(),
            mes.atEndOfMonth().atTime(LocalTime.MAX)
        );
    }

    private record LocalDateTimeRange(java.time.LocalDateTime inicio, java.time.LocalDateTime fin) {
    }

    private byte[] buildInventarioPdf(List<Producto> productos) {
        Document document = new Document(PageSize.A4);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title("Reporte de Inventario"));
            document.add(new Paragraph("Fecha: " + DATE_FORMAT.format(LocalDate.now())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {2.5f, 3f, 1.2f, 1.2f, 1.8f});
            addHeader(table, "Producto", "Stock por almacén", "Stock total", "Stock mínimo", "Categoría");

            for (Producto producto : productos) {
                List<Inventario> existencias = inventarioRepository.findByProductoId(producto.getId());
                BigDecimal total = existencias.stream()
                    .map(inv -> inv.getCantidad() != null ? inv.getCantidad() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                String detalleAlmacenes = existencias.isEmpty()
                    ? "Sin existencias"
                    : existencias.stream()
                        .map(inv -> safe(inv.getAlmacen() != null ? inv.getAlmacen().getNombre() : "") + ": "
                            + plain(inv.getCantidad()))
                        .reduce((a, b) -> a + ", " + b).orElse("");

                table.addCell(cell(safe(producto.getNombre())));
                table.addCell(cell(detalleAlmacenes));
                table.addCell(cell(plain(total)));
                table.addCell(cell(plain(producto.getStockMinimo())));
                String categoria = producto.getCategoria() != null ? safe(producto.getCategoria().getNombre()) : "";
                table.addCell(cell(categoria));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de inventario.", ex);
        }
    }

    private byte[] buildComprasPdf(List<Compra> compras, YearMonth mes) {
        Document document = new Document(PageSize.A4);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title("Reporte de Compras - " + mes));
            document.add(new Paragraph("Fecha de generación: " + DATE_FORMAT.format(LocalDate.now())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {0.8f, 2.5f, 1.5f, 1.5f, 1.5f});
            addHeader(table, "ID", "Proveedor", "Fecha", "Estado", "Total");

            for (Compra compra : compras) {
                table.addCell(cell(String.valueOf(compra.getId())));
                table.addCell(cell(compra.getProveedor() != null ? safe(compra.getProveedor().getNombre()) : ""));
                table.addCell(cell(compra.getFecha() != null ? compra.getFecha().toLocalDate().toString() : ""));
                table.addCell(cell(compra.getEstado() != null ? compra.getEstado().name() : ""));
                table.addCell(cell(plain(compra.getTotal())));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de compras.", ex);
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

    private String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
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
