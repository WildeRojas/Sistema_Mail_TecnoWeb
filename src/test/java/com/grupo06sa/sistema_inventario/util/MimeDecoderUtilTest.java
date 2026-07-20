package com.grupo06sa.sistema_inventario.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MimeDecoderUtilTest {

    @Test
    void decodificaEncodedWordBase64() {
        String encoded = "=?UTF-8?B?" + base64("Hola Mundo") + "?=";
        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("Hola Mundo");
    }

    @Test
    void decodificaEncodedWordBase64MinusculaB() {
        String encoded = "=?UTF-8?b?" + base64("HELP") + "?=";
        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("HELP");
    }

    @Test
    void decodificaEncodedWordQuotedPrintableConGuionBajoComoEspacio() {
        assertThat(MimeDecoderUtil.decode("=?UTF-8?Q?Hola_Mundo?=")).isEqualTo("Hola Mundo");
    }

    @Test
    void decodificaEncodedWordQuotedPrintableConSecuenciaHexadecimal() {

        assertThat(MimeDecoderUtil.decode("=?ISO-8859-1?Q?Bs=E1sico?=")).isEqualTo("Bsásico");
    }

    @Test
    void decodificaEncodedWordQuotedPrintableMinusculaQ() {
        assertThat(MimeDecoderUtil.decode("=?UTF-8?q?Hola_Mundo?=")).isEqualTo("Hola Mundo");
    }

    @Test
    void concatenaEncodedWordsAdyacentesSinElEspacioDePliegue() {
        String encoded = "=?UTF-8?B?" + base64("Hola") + "?= =?UTF-8?B?" + base64("Mundo") + "?=";
        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("HolaMundo");
    }

    @Test
    void concatenaEncodedWordsAdyacentesMixtasBase64YQuotedPrintable() {
        String encoded = "=?UTF-8?B?" + base64("HELP") + "?= =?UTF-8?Q?=5FTEST?=";

        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("HELP_TEST");
    }

    @Test
    void preservaTextoPlanoAlrededorDeUnEncodedWord() {
        String encoded = "Prefijo " + "=?UTF-8?B?" + base64("MEDIO") + "?=" + " Sufijo";
        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("Prefijo MEDIO Sufijo");
    }

    @Test
    void textoSinCodificarSeDevuelveIgual() {
        String texto = "Texto normal sin codificar, con ñ y tildes áéíóú";
        assertThat(MimeDecoderUtil.decode(texto)).isEqualTo(texto);
    }

    @Test
    void valorNuloSeDevuelveIgual() {
        assertThat(MimeDecoderUtil.decode(null)).isNull();
    }

    @Test
    void valorEnBlancoSeDevuelveIgual() {
        assertThat(MimeDecoderUtil.decode("   ")).isEqualTo("   ");
    }

    @Test
    void base64InvalidoDejaElEncodedWordOriginalSinCambios() {
        String original = "=?UTF-8?B?###no-es-base64###?=";
        assertThat(MimeDecoderUtil.decode(original)).isEqualTo(original);
    }

    @Test
    void charsetDesconocidoCaeEnUtf8SinLanzarExcepcion() {
        String encoded = "=?CHARSET-INEXISTENTE-XYZ?B?" + base64("ABC") + "?=";
        assertThat(MimeDecoderUtil.decode(encoded)).isEqualTo("ABC");
    }

    @Test
    void quotedPrintableConIgualAlFinalSinDigitosSuficientesSeDejaLiteral() {
        assertThat(MimeDecoderUtil.decode("=?UTF-8?Q?abc=?=")).isEqualTo("abc=");
    }

    @Test
    void quotedPrintableConSecuenciaHexInvalidaSeDejaLiteral() {
        assertThat(MimeDecoderUtil.decode("=?UTF-8?Q?ab=ZZcd?=")).isEqualTo("ab=ZZcd");
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
