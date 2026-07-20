package com.grupo06sa.sistema_inventario.processor;

import com.grupo06sa.sistema_inventario.security.RoleAccessDeniedException;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import java.sql.SQLException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

public final class DbErrorTranslator {

    public record Traduccion(String titulo, String mensaje, CommandResult.Tipo tipo) {
    }

    private DbErrorTranslator() {
    }

    public static Traduccion traducir(Throwable ex) {
        if (ex instanceof RoleAccessDeniedException) {
            return new Traduccion(
                "Acceso denegado",
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "No tiene acceso a este recurso.",
                CommandResult.Tipo.PERMISO_DENEGADO
            );
        }

        Throwable current = ex;
        while (current != null) {
            if (current instanceof PermissionDeniedDataAccessException
                || (current instanceof SQLException sqlEx && esCredencialInvalida(sqlEx))) {
                return new Traduccion(
                    "Credenciales incorrectas",
                    "No se pudo autenticar contra la base de datos. Contacte al administrador del sistema.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            if (current instanceof CannotCreateTransactionException
                || current instanceof DataAccessResourceFailureException) {
                return new Traduccion(
                    "Base de datos no disponible",
                    "No se pudo conectar a la base de datos (servicio caído o límite de conexiones alcanzado). "
                        + "Intente nuevamente más tarde.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            if (current instanceof QueryTimeoutException) {
                return new Traduccion(
                    "Base de datos sin respuesta",
                    "La base de datos tardó demasiado en responder (tiempo de espera agotado). Intente nuevamente más tarde.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            if (current instanceof CannotAcquireLockException
                || current instanceof ConcurrencyFailureException) {
                return new Traduccion(
                    "Base de datos ocupada",
                    "La base de datos está procesando otra operación sobre el mismo registro. Intente nuevamente.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            if (current instanceof InvalidDataAccessResourceUsageException) {
                return new Traduccion(
                    "Estructura de datos incompatible",
                    "La base de datos no tiene la estructura esperada (posibles migraciones pendientes). "
                        + "Contacte al administrador del sistema.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            if (current instanceof DataIntegrityViolationException) {
                return new Traduccion(
                    "Datos no válidos",
                    "La base de datos rechazó la operación porque los datos no cumplen una restricción.",
                    CommandResult.Tipo.ERROR_VALIDACION
                );
            }
            if (current instanceof DataAccessException || current instanceof TransactionException) {
                return new Traduccion(
                    "Error de base de datos",
                    "La base de datos no pudo procesar la solicitud. Intente nuevamente más tarde.",
                    CommandResult.Tipo.ERROR_BD
                );
            }
            current = current.getCause();
        }

        if ((ex instanceof IllegalArgumentException || ex instanceof IllegalStateException)
            && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return new Traduccion("Solicitud no válida", ex.getMessage(), CommandResult.Tipo.ERROR_VALIDACION);
        }

        return new Traduccion(
            "Error interno",
            "No fue posible completar el comando debido a un error interno. "
                + "El incidente fue registrado y puede volver a intentarlo.",
            CommandResult.Tipo.ERROR_INTERNO
        );
    }

    private static boolean esCredencialInvalida(SQLException sqlEx) {
        String sqlState = sqlEx.getSQLState();
        return sqlState != null && sqlState.startsWith("28");
    }
}
