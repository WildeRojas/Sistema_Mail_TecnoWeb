package com.grupo06sa.sistema_inventario.util;

public class CommandResult {
    private final String body;
    private final EmailAttachment attachment;

    private CommandResult(String body, EmailAttachment attachment) {
        this.body = body;
        this.attachment = attachment;
    }

    public static CommandResult text(String body) {
        return new CommandResult(body, null);
    }

    public static CommandResult withAttachment(String body, EmailAttachment attachment) {
        return new CommandResult(body, attachment);
    }

    public String getBody() {
        return body;
    }

    public EmailAttachment getAttachment() {
        return attachment;
    }
}
