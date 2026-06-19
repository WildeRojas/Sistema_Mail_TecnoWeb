package com.grupo06sa.sistema_inventario.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^([A-Z_]+)\\[(.*)]$");
    private static final Pattern PARAM_PATTERN = Pattern.compile("\"(.*?)\"");

    private CommandParser() {
    }

    public static CommandRequest parse(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is empty");
        }

        Matcher matcher = COMMAND_PATTERN.matcher(subject.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid command format");
        }

        String command = matcher.group(1);
        String paramsPart = matcher.group(2).trim();
        List<String> params = new ArrayList<>();

        if (!paramsPart.isEmpty()) {
            Matcher paramMatcher = PARAM_PATTERN.matcher(paramsPart);
            while (paramMatcher.find()) {
                params.add(paramMatcher.group(1));
            }

            String leftovers = PARAM_PATTERN.matcher(paramsPart).replaceAll("");
            String cleaned = leftovers.replaceAll("[,\\s]", "");
            if (!cleaned.isEmpty()) {
                throw new IllegalArgumentException("Invalid parameters format");
            }
        }

        return new CommandRequest(command, params);
    }
}
