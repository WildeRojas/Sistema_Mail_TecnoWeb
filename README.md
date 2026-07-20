# Sistema Mail — Tiendas Junior

Sistema Mail es el backend que permite operar por correo electrónico el inventario, las compras y la
relación con proveedores de Tiendas Junior. No existe venta a cliente final: el negocio que modela esta
aplicación es exclusivamente B2B, entre la empresa (propietario y trabajadores) y sus proveedores externos.

Cada acción del sistema —dar de alta un producto, registrar una oferta de proveedor, solicitar una
compra, recibir mercadería, pagar a un proveedor con QR, generar un reporte— se dispara enviando un
correo cuyo asunto contiene un comando con una sintaxis fija. El sistema responde por correo, en español,
con el resultado de la operación o con el motivo del error.

Para el catálogo completo de comandos ver [README_Comandos.md](README_Comandos.md). Para los flujos de
negocio de punta a punta ver [README_Flujo.md](README_Flujo.md). Para la matriz de comandos por rol y las
reglas de aislamiento por proveedor ver [docs/matriz-roles.md](docs/matriz-roles.md).

## Roles

El sistema reconoce exactamente tres roles de negocio:

- **PROPIETARIO**: dueño del negocio, acceso completo.
- **TRABAJADOR**: personal interno, acceso operativo (catálogo, inventario, alertas, reportes), sin
  gestión de usuarios ni de proveedores/productos/categorías.
- **PROVEEDOR**: usuario externo asociado a un proveedor concreto; solo ve y opera sobre sus propios
  productos ofertados, solicitudes, compras y pagos.

## Arquitectura de componentes

El procesamiento de un correo entrante sigue siempre la misma cadena de componentes:

```
EmailReceiverService (POP3)
  -> CommandParser (reconocimiento y normalización del asunto)
  -> ComandoRegistry (catálogo declarativo: nombre, roles, aridad, handler)
  -> CommandProcessor (autenticación, autorización, validación de aridad, ejecución)
  -> service/command/*CommandService (lógica de comando, un servicio por módulo)
  -> service/* (reglas de negocio, JPA/Hibernate sobre PostgreSQL)
  -> EmailSenderService (respuesta por SMTP)
```

- **EmailReceiverService**: sondea el buzón POP3 del bot (`@Scheduled`), habla el protocolo POP3
  directamente sobre un socket (sin cliente JavaMail). Antes de ejecutar un comando comprueba
  idempotencia contra `CorreoProcesado` (por Message-ID, o UID del servidor, o hash de cabeceras como
  último recurso); el mensaje solo se borra (`DELE`) después de conocer el resultado. Un fallo al
  procesar un mensaje no detiene el resto del ciclo.
- **CommandParser**: único punto de reconocimiento del asunto (`CommandParser.esComando`). Normaliza
  Unicode, decodifica encoded-words MIME, retira prefijos de respuesta/reenvío y tokeniza
  `COMANDO["p1","p2",...]`. Nunca lanza excepciones: siempre devuelve un resultado con estado explícito.
- **ComandoRegistry**: catálogo único y autoritativo de comandos (nombre, roles autorizados, aridad
  mínima/máxima, si el comando queda restringido al proveedor del remitente, y el handler). Es la fuente
  de verdad de todo lo documentado en este repositorio.
- **CommandProcessor**: orquesta el ciclo completo — autentica al remitente (`SecurityService`), autoriza
  contra el rol, valida la cantidad de parámetros, ejecuta el handler y traduce cualquier excepción a un
  mensaje en español (`DbErrorTranslator`). Ningún camino deja una excepción sin controlar.
- **PagoFacilWebhookController**: endpoint público (sin autenticación) que recibe la confirmación de pago
  de Pago Fácil. Ver el contrato exacto en [README_Flujo.md](README_Flujo.md).
- **Resiliencia e idempotencia**:
  - El envío de correo (`EmailSenderService`) reintenta con backoff creciente y, si SMTP no responde,
    encola la respuesta en un spool local durable que un job programado reintenta más tarde sin duplicar
    envíos.
  - La recepción (`EmailReceiverService`) reconecta en el siguiente ciclo si el buzón no responde, y
    descarta un mensaje como "venenoso" tras un número máximo de intentos fallidos de lectura.
  - Los efectos de un comando (alta, actualización, ingreso de inventario, pago) solo se ejecutan una vez
    por correo: `CorreoProcesado` evita reprocesar un Message-ID ya atendido, y `RECIBIR_COMPRA` es
    idempotente por sí mismo (`Compra.inventarioIngresado`).

## Compilación y pruebas

```
./mvnw test
```

En Windows sin Git Bash: `mvnw.cmd test`. La suite corre entera sin Docker ni servicios externos:
incluye pruebas unitarias (parser, matriz de roles, distancia de Levenshtein, decodificación
quoted-printable), pruebas de integración sobre una base H2 en memoria (modo de compatibilidad
PostgreSQL) con GreenMail y WireMock embebidos en proceso (flujo solicitud → compra → recepción → pago →
callback, idempotencia de correo y de webhook, aislamiento por proveedor) y pruebas E2E de correo
(SMTP → GreenMail → sondeo → respuesta en el buzón).

Para compilar sin ejecutar pruebas: `./mvnw package -DskipTests`.

## Ejecución local paso a paso

### Opción A — Verificar todo el sistema (sin instalar nada)

Es la forma recomendada de comprobar que todo funciona. No requiere PostgreSQL, ni servidor de correo, ni
Pago Fácil, ni Docker: la base de datos es H2 en memoria y el correo y la pasarela se simulan en el propio
proceso (GreenMail y WireMock).

