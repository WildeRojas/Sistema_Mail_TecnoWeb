package com.grupo06sa.sistema_inventario.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;

class DbErrorTranslatorTest {

    @Test
    void accesoDenegadoPorRolSeTraduceComoPermisoDenegado() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new RoleAccessDeniedException("No tiene acceso a este recurso."));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        assertThat(t.titulo()).isEqualTo("Acceso denegado");
        assertThat(t.mensaje()).isEqualTo("No tiene acceso a este recurso.");
    }

    @Test
    void accesoDenegadoSinMensajeUsaMensajeGenerico() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new RoleAccessDeniedException(null));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.PERMISO_DENEGADO);
        assertThat(t.mensaje()).isEqualTo("No tiene acceso a este recurso.");
    }

    @Test
    void credencialesInvalidasDeBaseDeDatosSeTraducenSinExponerDetalle() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new PermissionDeniedDataAccessException("denied", null));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Credenciales incorrectas");
        assertThat(t.mensaje()).doesNotContain("denied");
    }

    @Test
    void sqlExceptionConSqlState28SeTraduceComoCredencialesIncorrectas() {
        SQLException sqlEx = new SQLException("invalid password", "28000");
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(sqlEx);
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Credenciales incorrectas");
    }

    @Test
    void baseDeDatosCaidaSeTraduceComoNoDisponible() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new CannotCreateTransactionException("no se pudo abrir conexion")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos no disponible");
        assertThat(t.mensaje()).containsIgnoringCase("intente nuevamente");
    }

    @Test
    void dataAccessResourceFailureSeTraduceComoNoDisponible() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new DataAccessResourceFailureException("conexion perdida")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos no disponible");
    }

    @Test
    void timeoutDeConsultaSeTraduceComoSinRespuesta() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new QueryTimeoutException("timeout"));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos sin respuesta");
    }

    @Test
    void bloqueoConcurrenteSeTraduceComoBaseDeDatosOcupada() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new CannotAcquireLockException("lock"));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos ocupada");
    }

    @Test
    void fallaDeConcurrenciaSeTraduceComoBaseDeDatosOcupada() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(new ConcurrencyFailureException("conflicto"));
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos ocupada");
    }

    @Test
    void estructuraIncompatibleSeTraduceComoErrorBd() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new InvalidDataAccessResourceUsageException("tabla no existe")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Estructura de datos incompatible");
    }

    @Test
    void violacionDeIntegridadSeTraduceComoErrorDeValidacion() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new DataIntegrityViolationException("constraint violada")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(t.titulo()).isEqualTo("Datos no válidos");
    }

    @Test
    void illegalArgumentConMensajeSeMuestraTalCualComoValidacion() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new IllegalArgumentException("La compra solicitada no existe.")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(t.mensaje()).isEqualTo("La compra solicitada no existe.");
    }

    @Test
    void illegalStateConMensajeSeMuestraTalCualComoValidacion() {
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(
            new IllegalStateException("La compra no está en estado PENDIENTE.")
        );
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_VALIDACION);
        assertThat(t.mensaje()).isEqualTo("La compra no está en estado PENDIENTE.");
    }

    @Test
    void excepcionInesperadaSeTraduceComoErrorInternoSinExponerElMensajeOriginal() {
        RuntimeException fallaInesperada = new NullPointerException("detalle tecnico sensible con stacktrace interno");
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(fallaInesperada);
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_INTERNO);
        assertThat(t.titulo()).isEqualTo("Error interno");
        assertThat(t.mensaje()).doesNotContain("detalle tecnico sensible");
        assertThat(t.mensaje()).doesNotContainIgnoringCase("nullpointerexception");
    }

    @Test
    void excepcionEnvueltaConCausaDeAccesoADatosSeDetectaEnLaCadena() {
        RuntimeException envoltura = new RuntimeException(
            "fallo de negocio", new DataAccessResourceFailureException("bd caida")
        );
        DbErrorTranslator.Traduccion t = DbErrorTranslator.traducir(envoltura);
        assertThat(t.tipo()).isEqualTo(CommandResult.Tipo.ERROR_BD);
        assertThat(t.titulo()).isEqualTo("Base de datos no disponible");
    }
}
