package com.grupo06sa.sistema_inventario.service.command;

import com.grupo06sa.sistema_inventario.service.TrasladoService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrasladoCommandService {
    private final TrasladoService trasladoService;

    public TrasladoCommandService(TrasladoService trasladoService) {
        this.trasladoService = trasladoService;
    }

    public String trasladarStock(List<String> params, String emailRemitente) {
        return trasladoService.trasladarStock(params, emailRemitente);
    }
}
