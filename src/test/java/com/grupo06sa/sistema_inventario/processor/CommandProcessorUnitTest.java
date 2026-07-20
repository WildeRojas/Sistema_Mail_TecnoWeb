package com.grupo06sa.sistema_inventario.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.service.command.AlertaCommandService;
import com.grupo06sa.sistema_inventario.service.command.AlmacenCommandService;
import com.grupo06sa.sistema_inventario.service.command.CategoriaCommandService;
import com.grupo06sa.sistema_inventario.service.command.CompraCommandService;
import com.grupo06sa.sistema_inventario.service.command.InventarioCommandService;
import com.grupo06sa.sistema_inventario.service.command.OfertaCommandService;
import com.grupo06sa.sistema_inventario.service.command.PagoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProductoCommandService;
import com.grupo06sa.sistema_inventario.service.command.ProveedorCommandService;
import com.grupo06sa.sistema_inventario.service.command.ReporteCommandService;
import com.grupo06sa.sistema_inventario.service.command.SolicitudCommandService;
import com.grupo06sa.sistema_inventario.service.command.UsuarioCommandService;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CommandProcessorUnitTest {

    private SecurityService securityService;
    private ComandoRegistry registry;
    private CommandProcessor processor;

    private UsuarioCommandService usuarioCommandService;
    private CompraCommandService compraCommandService;

    @BeforeEach
    void construir() {
        securityService = mock(SecurityService.class);
        usuarioCommandService = mock(UsuarioCommandService.class);
        compraCommandService = mock(CompraCommandService.class);

        registry = new ComandoRegistry(
            usuarioCommandService,
            mock(ProveedorCommandService.class),
            mock(CategoriaCommandService.class),
            mock(ProductoCommandService.class),
            mock(AlmacenCommandService.class),
            mock(InventarioCommandService.class),
            mock(AlertaCommandService.class),
            mock(OfertaCommandService.class),
            mock(SolicitudCommandService.class),
            compraCommandService,
            mock(PagoCommandService.class),
            mock(ReporteCommandService.class)
        );

        processor = new CommandProcessor(securityService, registry);
    }

    private ContextoAutenticado contextoPara(RolNombre rol) {
        Usuario usuario = new Usuario();
        usuario.setEmail("usuario@test.local");
        return new ContextoAutenticado(usuario, rol, rol == RolNombre.PROVEEDOR ? 1L : null);
    }

    @Test
    void helpDevuelveSoloLosComandosAutorizadosParaElRolDelRemitente() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));

        CommandResult resultado = processor.process("HELP", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains("PAGAR_COMPRA");
        assertThat(resultado.getBody()).doesNotContain("INS_OFERTA");
    }

    @Test
    void helpConParametroNoVacioEsParametroNoPermitido() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));

        CommandResult resultado = processor.process("HELP[\"algo\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PARAMETRO_NO_PERMITIDO);
        assertThat(resultado.getBody()).contains("no admite parámetros");
    }

    @Test
    void helpParaRemitenteNoRegistradoNoConsultaComandosSinoQuePideRegistro() {
        when(securityService.autenticar("desconocido@test.local"))
            .thenThrow(new UserNotFoundException("no registrado"));

        CommandResult resultado = processor.process("HELP", "desconocido@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.NO_REGISTRADO);
        assertThat(resultado.getBody()).contains("No estás registrado en el sistema");
    }

    @Test
    void comandoDesconocidoSugiereElComandoRealMasParecido() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));

        CommandResult resultado = processor.process("GET_USUARIOO[\"1\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.COMANDO_DESCONOCIDO);
        assertThat(resultado.getBody()).contains("No se reconoció el comando");
        assertThat(resultado.getBody()).contains("GET_USUARIO[");
    }

    @Test
    void comandoDesconocidoParaRemitenteNoRegistradoNoSugiereNiRevelaComandosInternos() {
        when(securityService.autenticar("desconocido@test.local"))
            .thenThrow(new UserNotFoundException("no registrado"));

        CommandResult resultado = processor.process("ALGO_RARO", "desconocido@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.NO_REGISTRADO);
    }

    @Test
    void trabajadorNoAutorizadoParaInsComprasRecibePermisoDenegado() {
        when(securityService.autenticar("trabajador@test.local")).thenReturn(contextoPara(RolNombre.TRABAJADOR));

        CommandResult resultado = processor.process("INS_COMPRA[\"1\",\"1\"]", "trabajador@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        assertThat(resultado.getBody()).contains("Privilegios insuficientes")
            .contains("No tiene autorización para ejecutar este comando");
    }

    @Test
    void aridadFaltanteDevuelveErrorDeValidacion() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));

        CommandResult resultado = processor.process("GET_USUARIO[]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("espera exactamente 1 parámetro");
    }

    @Test
    void parametroSobranteEnComandoSinParametrosEsParametroNoPermitido() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));

        CommandResult resultado = processor.process("LIS_USUARIO[\"algo\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.PARAMETRO_NO_PERMITIDO);
    }

    @Test
    void unicoParametroEnBlancoEnComandoSinParametrosSeAceptaComoSinParametros() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));
        when(usuarioCommandService.listar(any(), anyList())).thenReturn(CommandResult.text("listado ok"));

        CommandResult resultado = processor.process("LIS_USUARIO[\"\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).isEqualTo("listado ok");
    }

    @Test
    void comandoValidoAutorizadoEjecutaElHandlerYDevuelveSuResultado() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));
        when(usuarioCommandService.obtener(any(), anyList())).thenReturn(CommandResult.text("Usuario 7"));

        CommandResult resultado = processor.process("GET_USUARIO[\"7\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).isEqualTo("Usuario 7");
    }

    @Test
    void errorDeNegocioDelHandlerSeTraduceComoErrorDeValidacionSinStackTrace() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));
        when(compraCommandService.crear(any(), anyList()))
            .thenThrow(new IllegalStateException("El proveedor no tiene ofertas disponibles."));

        CommandResult resultado = processor.process("INS_COMPRA[\"1\",\"1\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(resultado.getBody()).contains("El proveedor no tiene ofertas disponibles.");
        assertThat(resultado.getBody()).doesNotContain("IllegalStateException");
        assertThat(resultado.getBody()).doesNotContain("at com.grupo06sa");
    }

    @Test
    void errorDeBaseDeDatosDelHandlerSeTraduceComoErrorBdSinExponerDetalleTecnico() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));
        when(compraCommandService.crear(any(), anyList()))
            .thenThrow(new DataAccessResourceFailureException("conexion perdida con detalle interno sensible"));

        CommandResult resultado = processor.process("INS_COMPRA[\"1\",\"1\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(resultado.getBody()).contains("Base de datos no disponible");
        assertThat(resultado.getBody()).doesNotContain("conexion perdida con detalle interno sensible");
    }

    @Test
    void asuntoSinFormaDeComandoEsErrorDeSintaxisGenerico() {
        CommandResult resultado = processor.process(CommandRequest.noEsComando(), "cualquiera@test.local");
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_SINTAXIS);
        assertThat(resultado.getBody()).contains("Formato no reconocido");
    }

    @Test
    void errorDeSintaxisDelParserSePropagaComoErrorDeSintaxis() {
        CommandResult resultado = processor.process(
            CommandRequest.errorSintaxis("Hay comillas sin cerrar en los parámetros de GET_USUARIO."),
            "cualquiera@test.local"
        );
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_SINTAXIS);
        assertThat(resultado.getBody()).contains("comillas sin cerrar");
    }

    @Test
    void solicitudNulaEsErrorDeSintaxis() {
        CommandResult resultado = processor.process((CommandRequest) null, "cualquiera@test.local");
        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.ERROR_SINTAXIS);
    }

    @Test
    void metodoDeConvenienciaNormalizaYParseaElAsuntoCrudo() {
        when(securityService.autenticar("propietario@test.local")).thenReturn(contextoPara(RolNombre.PROPIETARIO));
        when(usuarioCommandService.obtener(any(), anyList())).thenReturn(CommandResult.text("Usuario 9"));

        CommandResult resultado = processor.process("RE: get_usuario[\"9\"]", "propietario@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).isEqualTo("Usuario 9");
    }

    @Test
    void proveedorNoPuedeVerAyudaConComandosDeOtroRol() {
        when(securityService.autenticar("proveedor@test.local")).thenReturn(contextoPara(RolNombre.PROVEEDOR));

        CommandResult resultado = processor.process("HELP", "proveedor@test.local");

        assertThat(resultado.getTipo()).isEqualTo(CommandResult.Tipo.OK);
        assertThat(resultado.getBody()).contains("LIS_OFERTA");
        assertThat(resultado.getBody()).doesNotContain("INS_USUARIO");
        assertThat(resultado.getBody()).doesNotContain("PAGAR_COMPRA[");
    }
}
