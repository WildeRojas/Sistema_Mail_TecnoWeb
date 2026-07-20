package com.grupo06sa.sistema_inventario.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MimeDecoderUtil {
    private static final Pattern ENCODED_WORD =
        Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]*)\\?=");

    private MimeDecoderUtil() {
    }

    public static String decode(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = ENCODED_WORD.matcher(value);
        StringBuilder resultado = new StringBuilder();
        int posicionAnterior = 0;
        boolean anteriorFueCodificada = false;

        while (matcher.find()) {
            String entre = value.substring(posicionAnterior, matcher.start());
            if (!(anteriorFueCodificada && entre.isBlank())) {
                resultado.append(entre);
            }

            resultado.append(decodeWord(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(0)));

            posicionAnterior = matcher.end();
            anteriorFueCodificada = true;
        }

        resultado.append(value.substring(posicionAnterior));
        return resultado.toString();
    }

    private static String decodeWord(String charsetName, String encoding, String texto, String original) {
        Charset charset = toCharset(charsetName);
        try {
            if ("B".equalsIgnoreCase(encoding)) {
                byte[] bytes = Base64.getDecoder().decode(texto);
                return new String(bytes, charset);
            }
            if ("Q".equalsIgnoreCase(encoding)) {
                return decodeQuotedPrintable(texto, charset);
            }
        } catch (IllegalArgumentException ex) {
            return original;
        }
        return original;
    }

    private static String decodeQuotedPrintable(String texto, Charset charset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(texto.length());
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c == '_') {
                bytes.write(' ');
                i++;
                continue;
            }
            if (c == '=' && i + 2 < texto.length()) {
                String hex = texto.substring(i + 1, i + 3);
                try {
                    bytes.write(Integer.parseInt(hex, 16));
                    i += 3;
                    continue;
                } catch (NumberFormatException ex) {
                    bytes.write(c);
                    i++;
                    continue;
                }
            }
            bytes.write(c);
            i++;
        }
        return new String(bytes.toByteArray(), charset);
    }

    private static Charset toCharset(String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
