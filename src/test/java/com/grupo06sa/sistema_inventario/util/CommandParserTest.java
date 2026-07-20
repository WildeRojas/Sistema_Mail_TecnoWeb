package com.grupo06sa.sistema_inventario.util;

import static com.grupo06sa.sistema_inventario.util.CommandRequest.Estado.ERROR_SINTAXIS;
import static com.grupo06sa.sistema_inventario.util.CommandRequest.Estado.NO_ES_COMANDO;
import static com.grupo06sa.sistema_inventario.util.CommandRequest.Estado.RECONOCIDO;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "HELP", "help", "Help", "HELP[]", "HELP[\"\"]", "HELP()", "HELP( )",
        "  HELP  ", "HELP[   ]", "HELP(   )"
    })
    void reconoceVariantesDeHelpSinParametrosReales(String asunto) {
        CommandRequest request = CommandParser.parse(asunto);
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("HELP");
    }

    @Test
    void helpConParametroNoVacioSeParseaConEseParametro() {

        CommandRequest request = CommandParser.parse("HELP[\"algo\"]");
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("HELP");
        assertThat(request.getParams()).containsExactly("algo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RE: HELP", "RV: help", "FW: HELP", "FWD: HELP",
        "re:HELP", "RE:RV:HELP", "FWD: RE: FW: HELP", "  re : HELP"
    })
    void retiraPrefijosDeRespuestaOReenvioAntesDeReconocer(String asunto) {
        CommandRequest request = CommandParser.parse(asunto);
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("HELP");
    }

    @Test
    void reconoceComandoValidoConParametros() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[\"5\"]");
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("GET_USUARIO");
        assertThat(request.getParams()).containsExactly("5");
    }

    @Test
    void nombreDelComandoSeNormalizaAMayusculasSinAlterarParametros() {
        CommandRequest request = CommandParser.parse("get_usuario[\"Valor Exacto\"]");
        assertThat(request.getCommand()).isEqualTo("GET_USUARIO");
        assertThat(request.getParams()).containsExactly("Valor Exacto");
    }

    @Test
    void parserNoExigeParametroObligatorioSoloTokeniza() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[]");
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getParams()).isEmpty();
    }

    @Test
    void parserAceptaParametrosDeMasQueLuegoValidaCommandProcessor() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[\"1\",\"2\",\"3\"]");
        assertThat(request.getParams()).containsExactly("1", "2", "3");
    }

    @Test
    void detectaComillasSinCerrar() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[\"5]");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("comillas sin cerrar");
    }

    @Test
    void detectaCorcheteDeCierreFaltante() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[\"5\"");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("corchete de cierre");
    }

    @Test
    void detectaParametroSinComillas() {
        CommandRequest request = CommandParser.parse("GET_USUARIO[5]");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("comillas dobles");
    }

    @Test
    void detectaSeparadorIncorrectoEntreParametros() {
        CommandRequest request = CommandParser.parse("INS_CATEGORIA[\"Mochilas\";\"img\"]");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("Separador incorrecto");
    }

    @Test
    void detectaCaracteresSobrantesDespuesDelCierre() {
        CommandRequest request = CommandParser.parse("HELP[\"\"] basura");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("sobrantes");
    }

    @Test
    void detectaParentesisDeCierreFaltante() {
        CommandRequest request = CommandParser.parse("HELP(");
        assertThat(request.getEstado()).isEqualTo(ERROR_SINTAXIS);
        assertThat(request.getMensajeError()).contains("paréntesis de cierre");
    }

    @Test
    void preservaAcentosYTildesExactosEnParametros() {
        CommandRequest request = CommandParser.parse(
            "INS_PROVEEDOR[\"Ñandú S.A.\",\"70000000\",\"correo@x.com\",\"Dirección Ávila 123\"]"
        );
        assertThat(request.getParams()).containsExactly(
            "Ñandú S.A.", "70000000", "correo@x.com", "Dirección Ávila 123"
        );
    }

    @Test
    void normalizaUnicodeAFormaDeComposicionCanonicaNfc() {
        String nfd = "é";
        CommandRequest request = CommandParser.parse("INS_CATEGORIA[\"" + nfd + "\"]");
        assertThat(request.getParams().get(0)).isEqualTo("é");
    }

    @Test
    void permiteParametroVacioEntreOtrosNoVacios() {
        CommandRequest request = CommandParser.parse("INS_CATEGORIA[\"Mochilas\",\"\"]");
        assertThat(request.getParams()).containsExactly("Mochilas", "");
    }

    @Test
    void soportaEscapeDeComillasDentroDeUnParametro() {
        CommandRequest request = CommandParser.parse("INS_CATEGORIA[\"Bolsas \\\"Premium\\\"\"]");
        assertThat(request.getParams()).containsExactly("Bolsas \"Premium\"");
    }

    @Test
    void comandoInexistenteSeReconocePeroSuExistenciaLaValidaElRegistro() {
        CommandRequest request = CommandParser.parse("COMANDO_QUE_NO_EXISTE[\"x\"]");
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("COMANDO_QUE_NO_EXISTE");
    }

    @Test
    void asuntoVacioOEnBlancoNoEsComando() {
        assertThat(CommandParser.esComando("")).isFalse();
        assertThat(CommandParser.parse("").getEstado()).isEqualTo(NO_ES_COMANDO);
        assertThat(CommandParser.parse("   ").getEstado()).isEqualTo(NO_ES_COMANDO);
        assertThat(CommandParser.parse(null).getEstado()).isEqualTo(NO_ES_COMANDO);
    }

    @Test
    void textoLibreSinFormaDeComandoNoEsComando() {
        assertThat(CommandParser.esComando("Hola, ¿cómo estás?")).isFalse();
        assertThat(CommandParser.parse("Reunión de mañana a las 10am").getEstado()).isEqualTo(NO_ES_COMANDO);
    }

    @Test
    void asuntoExcesivamenteLargoSeClasificaComoNoEsComando() {
        String largo = "HELP" + "x".repeat(2000);
        assertThat(largo.length()).isGreaterThan(2000);
        CommandRequest request = CommandParser.parse(largo);
        assertThat(request.getEstado()).isEqualTo(NO_ES_COMANDO);
        assertThat(CommandParser.esComando(largo)).isFalse();
    }

    @Test
    void asuntoDentroDelLimitePermitidoSiSeReconoce() {
        String limite = "HELP[\"" + "a".repeat(100) + "\"]";
        assertThat(limite.length()).isLessThan(2000);
        assertThat(CommandParser.parse(limite).getEstado()).isEqualTo(RECONOCIDO);
    }

    @Test
    void esComandoEsVerdaderoParaSintaxisValidaYParaErrorDeSintaxis() {
        assertThat(CommandParser.esComando("HELP")).isTrue();
        assertThat(CommandParser.esComando("GET_USUARIO[\"5\"")).isTrue();
    }

    @Test
    void decodificaEncodedWordBase64EnAsuntoAntesDeReconocer() {
        String encoded = "=?UTF-8?B?" + Base64.getEncoder().encodeToString("HELP".getBytes(StandardCharsets.UTF_8)) + "?=";
        CommandRequest request = CommandParser.parse(encoded);
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("HELP");
    }

    @Test
    void decodificaEncodedWordQuotedPrintableDeAsuntoCompletoAntesDeReconocer() {

        String encoded = "=?UTF-8?Q?INS=5FCATEGORIA[=22Hola_Mundo=22]?=";
        CommandRequest request = CommandParser.parse(encoded);
        assertThat(request.getEstado()).isEqualTo(RECONOCIDO);
        assertThat(request.getCommand()).isEqualTo("INS_CATEGORIA");
        assertThat(request.getParams()).containsExactly("Hola Mundo");
    }
}
