package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.testsupport.IntegracionBaseTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ResilienciaBaseDeDatosTest extends IntegracionBaseTest {

    @MockitoSpyBean
    private UsuarioRepository usuarioRepositoryEspia;

    @Test
    void baseDeDatosCaidaRespondeErrorEnEspanolNoBorraElMensajeYSeRecuperaSinReiniciar() throws Exception {
        String email = emailUnico("propietario.resiliencia.bd");
        crearPropietario(email);
        String messageId = "resil-bd-" + siguienteSecuencia() + "@test.local";

        enviarCorreo(email, direccionBot(), "LIS_PRODUCTO[\"\"]", null, messageId);

        doThrow(new DataAccessResourceFailureException("Simulación de caída de base de datos (prueba de resiliencia)."))
            .when(usuarioRepositoryEspia).findByEmailIgnoreCase(anyString());

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isEqualTo(1);

        MimeMessage[] recibidos = GREEN_MAIL.getReceivedMessagesForDomain(email);
        assertThat(recibidos).hasSize(1);
        String cuerpo = cuerpoDe(recibidos[0]);
        assertThat(cuerpo).contains("Base de datos no disponible");
        assertThat(cuerpo).doesNotContainIgnoringCase("exception");
        assertThat(cuerpo).doesNotContain("at com.grupo06sa");
        assertThat(cuerpo).doesNotContain("Simulación de caída de base de datos");

        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isEmpty();

        Mockito.reset(usuarioRepositoryEspia);

        emailReceiverService.checkInbox();

        assertThat(contarMensajesEnBuzonDelBot()).isZero();
        assertThat(correoProcesadoRepository.findByMessageId("mid:" + messageId)).isPresent();

        MimeMessage[] recibidosTrasRecuperacion = GREEN_MAIL.getReceivedMessagesForDomain(email);
        assertThat(recibidosTrasRecuperacion).hasSize(2);
    }
}
