package com.grupo06sa.sistema_inventario.util;

import java.util.List;

public class CommandRequest {
    private final String command;
    private final List<String> params;

    public CommandRequest(String command, List<String> params) {
        this.command = command;
        this.params = List.copyOf(params);
    }

    public String getCommand() {
        return command;
    }

    public List<String> getParams() {
        return params;
    }
}
