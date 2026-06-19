package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.service.AlmacenService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlmacenCommandService {
    private final AlmacenService almacenService;

    public AlmacenCommandService(AlmacenService almacenService) {
        this.almacenService = almacenService;
    }

    public String listarAlmacenes(String emailRemitente) {
        return almacenService.listarAlmacenes(emailRemitente);
    }

    public String obtenerAlmacen(List<String> params, String emailRemitente) {
        return almacenService.obtenerAlmacen(params, emailRemitente);
    }

    public String crearAlmacen(List<String> params, String emailRemitente) {
        return almacenService.crearAlmacen(params, emailRemitente);
    }

    public String actualizarAlmacen(List<String> params, String emailRemitente) {
        return almacenService.actualizarAlmacen(params, emailRemitente);
    }

    public String eliminarAlmacen(List<String> params, String emailRemitente) {
        return almacenService.eliminarAlmacen(params, emailRemitente);
    }
}
