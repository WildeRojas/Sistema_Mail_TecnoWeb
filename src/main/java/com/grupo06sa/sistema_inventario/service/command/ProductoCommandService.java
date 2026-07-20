package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProductoCommandService {
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorProductoRepository ofertaRepository;

    public ProductoCommandService(
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository,
        ProveedorProductoRepository ofertaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.ofertaRepository = ofertaRepository;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Producto> productos;
        if (ctx.getRol() == RolNombre.PROVEEDOR) {
            productos = ofertaRepository.findByProveedorId(ctx.getProveedorId()).stream()
                .map(ProveedorProducto::getProducto)
                .toList();
        } else {
            productos = productoRepository.findAll();
        }

        if (productos.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Catálogo", "No hay productos registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildProductosTable(productos));
    }

    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El producto solicitado no existe."));

        if (ctx.getRol() == RolNombre.PROVEEDOR
            && !ofertaRepository.existsByProveedorIdAndProductoId(ctx.getProveedorId(), id)) {
            throw new RoleAccessDeniedException("No tiene una oferta registrada para este producto.");
        }

        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(producto.getId()).append("\n")
            .append("Codigo: ").append(safe(producto.getCodigo())).append("\n")
            .append("Nombre: ").append(safe(producto.getNombre())).append("\n")
            .append("Costo unitario: ").append(plain(producto.getCostoUnitario())).append("\n")
            .append("Stock minimo: ").append(plain(producto.getStockMinimo())).append("\n")
            .append("Categoria: ").append(
                producto.getCategoria() != null ? safe(producto.getCategoria().getNombre()) : ""
            );

        return CommandResult.text(HtmlBuilderUtil.buildPlainTemplate("Producto", detail.toString()));
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        String codigo = params.get(0);
        String nombre = params.get(1);
        BigDecimal costoUnitario = parseBigDecimal(params.get(2));
        BigDecimal stockMinimo = parseBigDecimal(params.get(3));
        Long categoriaId = parseLong(params.get(4));
        String imagen = params.size() >= 6 ? params.get(5) : null;

        if (productoRepository.findByCodigo(codigo).isPresent()) {
            throw new IllegalStateException("Ya existe un producto con ese código.");
        }

        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new IllegalArgumentException("La categoría indicada no existe."));

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCostoUnitario(costoUnitario);
        producto.setStockMinimo(stockMinimo);
        producto.setCategoria(categoria);
        producto.setImagen(imagen);

        productoRepository.save(producto);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Producto registrado correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El producto solicitado no existe."));

        String codigo = params.get(1);
        String nombre = params.get(2);
        BigDecimal costoUnitario = parseBigDecimal(params.get(3));
        BigDecimal stockMinimo = parseBigDecimal(params.get(4));
        Long categoriaId = parseLong(params.get(5));
        String imagen = params.size() >= 7 ? params.get(6) : producto.getImagen();

        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new IllegalArgumentException("La categoría indicada no existe."));

        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCostoUnitario(costoUnitario);
        producto.setStockMinimo(stockMinimo);
        producto.setCategoria(categoria);
        producto.setImagen(imagen);

        productoRepository.save(producto);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Actualización completa", "Producto actualizado correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Long id = parseLong(params.get(0));
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El producto solicitado no existe."));

        try {
            productoRepository.delete(producto);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar el producto porque está relacionado con otros registros.");
        }
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Eliminación completa", "Producto eliminado correctamente."));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar un id numérico válido.");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El id proporcionado no es válido.");
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar un valor numérico válido.");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor numérico proporcionado no es válido.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
