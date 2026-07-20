package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.CorreoProcesado;
import com.grupo06sa.sistema_inventario.entity.EstadoCorreo;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResilienciaMensajeYAntiLoopTest extends IntegracionBaseTest {

    @Test
    void unMensajeConErrorDeNegocioNoImpideProcesarElSiguienteDelMismoCiclo() throws Exception {
        String emailPropietario = emailUnico("propietario.resiliencia.msg");
        crearPropietario(emailPropietario);

        String idMensajeConError = "resil-error-" + siguienteSecuencia() + "@test.local";
        String idMensajeValido = "resil-ok-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(emailPropietario, direccionBot(), "GET_PRODUCTO[\"999999999\"]", null, idMensajeConError);

        enviarCorreo(emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, idMensajeValido);

        assertThat(contarMensajesEnBuzonDelBot()).isEqualTo(2);

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();

        Optional<CorreoProcesado> registroError = correoProcesadoRepository.findByMessageId("mid:" + idMensajeConError);
        Optional<CorreoProcesado> registroValido = correoProcesadoRepository.findByMessageId("mid:" + idMensajeValido);

        assertThat(registroError).isPresent();
        assertThat(registroError.get().getEstado()).isEqualTo(EstadoCorreo.ERROR);
        assertThat(registroError.get().getComando()).isEqualTo("GET_PRODUCTO");

        assertThat(registroValido).isPresent();
        assertThat(registroValido.get().getEstado()).isEqualTo(EstadoCorreo.PROCESADO);
        assertThat(registroValido.get().getComando()).isEqualTo("LIS_PRODUCTO");

        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).hasSize(2);
    }

    @Test
    void correoConCabeceraAutoSubmittedNoRecibeRespuestaYSeDescartaSinRegistrar() throws Exception {
        String emailPropietario = emailUnico("propietario.resiliencia.auto");
        crearPropietario(emailPropietario);
        String messageId = "resil-auto-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(
            emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId,
            Map.of("Auto-Submitted", "auto-replied")
        );

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).isEmpty();
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isEmpty();
    }

    @Test
    void correoConPrecedenceBulkNoRecibeRespuestaYSeDescartaSinRegistrar() throws Exception {
        String emailPropietario = emailUnico("propietario.resiliencia.bulk");
        crearPropietario(emailPropietario);
        String messageId = "resil-bulk-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(
            emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId,
            Map.of("Precedence", "bulk")
        );

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).isEmpty();
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isEmpty();
    }

    @Test
    void correoNormalSinCabecerasAutomaticasSiRecibeRespuestaComoControlDeSanidad() throws Exception {
        String emailPropietario = emailUnico("propietario.resiliencia.normal");
        crearPropietario(emailPropietario);
        String messageId = "resil-normal-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(
            emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId,
            Map.of("Auto-Submitted", "no")
        );

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        MimeMessage[] recibidos = GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario);
        assertThat(recibidos).hasSize(1);
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isPresent();
    }
}
