package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProveedorCommandService {
    private final ProveedorRepository proveedorRepository;

    public ProveedorCommandService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public CommandResult listar(ContextoAutenticado ctx, List<String> params) {
        List<Proveedor> proveedores = proveedorRepository.findAll();
        if (proveedores.isEmpty()) {
            return CommandResult.text(HtmlBuilderUtil.buildInfoTemplate("Proveedores", "No hay proveedores registrados."));
        }
        return CommandResult.text(HtmlBuilderUtil.buildProveedoresTable(proveedores));
    }

    public CommandResult obtener(ContextoAutenticado ctx, List<String> params) {
        Proveedor p = obtener(parseLong(params.get(0)));
        StringBuilder detail = new StringBuilder();
        detail.append("ID: ").append(p.getId()).append("\n")
            .append("Nombre: ").append(safe(p.getNombre())).append("\n")
            .append("Telefono: ").append(safe(p.getTelefono())).append("\n")
            .append("Correo: ").append(safe(p.getCorreo())).append("\n")
            .append("Direccion: ").append(safe(p.getDireccion())).append("\n")
            .append("NIT: ").append(safe(p.getNit()));

        return CommandResult.text(HtmlBuilderUtil.buildPlainTemplate("Proveedor", detail.toString()));
    }

    public CommandResult insertar(ContextoAutenticado ctx, List<String> params) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(params.get(0));
        proveedor.setTelefono(params.get(1));
        proveedor.setCorreo(params.get(2));
        proveedor.setDireccion(params.get(3));
        proveedor.setNit(params.size() >= 5 ? params.get(4) : null);

        proveedorRepository.save(proveedor);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Proveedor registrado", "Proveedor registrado correctamente."));
    }

    public CommandResult actualizar(ContextoAutenticado ctx, List<String> params) {
        Proveedor proveedor = obtener(parseLong(params.get(0)));
        proveedor.setNombre(params.get(1));
        proveedor.setTelefono(params.get(2));
        proveedor.setCorreo(params.get(3));
        proveedor.setDireccion(params.get(4));
        proveedor.setNit(params.size() >= 6 ? params.get(5) : proveedor.getNit());

        proveedorRepository.save(proveedor);
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Proveedor actualizado", "Proveedor actualizado correctamente."));
    }

    public CommandResult eliminar(ContextoAutenticado ctx, List<String> params) {
        Proveedor proveedor = obtener(parseLong(params.get(0)));
        try {
            proveedorRepository.delete(proveedor);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar el proveedor porque tiene ofertas o compras asociadas.");
        }
        return CommandResult.text(HtmlBuilderUtil.buildSuccessTemplate("Proveedor eliminado", "Proveedor eliminado correctamente."));
    }

    private Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El proveedor solicitado no existe."));
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
