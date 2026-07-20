# Stub WireMock de Pago Facil (infraestructura de pruebas)

Estos mappings son un **doble de pruebas** del contrato de Pago Facil, no configuración
del producto ni credenciales reales. Sirven para que `PagoFacilService` (login,
generate-qr, query-transaction) tenga un backend HTTP determinista durante `./mvnw test`,
servido por un `WireMockServer` embebido en proceso (sin Docker) desde
`IntegracionBaseTest`.

## Archivos

- `mappings/login.json` — responde 200 a cualquier ruta que contenga "login" con un
  `accessToken` de prueba (`test-access-token`), disponible tanto en la raíz como en
  `values.accessToken` / `values.token` para máxima compatibilidad.
- `mappings/generate-qr.json` — responde 200 a rutas tipo `generate-qr` / `generarQr`
  con un PNG 1x1 en base64 (`values.qrImage`, con alias `values.qrBase64`), un
  `transactionId` generado con response templating (`{{randomValue ...}}`) y una
  `expirationDate` futura.
- `mappings/query-transaction-*.json` — responden a rutas tipo `query-transaction` /
  `consultarEstado` con `values.paymentStatus`. Usan un WireMock **Scenario** llamado
  `pago` con tres estados:
  - `Started` (estado inicial, por defecto) → `paymentStatus: 4` (expirado/pendiente).
  - `PAGADO` → `paymentStatus: 2`.
  - `CANCELADO` → `paymentStatus: 3`.

## Cómo cambiar el estado del escenario en una prueba

WireMock no cambia de estado solo: hay que pedirlo explícitamente. En los tests JUnit de
este proyecto, con el `WireMockServer` embebido expuesto como `WIRE_MOCK` en
`IntegracionBaseTest`:

```java
WIRE_MOCK.setScenarioState("pago", "PAGADO");
```

También se puede resetear todo el estado de escenarios con
`WIRE_MOCK.resetAllScenarios()` (o `WIRE_MOCK.resetAll()`, que además limpia peticiones
registradas, usado en `baseSetUp()` antes de cada prueba).
