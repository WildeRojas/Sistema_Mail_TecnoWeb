package com.grupo06sa.sistema_inventario.util;

public class CommandResult {

    public enum Tipo {
        OK,
        ERROR_SINTAXIS,
        COMANDO_DESCONOCIDO,
        PARAMETRO_NO_PERMITIDO,
        NO_REGISTRADO,
        PERMISO_DENEGADO,
        ERROR_VALIDACION,
        ERROR_BD,
        ERROR_INTERNO
    }

    private final String body;
    private final EmailAttachment attachment;
    private final Tipo tipo;

    private CommandResult(String body, EmailAttachment attachment, Tipo tipo) {
        this.body = body;
        this.attachment = attachment;
        this.tipo = tipo;
    }

    public static CommandResult text(String body) {
        return new CommandResult(body, null, Tipo.OK);
    }

    public static CommandResult text(String body, Tipo tipo) {
        return new CommandResult(body, null, tipo);
    }

    public static CommandResult withAttachment(String body, EmailAttachment attachment) {
        return new CommandResult(body, attachment, Tipo.OK);
    }

    public String getBody() {
        return body;
    }

    public EmailAttachment getAttachment() {
        return attachment;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
