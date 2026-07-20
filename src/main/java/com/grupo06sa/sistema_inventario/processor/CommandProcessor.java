package com.grupo06sa.sistema_inventario.processor;

import com.grupo06sa.sistema_inventario.processor.ComandoRegistry.ComandoDef;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import com.grupo06sa.sistema_inventario.security.UserNotFoundException;
import com.grupo06sa.sistema_inventario.util.CommandParser;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import com.grupo06sa.sistema_inventario.util.Levenshtein;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CommandProcessor.class);
    private static final String HELP = "HELP";
    private static final double UMBRAL_SIN_PARECIDO = 0.75;
    private static final int MAX_SUGERENCIAS = 3;

    private final SecurityService securityService;
    private final ComandoRegistry registry;

    public CommandProcessor(SecurityService securityService, ComandoRegistry registry) {
        this.securityService = securityService;
        this.registry = registry;
    }

    public CommandResult process(String asuntoCrudo, String emailRemitente) {
        return process(CommandParser.parse(asuntoCrudo), emailRemitente);
    }

    public CommandResult process(CommandRequest request, String emailRemitente) {
        if (request == null) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate("Error", "Solicitud inválida."),
                CommandResult.Tipo.ERROR_SINTAXIS
            );
        }

        return switch (request.getEstado()) {
            case NO_ES_COMANDO -> CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(
                    "Formato no reconocido",
                    "El asunto del correo no tiene el formato de un comando. Envíe HELP para ver los comandos disponibles."
                ),
                CommandResult.Tipo.ERROR_SINTAXIS
            );
            case ERROR_SINTAXIS -> CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate("Sintaxis inválida", request.getMensajeError()),
                CommandResult.Tipo.ERROR_SINTAXIS
            );
            case RECONOCIDO -> procesarReconocido(request, emailRemitente);
        };
    }

    private CommandResult procesarReconocido(CommandRequest request, String emailRemitente) {
        String nombreComando = request.getCommand();
        List<String> params = request.getParams();

        if (HELP.equals(nombreComando)) {
            return procesarHelp(params, emailRemitente);
        }

        ComandoDef definicion = registry.buscar(nombreComando);
        if (definicion == null) {
            return responderComandoDesconocido(nombreComando, emailRemitente);
        }

        ContextoAutenticado ctx;
        try {
            ctx = securityService.autenticar(emailRemitente);
        } catch (UserNotFoundException ex) {
            return CommandResult.text(HtmlBuilderUtil.buildRegistroRequeridoTemplate(), CommandResult.Tipo.NO_REGISTRADO);
        }

        if (!definicion.roles().contains(ctx.getRol())) {
            logger.warn(
                "Permiso denegado: el rol {} intentó ejecutar el comando {} (remitente={})",
                ctx.getRol(), nombreComando, emailRemitente
            );
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(
                    "Privilegios insuficientes",
                    "No tiene autorización para ejecutar este comando con su rol actual."
                ),
                CommandResult.Tipo.PERMISO_DENEGADO
            );
        }

        int cantidadParametros = contarParametrosReales(params);
        if (cantidadParametros < definicion.aridadMin() || cantidadParametros > definicion.aridadMax()) {
            return responderAridadInvalida(definicion, cantidadParametros);
        }

        try {
            return definicion.handler().apply(ctx, params);
        } catch (RuntimeException ex) {
            logger.error("Fallo al ejecutar el comando {}", nombreComando, ex);
            DbErrorTranslator.Traduccion traduccion = DbErrorTranslator.traducir(ex);
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(traduccion.titulo(), traduccion.mensaje()),
                traduccion.tipo()
            );
        }
    }

    private CommandResult procesarHelp(List<String> params, String emailRemitente) {
        if (contarParametrosReales(params) > 0) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(
                    "Parámetro no permitido",
                    "El comando HELP no admite parámetros. Uso correcto: HELP[\"\"]."
                ),
                CommandResult.Tipo.PARAMETRO_NO_PERMITIDO
            );
        }

        ContextoAutenticado ctx;
        try {
            ctx = securityService.autenticar(emailRemitente);
        } catch (UserNotFoundException ex) {
            return CommandResult.text(HtmlBuilderUtil.buildRegistroRequeridoTemplate(), CommandResult.Tipo.NO_REGISTRADO);
        }

        Set<String> comandos = registry.comandosParaRol(ctx.getRol());
        return CommandResult.text(HtmlBuilderUtil.buildHelpTemplate(ctx.getRol(), comandos), CommandResult.Tipo.OK);
    }

    private CommandResult responderComandoDesconocido(String nombreComando, String emailRemitente) {
        ContextoAutenticado ctx;
        try {
            ctx = securityService.autenticar(emailRemitente);
        } catch (UserNotFoundException ex) {
            return CommandResult.text(HtmlBuilderUtil.buildRegistroRequeridoTemplate(), CommandResult.Tipo.NO_REGISTRADO);
        }

        logger.warn("Comando desconocido recibido: {} (remitente={}, rol={})", nombreComando, emailRemitente, ctx.getRol());

        List<ComandoDef> candidatos = registry.definicionesParaRol(ctx.getRol());
        List<ComandoDef> sugerencias = sugerirComandos(nombreComando, candidatos);

        List<String> usos = new ArrayList<>();
        for (ComandoDef def : sugerencias) {
            usos.add(def.uso());
        }

        return CommandResult.text(
            HtmlBuilderUtil.buildComandoDesconocidoTemplate(nombreComando, usos),
            CommandResult.Tipo.COMANDO_DESCONOCIDO
        );
    }

    private List<ComandoDef> sugerirComandos(String nombreIngresado, List<ComandoDef> candidatos) {
        if (candidatos.isEmpty()) {
            return List.of();
        }

        List<ComandoDef> ordenados = new ArrayList<>(candidatos);
        ordenados.sort(Comparator.comparingDouble(def -> puntuacion(nombreIngresado, def.nombre())));

        List<ComandoDef> sugerencias = new ArrayList<>();
        for (ComandoDef def : ordenados) {
            if (puntuacion(nombreIngresado, def.nombre()) > UMBRAL_SIN_PARECIDO) {
                break;
            }
            sugerencias.add(def);
            if (sugerencias.size() >= MAX_SUGERENCIAS) {
                break;
            }
        }
        return sugerencias;
    }

    private double puntuacion(String a, String b) {
        int distancia = Levenshtein.distancia(a, b);
        int prefijo = Levenshtein.prefijoComun(a, b);
        int largoMax = Math.max(a.length(), b.length());
        double distanciaRelativa = largoMax == 0 ? 0 : (double) distancia / largoMax;
        return Math.max(0, distanciaRelativa - (prefijo * 0.05));
    }

    private CommandResult responderAridadInvalida(ComandoDef definicion, int cantidadParametros) {
        if (definicion.aridadMax() == 0 && cantidadParametros > 0) {
            return CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(
                    "Parámetro no permitido",
                    "El comando " + definicion.nombre() + " no admite parámetros. Uso correcto: " + definicion.uso() + "."
                ),
                CommandResult.Tipo.PARAMETRO_NO_PERMITIDO
            );
        }

        return CommandResult.text(
            HtmlBuilderUtil.buildErrorTemplate(
                "Parámetros incorrectos",
                "El comando " + definicion.nombre() + " espera " + describirAridad(definicion)
                    + ". Uso correcto: " + definicion.uso() + "."
            ),
            CommandResult.Tipo.ERROR_VALIDACION
        );
    }

    private int contarParametrosReales(List<String> params) {
        if (params == null) {
            return 0;
        }
        if (params.size() == 1 && (params.get(0) == null || params.get(0).isBlank())) {
            return 0;
        }
        return params.size();
    }

    private String describirAridad(ComandoDef definicion) {
        if (definicion.aridadMin() == definicion.aridadMax()) {
            return "exactamente " + definicion.aridadMin() + " parámetro(s)";
        }
        return "entre " + definicion.aridadMin() + " y " + definicion.aridadMax() + " parámetro(s)";
    }
}
