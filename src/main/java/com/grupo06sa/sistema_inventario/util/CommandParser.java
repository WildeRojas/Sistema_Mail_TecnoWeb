package com.grupo06sa.sistema_inventario.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser {
    private static final int MAX_LENGTH = 2000;
    private static final Pattern NAME_PATTERN = Pattern.compile("^([A-Za-z_]+)");
    private static final Pattern PREFIJO_RESPUESTA =
        Pattern.compile("^\\s*(RE|RV|FW|FWD)\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    private CommandParser() {
    }

    public static boolean esComando(String asunto) {
        return interpretar(asunto).getEstado() != CommandRequest.Estado.NO_ES_COMANDO;
    }

    public static CommandRequest parse(String asunto) {
        return interpretar(asunto);
    }

    private static CommandRequest interpretar(String asuntoCrudo) {
        if (asuntoCrudo == null || asuntoCrudo.length() > MAX_LENGTH) {
            return CommandRequest.noEsComando();
        }

        String normalizado = normalizar(asuntoCrudo);
        if (normalizado.isEmpty()) {
            return CommandRequest.noEsComando();
        }

        Matcher nameMatcher = NAME_PATTERN.matcher(normalizado);
        if (!nameMatcher.lookingAt()) {
            return CommandRequest.noEsComando();
        }

        if (normalizado.length() > MAX_LENGTH) {
            return CommandRequest.errorSintaxis(
                "El asunto del comando supera la longitud máxima permitida de " + MAX_LENGTH + " caracteres.");
        }

        String nombre = nameMatcher.group(1).toUpperCase(Locale.ROOT);
        int n = normalizado.length();
        int p = skipWhitespace(normalizado, nameMatcher.end(), n);

        if (p == n) {
            return CommandRequest.reconocido(nombre, List.of());
        }

        char siguiente = normalizado.charAt(p);
        if (siguiente == '[') {
            return parseCorchetes(nombre, normalizado, p);
        }
        if (siguiente == '(') {
            return parseParentesis(nombre, normalizado, p);
        }
        return CommandRequest.noEsComando();
    }

    private static CommandRequest parseCorchetes(String nombre, String s, int openIdx) {
        int n = s.length();
        int i = skipWhitespace(s, openIdx + 1, n);
        if (i < n && s.charAt(i) == ']') {
            return finalizarConSobrantes(nombre, List.of(), s, i + 1);
        }

        List<String> params = new ArrayList<>();
        while (true) {
            if (i >= n) {
                return CommandRequest.errorSintaxis(
                    "Falta el corchete de cierre \"]\" en el comando " + nombre + ".");
            }

            char c = s.charAt(i);
            if (c != '"') {
                return CommandRequest.errorSintaxis(
                    "Cada parámetro de " + nombre + " debe ir entre comillas dobles, por ejemplo: \"valor\".");
            }

            i++;
            StringBuilder valor = new StringBuilder();
            boolean cerrada = false;
            while (i < n) {
                char cur = s.charAt(i);
                if (cur == '\\' && i + 1 < n && (s.charAt(i + 1) == '"' || s.charAt(i + 1) == '\\')) {
                    valor.append(s.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (cur == '"') {
                    cerrada = true;
                    i++;
                    break;
                }
                valor.append(cur);
                i++;
            }
            if (!cerrada) {
                return CommandRequest.errorSintaxis(
                    "Hay comillas sin cerrar en los parámetros de " + nombre + ".");
            }
            params.add(valor.toString());

            i = skipWhitespace(s, i, n);
            if (i >= n) {
                return CommandRequest.errorSintaxis(
                    "Falta el corchete de cierre \"]\" en el comando " + nombre + ".");
            }

            char separador = s.charAt(i);
            if (separador == ']') {
                return finalizarConSobrantes(nombre, params, s, i + 1);
            }
            if (separador != ',') {
                return CommandRequest.errorSintaxis(
                    "Separador incorrecto en los parámetros de " + nombre + "; use una coma (,) entre valores.");
            }
            i = skipWhitespace(s, i + 1, n);
        }
    }

    private static CommandRequest parseParentesis(String nombre, String s, int openIdx) {
        int n = s.length();
        int i = skipWhitespace(s, openIdx + 1, n);
        if (i >= n) {
            return CommandRequest.errorSintaxis(
                "Falta el paréntesis de cierre \")\" en el comando " + nombre + ".");
        }
        if (s.charAt(i) != ')') {
            return CommandRequest.errorSintaxis(
                "El comando " + nombre + " no admite parámetros entre paréntesis; use corchetes, por ejemplo: "
                    + nombre + "[\"valor\"].");
        }
        return finalizarConSobrantes(nombre, List.of(), s, i + 1);
    }

    private static CommandRequest finalizarConSobrantes(String nombre, List<String> params, String s, int after) {
        String sobrante = s.substring(after).strip();
        if (!sobrante.isEmpty()) {
            return CommandRequest.errorSintaxis(
                "Hay caracteres sobrantes después del cierre del comando " + nombre + ": \"" + sobrante + "\".");
        }
        return CommandRequest.reconocido(nombre, params);
    }

    private static int skipWhitespace(String s, int from, int n) {
        int p = from;
        while (p < n && Character.isWhitespace(s.charAt(p))) {
            p++;
        }
        return p;
    }

    private static String normalizar(String asuntoCrudo) {
        String decodificado = MimeDecoderUtil.decode(asuntoCrudo);
        String nfc = Normalizer.normalize(decodificado, Normalizer.Form.NFC);
        String sinPrefijos = retirarPrefijos(nfc);
        return colapsarEspacios(sinPrefijos).trim();
    }

    private static String retirarPrefijos(String s) {
        String actual = s;
        Matcher matcher = PREFIJO_RESPUESTA.matcher(actual);
        while (matcher.find() && matcher.start() == 0) {
            actual = actual.substring(matcher.end());
            matcher = PREFIJO_RESPUESTA.matcher(actual);
        }
        return actual;
    }

    private static String colapsarEspacios(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean dentroComillas = false;
        boolean espacioPendiente = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (dentroComillas) {
                out.append(c);
                if (c == '\\' && i + 1 < s.length()) {
                    out.append(s.charAt(i + 1));
                    i++;
                    continue;
                }
                if (c == '"') {
                    dentroComillas = false;
                }
                continue;
            }

            if (c == '"') {
                if (espacioPendiente) {
                    out.append(' ');
                    espacioPendiente = false;
                }
                out.append(c);
                dentroComillas = true;
                continue;
            }

            if (Character.isWhitespace(c)) {
                espacioPendiente = out.length() > 0;
                continue;
            }

            if (espacioPendiente) {
                out.append(' ');
                espacioPendiente = false;
            }
            out.append(c);
        }

        return out.toString();
    }
}
