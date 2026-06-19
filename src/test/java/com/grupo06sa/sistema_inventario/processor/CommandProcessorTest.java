package com.grupo06sa.sistema_inventario.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.service.command.AlmacenCommandService;
import com.grupo06sa.sistema_inventario.service.command.CategoriaCommandService;
import com.grupo06sa.sistema_inventario.service.command.CompraCommandService;
import com.grupo06sa.sistema_inventario.service.command.PedidosCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProductoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProveedorCommandService;
import com.grupo06sa.sistema_inventario.service.command.ReporteCommandService;
import com.grupo06sa.sistema_inventario.service.command.TrasladoCommandService;
import com.grupo06sa.sistema_inventario.service.command.UsuarioCommandService;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CommandProcessorTest {
    @Mock
    private ProductoCommandService productoCommandService;
    @Mock
    private CategoriaCommandService categoriaCommandService;
    @Mock
    private UsuarioCommandService usuarioCommandService;
    @Mock
    private PedidosCommandService pedidosCommandService;
    @Mock
    private CompraCommandService compraCommandService;
    @Mock
    private ReporteCommandService reporteCommandService;
    @Mock
    private AlmacenCommandService almacenCommandService;
    @Mock
    private TrasladoCommandService trasladoCommandService;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ProveedorCommandService proveedorCommandService;

    private CommandProcessor commandProcessor;

    @BeforeEach
    void setUp() {
        commandProcessor = new CommandProcessor(
            productoCommandService,
            categoriaCommandService,
            usuarioCommandService,
            pedidosCommandService,
            compraCommandService,
            reporteCommandService,
            almacenCommandService,
            trasladoCommandService,
            proveedorRepository,
            proveedorCommandService
        );
    }

    @Test
    void devuelveAvisoCuandoBaseDeDatosNoEstaDisponible() {
        when(productoCommandService.listarProductos(List.of("*"), "cliente@test.com"))
            .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        CommandResult result = commandProcessor.process(
            new CommandRequest("LIS_PRODUCTO", List.of("*")),
            "cliente@test.com"
        );

        assertThat(result.getBody())
            .contains("Base de datos no disponible")
            .contains("No se pudo conectar a la base de datos")
            .doesNotContain("Connection refused");
    }

    @Test
    void devuelveAvisoCuandoBaseDeDatosRechazaLosDatos() {
        when(productoCommandService.listarProductos(List.of("*"), "cliente@test.com"))
            .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        CommandResult result = commandProcessor.process(
            new CommandRequest("LIS_PRODUCTO", List.of("*")),
            "cliente@test.com"
        );

        assertThat(result.getBody())
            .contains("Datos no validos")
            .contains("La base de datos rechazo la operacion")
            .doesNotContain("duplicate key");
    }
}
