package com.grupo06sa.sistema_inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.service.EmailSenderService;
import com.grupo06sa.sistema_inventario.service.MailSpoolService;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ResilienciaSpoolSmtpTest {

    private int puertoSmtpRoto;
    private Path spoolDir;
    private MailSpoolService mailSpoolService;
    private EmailSenderService emailSenderService;
    private GreenMail smtpRecuperado;

    @BeforeEach
    void configurar() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoSmtpRoto = socket.getLocalPort();
        }
        spoolDir = Files.createTempDirectory("spool-resiliencia-smtp-");

        mailSpoolService = new MailSpoolService();
        ReflectionTestUtils.setField(mailSpoolService, "directorioSpool", spoolDir.toString());

        emailSenderService = new EmailSenderService(mailSpoolService);
        ReflectionTestUtils.setField(emailSenderService, "mailServer", "127.0.0.1");
        ReflectionTestUtils.setField(emailSenderService, "smtpPort", puertoSmtpRoto);
        ReflectionTestUtils.setField(emailSenderService, "mailUser", "bot@tiendasjunior.local");
        ReflectionTestUtils.setField(emailSenderService, "mailPassword", "bot");
    }

    @AfterEach
    void limpiar() {
        if (smtpRecuperado != null) {
            smtpRecuperado.stop();
        }
    }

    @Test
    void smtpCaidoEncolaEnSpoolYAlRecuperarseFlushEntregaYBorraSinDuplicar() throws Exception {
        String destinatario = "destino.spool@test.local";

        emailSenderService.sendEmail(destinatario, "Asunto de prueba de spool", "<html><body>Cuerpo de prueba</body></html>");

        assertThat(mailSpoolService.listarPendientes()).hasSize(1);

        smtpRecuperado = new GreenMail(new ServerSetup(puertoSmtpRoto, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        smtpRecuperado.start();

        emailSenderService.flushSpool();

        assertThat(mailSpoolService.listarPendientes()).isEmpty();
        assertThat(smtpRecuperado.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1)).isTrue();
        assertThat(smtpRecuperado.getReceivedMessagesForDomain(destinatario)).hasSize(1);

        emailSenderService.flushSpool();
        assertThat(smtpRecuperado.getReceivedMessages()).hasSize(1);
    }

    @Test
    void smtpCaidoConAdjuntoTambienSeEncolaYSeEntregaAlRecuperar() throws Exception {
        String destinatario = "destino.spool.adjunto@test.local";
        byte[] datos = "contenido-adjunto".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var adjunto = new com.grupo06sa.sistema_inventario.util.EmailAttachment("nota.txt", "text/plain", datos);

        emailSenderService.sendEmailWithAttachment(destinatario, "Asunto con adjunto", "<html><body>Con adjunto</body></html>", adjunto);

        assertThat(mailSpoolService.listarPendientes()).hasSize(1);

        smtpRecuperado = new GreenMail(new ServerSetup(puertoSmtpRoto, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        smtpRecuperado.start();

        emailSenderService.flushSpool();

        assertThat(mailSpoolService.listarPendientes()).isEmpty();
        assertThat(smtpRecuperado.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1)).isTrue();
        assertThat(smtpRecuperado.getReceivedMessagesForDomain(destinatario)).hasSize(1);
    }
}