1. Requisito único: JDK 17 instalado (`java -version` debe indicar 17). Maven no hace falta: se usa el
   wrapper incluido (`./mvnw` o `mvnw.cmd`).
2. Desde la raíz del proyecto:

   ```
   ./mvnw test
   ```

3. Al finalizar debe leerse `Tests run: 217, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.

### Opción B — Ejecutar la aplicación real

Para operar el sistema de verdad hacen falta tres servicios propios: una base PostgreSQL, una cuenta de
correo (POP3 de entrada + SMTP de salida) y la URL de la API de Pago Fácil.

1. Requisitos: JDK 17 y un PostgreSQL accesible. Crear la base:

   ```
   createdb sistema_mail
   ```

2. Crear el archivo `src/main/resources/application.properties` (está fuera del control de versiones) con
   los datos reales del entorno. Plantilla:

   ```properties
   server.port=8080

   spring.datasource.url=jdbc:postgresql://localhost:5432/sistema_mail
   spring.datasource.username=TU_USUARIO_PG
   spring.datasource.password=TU_PASSWORD_PG
   spring.jpa.hibernate.ddl-auto=update

   mail.server=HOST_DE_TU_CORREO
   mail.smtp.port=587
   mail.pop3.port=110
   mail.user=CUENTA_DEL_BOT
   mail.password=PASSWORD_DEL_BOT

   pagofacil.url.base=URL_BASE_DE_PAGO_FACIL
   pagofacil.token.service=TU_TOKEN_SERVICE
   pagofacil.token.secret=TU_TOKEN_SECRET
   pagofacil.callback.url=http://TU_HOST_PUBLICO:8080/pagos/pagofacil/callback
   pagofacil.monto.tope=0

   app.seed.enabled=true
   app.mail.spool.dir=./spool
   ```

   Con `app.seed.enabled=true` se cargan datos mínimos de demostración (propietario, trabajador, dos
   proveedores con sus usuarios, catálogo, ofertas e inventario) la primera vez que arranca.

3. Arrancar la aplicación:

   ```
   ./mvnw spring-boot:run
   ```

4. A partir de ahí el sistema sondea el buzón cada 10 segundos. Para probar un comando, enviar un correo a
   la cuenta del bot con el comando en el **asunto** (por ejemplo `LIS_PRODUCTO[""]` desde el correo del
   propietario). El sistema responde por correo con el resultado. El webhook de Pago Fácil queda expuesto en
   `POST /pagos/pagofacil/callback`.

Los pasos del procesamiento quedan registrados en el log de forma clara, por ejemplo:

```
INFO  c.g.s.service.EmailReceiverService : Comando recibido: remitente=propietario@..., comando=LIS_PRODUCTO
INFO  c.g.s.service.EmailReceiverService : Resultado del comando LIS_PRODUCTO de propietario@...: OK
INFO  c.g.s.service.EmailSenderService   : Correo enviado a propietario@...
```

## Configuración

`src/main/resources/application.properties` (configuración real, con credenciales) está excluido del
control de versiones, y apunta a PostgreSQL en despliegue real. Se versiona el perfil de pruebas:

- `src/test/resources/application-test.properties`: perfil `test`, usado por las pruebas de integración.
  La base de datos es H2 en memoria (sin Docker); GreenMail y WireMock corren embebidos en proceso y sus
  puertos dinámicos se sobreescriben en tiempo de ejecución vía `@DynamicPropertySource`
  (`IntegracionBaseTest`).

Propiedades relevantes (sin valores secretos — los que se muestran son los del entorno de pruebas local):

| Propiedad | Descripción |
|---|---|
| `mail.server` | Host del servidor de correo (SMTP/POP3) del bot. |
| `mail.smtp.port` | Puerto SMTP de salida. |
| `mail.pop3.port` | Puerto POP3 de entrada. |
| `mail.user` / `mail.password` | Credenciales de la cuenta del bot. |
| `mail.receiver.poll.fixed-delay-ms` | Intervalo del sondeo POP3 (por defecto 10000 ms). |
| `mail.sender.spool.flush-delay-ms` | Intervalo de reintento del spool de correo saliente (por defecto 60000 ms). |
| `pagofacil.url.base` | URL base de la API de Pago Fácil (en pruebas, el stub de WireMock). |
| `pagofacil.token.service` / `pagofacil.token.secret` | Credenciales de autenticación ante Pago Fácil. |
| `pagofacil.callback.url` | URL pública que Pago Fácil invoca al confirmar un pago. |
| `pagofacil.monto.tope` | Si es mayor a 0, actúa como techo del importe enviado a la pasarela (se cobra `min(tope, monto real)`); `0` usa siempre el monto real de la compra. |
| `app.seed.enabled` | Habilita el sembrado de datos mínimos de demostración al arrancar. |
| `app.mail.spool.dir` | Directorio donde se encola de forma durable el correo saliente cuando SMTP no responde. |
| `spring.datasource.*` | Conexión a PostgreSQL en despliegue real; H2 en memoria en test. |
| `spring.jpa.hibernate.ddl-auto` | `update` en dev/producción, `create-drop` en test. |
| `server.port` | Puerto HTTP de la aplicación (expone el webhook de Pago Fácil). |

No hay endpoints ni pantallas web para el usuario final: toda la interacción funcional ocurre por
correo, salvo el webhook de confirmación de pago descrito en [README_Flujo.md](README_Flujo.md).
