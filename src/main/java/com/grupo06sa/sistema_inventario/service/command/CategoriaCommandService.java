package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CategoriaCommandService {
    private final CategoriaRepository categoriaRepository;

    public CategoriaCommandService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Categoria> categorias = categoriaRepository.findAll();
        if (categorias.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Categorías", "No hay categorías registradas."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildCategoriasTable(categorias));
    }

    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Categoria categoria = obtener(parseLong(params.get(0)));
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(categoria.getId()).append("\n")
            .append("Nombre: ").append(safe(categoria.getNombre())).append("\n")
            .append("Imagen: ").append(safe(categoria.getImagen()));

        return CommandResult.text(HtmlBuilderUtil.buildPlainTemplate("Categoría", detail.toString()));
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        Categoria categoria = new Categoria();
        categoria.setNombre(params.get(0));
        categoria.setImagen(params.size() >= 2 ? params.get(1) : null);
        categoriaRepository.save(categoria);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Registro completado", "Categoría registrada correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Categoria categoria = obtener(parseLong(params.get(0)));
        categoria.setNombre(params.get(1));
        categoria.setImagen(params.size() >= 3 ? params.get(2) : categoria.getImagen());
        categoriaRepository.save(categoria);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Actualización completa", "Categoría actualizada correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Categoria categoria = obtener(parseLong(params.get(0)));
        try {
            categoriaRepository.delete(categoria);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar la categoría porque tiene productos asociados.");
        }
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Eliminación completa", "Categoría eliminada correctamente."));
    }

    private Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La categoría solicitada no existe."));
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
