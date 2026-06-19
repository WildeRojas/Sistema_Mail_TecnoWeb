package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProductoCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoCommandService.class);

    private final SecurityService securityService;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoCommandService(
        SecurityService securityService,
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository,
        ProveedorRepository proveedorRepository
    ) {
        this.securityService = securityService;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public String listarProductos(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        List<Producto> productos = productoRepository.findAll();
        if (productos.isEmpty()) {
            return HtmlBuilderUtil.buildInfoTemplate("Catalogo", "No hay productos registrados.");
        }

        return HtmlBuilderUtil.buildProductosTable(productos);
    }

    public String obtenerProducto(List<String> params, String emailRemitente) {
        String accessError = validateReadAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del producto.");
        }

        Long productoId = parseLong(params.get(0));
        if (productoId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del producto no es valido.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        Producto producto = productoOpt.get();
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(producto.getId()).append("\n")
            .append("Codigo: ").append(safe(producto.getCodigo())).append("\n")
            .append("Nombre: ").append(safe(producto.getNombre())).append("\n")
            .append("Precio: ").append(safeNumber(producto.getPrecio())).append("\n")
            .append("Stock actual: ").append(safeNumber(producto.getStockActual())).append("\n")
            .append("Stock minimo: ").append(safeNumber(producto.getStockMinimo())).append("\n")
            .append("Categoria: ").append(
                producto.getCategoria() != null ? safe(producto.getCategoria().getNombre()) : ""
            ).append("\n")
            .append("Proveedor: ").append(
                producto.getProveedor() != null ? safe(producto.getProveedor().getNombre()) : ""
            );

        return HtmlBuilderUtil.buildPlainTemplate("Producto", detail.toString());
    }

    public String insertarProducto(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 6) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para registrar producto."
            );
        }

        try {
            String codigo = params.get(0);
            String nombre = params.get(1);
            Double precio = Double.parseDouble(params.get(2));
            Integer stockActual = Integer.parseInt(params.get(3));
            Integer stockMinimo = Integer.parseInt(params.get(4));
            Long categoriaId = Long.parseLong(params.get(5));
            Long proveedorId = Long.parseLong(params.get(6));
            String imagen = params.size() >= 8 ? params.get(7) : null;

            Optional<Categoria> categoria = categoriaRepository.findById(categoriaId);
            if (categoria.isEmpty()) {
                return HtmlBuilderUtil.buildErrorTemplate("Error", "La categoria indicada no existe.");
            }

            Optional<Proveedor> proveedor = proveedorRepository.findById(proveedorId);
            if (proveedor.isEmpty()) {
                return HtmlBuilderUtil.buildErrorTemplate("Error", "El proveedor indicado no existe.");
            }

            Producto producto = new Producto();
            producto.setCodigo(codigo);
            producto.setNombre(nombre);
            producto.setPrecio(precio);
            producto.setStockMinimo(stockMinimo);
            producto.setStockActual(stockActual);
            producto.setCategoria(categoria.get());
            producto.setProveedor(proveedor.get());
            if (imagen != null && !imagen.isBlank()) {
                producto.setImagen(imagen);
            }

            productoRepository.save(producto);
            return HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Producto registrado correctamente.");
        } catch (NumberFormatException ex) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Revise los valores numericos.");
        } catch (Exception ex) {
            logger.error("Failed to insert producto", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo registrar el producto.");
        }
    }

    public String actualizarProducto(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.size() < 7) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Datos incompletos",
                "Parametros insuficientes para actualizar producto."
            );
        }

        Long productoId = parseLong(params.get(0));
        if (productoId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del producto no es valido.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        try {
            String codigo = params.get(1);
            String nombre = params.get(2);
            Double precio = Double.parseDouble(params.get(3));
            Integer stockActual = Integer.parseInt(params.get(4));  
            Integer stockMinimo = Integer.parseInt(params.get(5));
            Long categoriaId = Long.parseLong(params.get(6));
            Long proveedorId = Long.parseLong(params.get(7));
            String imagen = params.size() >= 9 ? params.get(8) : null;

            Optional<Categoria> categoria = categoriaRepository.findById(categoriaId);
            if (categoria.isEmpty()) {
                return HtmlBuilderUtil.buildErrorTemplate("Error", "La categoria indicada no existe.");
            }

            Optional<Proveedor> proveedor = proveedorRepository.findById(proveedorId);
            if (proveedor.isEmpty()) {
                return HtmlBuilderUtil.buildErrorTemplate("Error", "El proveedor indicado no existe.");
            }

            Producto producto = productoOpt.get();
            producto.setCodigo(codigo);
            producto.setNombre(nombre);
            producto.setPrecio(precio);
            producto.setStockActual(stockActual);
            producto.setStockMinimo(stockMinimo);
            producto.setCategoria(categoria.get());
            producto.setProveedor(proveedor.get());
            if (imagen != null && !imagen.isBlank()) {
                producto.setImagen(imagen);
            }

            productoRepository.save(producto);
            return HtmlBuilderUtil.buildSuccessTemplate("Actualizacion completa", "Producto actualizado correctamente.");
        } catch (NumberFormatException ex) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "Revise los valores numericos.");
        } catch (Exception ex) {
            logger.error("Failed to update producto", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo actualizar el producto.");
        }
    }

    public String eliminarProducto(List<String> params, String emailRemitente) {
        String accessError = validateAdminAccess(emailRemitente);
        if (accessError != null) {
            return accessError;
        }

        if (params == null || params.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("Datos incompletos", "Debe proporcionar el id del producto.");
        }

        Long productoId = parseLong(params.get(0));
        if (productoId == null) {
            return HtmlBuilderUtil.buildErrorTemplate("Formato invalido", "El id del producto no es valido.");
        }

        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isEmpty()) {
            return HtmlBuilderUtil.buildErrorTemplate("No encontrado", "El producto solicitado no existe.");
        }

        try {
            productoRepository.delete(productoOpt.get());
            return HtmlBuilderUtil.buildSuccessTemplate("Eliminacion completa", "Producto eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Operacion no permitida",
                "No se puede eliminar el producto porque esta relacionado con otros registros."
            );
        } catch (Exception ex) {
            logger.error("Failed to delete producto", ex);
            return HtmlBuilderUtil.buildErrorTemplate("Error", "No se pudo eliminar el producto.");
        }
    }

    private String validateReadAccess(String emailRemitente) {
        try {
            Usuario usuario = securityService.authenticateAndCheckRole(emailRemitente, null);
            if (!isAdminOrCliente(usuario)) {
                return HtmlBuilderUtil.buildErrorTemplate(
                    "Privilegios insuficientes",
                    "Solo un ADMINISTRADOR o CLIENTE puede ejecutar este comando."
                );
            }
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
        }
    }

    private String validateAdminAccess(String emailRemitente) {
        try {
            securityService.authenticateAndCheckRole(emailRemitente, "ADMINISTRADOR");
            return null;
        } catch (UserNotFoundException ex) {
            return HtmlBuilderUtil.buildAccessDeniedTemplate();
        } catch (RoleAccessDeniedException ex) {
            return HtmlBuilderUtil.buildErrorTemplate(
                "Privilegios insuficientes",
                "Solo un ADMINISTRADOR puede ejecutar este comando."
            );
        }
    }

    private boolean isAdminOrCliente(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return false;
        }

        String roleName = usuario.getRol().getNombre();
        return "ADMINISTRADOR".equalsIgnoreCase(roleName) || "CLIENTE".equalsIgnoreCase(roleName);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeNumber(Number value) {
        return value == null ? "" : value.toString();
    }
}
