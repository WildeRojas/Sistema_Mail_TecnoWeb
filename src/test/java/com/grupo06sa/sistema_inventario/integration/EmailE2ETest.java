package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.entity.CorreoProcesado;
import com.grupo06sa.sistema_inventario.entity.EstadoCorreo;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailE2ETest extends IntegracionBaseTest {

    @Test
    void comandoValidoPorCorreoObtieneRespuestaYQuedaRegistradoComoProcesado() throws Exception {
        String emailPropietario = emailUnico("propietario.e2e");
        crearPropietario(emailPropietario);
        String messageId = "e2e-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId);

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();

        MimeMessage[] recibidosPorRemitente = GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario);
        assertThat(recibidosPorRemitente).hasSize(1);
        assertThat(recibidosPorRemitente[0].getSubject()).isEqualTo("Respuesta");

        Optional<CorreoProcesado> registro = correoProcesadoRepository.findByMessageId("mid:" + messageId);
        assertThat(registro).isPresent();
        assertThat(registro.get().getEstado()).isEqualTo(EstadoCorreo.PROCESADO);
        assertThat(registro.get().getComando()).isEqualTo("LIS_PRODUCTO");
        assertThat(registro.get().getRemitente()).isEqualTo(emailPropietario);
    }

    @Test
    void variantePrefijoDeRespuestaDelMismoComandoTambienSeProcesa() throws Exception {
        String emailPropietario = emailUnico("propietario.e2e.re");
        crearPropietario(emailPropietario);
        String messageId = "e2e-re-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(emailPropietario, direccionBot(), "RE: LIS_PRODUCTO[\"\"]", null, messageId);

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).hasSize(1);

        Optional<CorreoProcesado> registro = correoProcesadoRepository.findByMessageId("mid:" + messageId);
        assertThat(registro).isPresent();
        assertThat(registro.get().getComando()).isEqualTo("LIS_PRODUCTO");
    }

    @Test
    void reinyeccionDelMismoMessageIdNoReprocesaNiRespondeDeNuevo() throws Exception {
        String emailPropietario = emailUnico("propietario.e2e.dup");
        crearPropietario(emailPropietario);
        String messageId = "e2e-dup-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId);
        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).hasSize(1);
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isPresent();

        enviarCorreo(emailPropietario, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId);
        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();

        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(emailPropietario)).hasSize(1);

        long cantidadFilas = correoProcesadoRepository.findAll().stream()
            .filter(c -> ("mid:" + messageId).equals(c.getMessageId()))
            .count();
        assertThat(cantidadFilas).isEqualTo(1);
    }

    @Test
    void correoConAsuntoQueNoEsComandoNoRecibeRespuestaYSeDescarta() throws Exception {
        String remitenteCualquiera = emailUnico("desconocido.e2e");
        String messageId = "e2e-nocmd-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(remitenteCualquiera, direccionBot(), "Reunion de mañana a las 10am", "Sin relacion con comandos", messageId);

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(GREEN_MAIL.getReceivedMessagesForDomain(remitenteCualquiera)).isEmpty();
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isEmpty();
    }

    @Test
    void remitenteNoRegistradoRecibeRespuestaDeRegistroRequerido() throws Exception {
        String remitenteNoRegistrado = emailUnico("noregistrado.e2e");
        String messageId = "e2e-noreg-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(remitenteNoRegistrado, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId);
        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        MimeMessage[] recibidos = GREEN_MAIL.getReceivedMessagesForDomain(remitenteNoRegistrado);
        assertThat(recibidos).hasSize(1);

        String cuerpo = cuerpoDe(recibidos[0]);
        assertThat(cuerpo).contains("No estás registrado en el sistema");
        assertThat(cuerpo).doesNotContainIgnoringCase("exception");
        assertThat(cuerpo).doesNotContain("at com.grupo06sa");
    }
}
