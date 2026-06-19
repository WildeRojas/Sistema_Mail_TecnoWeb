package com.grupo06sa.sistema_inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo06sa.sistema_inventario.processor.CommandProcessor;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailReceiverServiceTest {
    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private CommandProcessor commandProcessor;

    @Test
    void enviaAvisoDeBaseDeDatosCuandoElProcesadorFallaPorConexion() {
        EmailReceiverService receiverService = new EmailReceiverService(emailSenderService, commandProcessor);
        when(commandProcessor.process(any(CommandRequest.class), eq("cliente@test.com")))
            .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        ReflectionTestUtils.invokeMethod(receiverService, "processMessage", List.of(
            "From: Cliente <cliente@test.com>",
            "Subject: LIS_PRODUCTO[\"*\"]",
            "",
            "contenido"
        ));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSenderService).sendEmail(eq("cliente@test.com"), eq("Respuesta"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
            .contains("Base de datos no disponible")
            .contains("No se pudo conectar a la base de datos")
            .doesNotContain("Connection refused");
    }
}
