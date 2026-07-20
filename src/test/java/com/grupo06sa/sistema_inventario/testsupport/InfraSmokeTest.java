package com.grupo06sa.sistema_inventario.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.security.RolNombre;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InfraSmokeTest extends IntegracionBaseTest {

    @Test
    void elContextoDeSpringLevantaConH2GreenMailYWireMock() {
        assertThat(GREEN_MAIL.isRunning()).isTrue();
        assertThat(WIRE_MOCK.isRunning()).isTrue();
        assertThat(rol(RolNombre.PROPIETARIO)).isNotNull();
        assertThat(rol(RolNombre.TRABAJADOR)).isNotNull();
        assertThat(rol(RolNombre.PROVEEDOR)).isNotNull();
    }

    @Test
    void puedeEnviarYRecibirUnCorreoPorGreenMail() throws Exception {
        String destino = emailUnico("smoke");
        enviarCorreo("origen@test.local", destino, "Asunto de prueba");

        boolean llego = GREEN_MAIL.waitForIncomingEmail(Duration.ofSeconds(5).toMillis(), 1);
        assertThat(llego).isTrue();
        assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1);
        assertThat(GREEN_MAIL.getReceivedMessages()[0].getSubject()).isEqualTo("Asunto de prueba");
    }

    @Test
    void wireMockRespondeElStubDeLoginDePagoFacil() throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:" + WIRE_MOCK.port() + "/login"))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
            .build();
        java.net.http.HttpResponse<String> respuesta =
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("test-access-token");
    }
}
