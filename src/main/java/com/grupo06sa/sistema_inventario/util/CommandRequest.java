package com.grupo06sa.sistema_inventario.util;

import java.util.List;

public final class CommandRequest {

    public enum Estado {

        RECONOCIDO,

        ERROR_SINTAXIS,

        NO_ES_COMANDO
    }

    private final String command;
    private final List<String> params;
    private final Estado estado;
    private final String mensajeError;

    private CommandRequest(String command, List<String> params, Estado estado, String mensajeError) {
        this.command = command;
        this.params = params == null ? List.of() : List.copyOf(params);
        this.estado = estado;
        this.mensajeError = mensajeError;
    }

    public static CommandRequest reconocido(String command, List<String> params) {
        return new CommandRequest(command, params, Estado.RECONOCIDO, null);
    }

    public static CommandRequest errorSintaxis(String mensaje) {
        return new CommandRequest(null, List.of(), Estado.ERROR_SINTAXIS, mensaje);
    }

    public static CommandRequest noEsComando() {
        return new CommandRequest(null, List.of(), Estado.NO_ES_COMANDO, null);
    }

    public String getCommand() {
        return command;
    }

    public List<String> getParams() {
        return params;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }
}
