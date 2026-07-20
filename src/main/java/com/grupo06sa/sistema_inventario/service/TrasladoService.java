package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.MovimientoInventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TrasladoService {
    private final InventarioService inventarioService;

    public TrasladoService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    public MovimientoInventario trasladar(
        Producto producto,
        Almacen origen,
        Almacen destino,
        BigDecimal cantidad,
        Usuario usuario
    ) {
        return inventarioService.trasladar(producto, origen, destino, cantidad, usuario);
    }
}
