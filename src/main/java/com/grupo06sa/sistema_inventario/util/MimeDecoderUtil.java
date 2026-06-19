package com.grupo06sa.sistema_inventario.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MimeDecoderUtil {
    private static final Pattern ENCODED_WORD =
        Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]+)\\?=");

    private MimeDecoderUtil() {
    }

    public static String decode(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = ENCODED_WORD.matcher(value);
        StringBuffer buffer = new StringBuffer();
        boolean decodedAny = false;

        while (matcher.find()) {
            decodedAny = true;
            String charsetName = matcher.group(1);
            String encoding = matcher.group(2);
            String encodedText = matcher.group(3);
            String decoded = matcher.group(0);

            if ("B".equalsIgnoreCase(encoding)) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(encodedText);
                    Charset charset = toCharset(charsetName);
                    decoded = new String(bytes, charset);
                } catch (IllegalArgumentException ex) {
                    decoded = matcher.group(0);
                }
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(decoded));
        }

        matcher.appendTail(buffer);
        return decodedAny ? buffer.toString() : value;
    }

    private static Charset toCharset(String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
