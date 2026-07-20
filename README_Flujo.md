# Flujos de negocio — Sistema Mail Tiendas Junior

Este documento describe los flujos de negocio de punta a punta soportados por el sistema: inventario,
ofertas y compras a proveedores, pagos y reportes. No existe flujo de venta a cliente final ni rol
CLIENTE: todo el negocio es B2B, entre Tiendas Junior (PROPIETARIO/TRABAJADOR) y sus proveedores
(PROVEEDOR). El catálogo exacto de comandos usado en cada paso está en
[README_Comandos.md](README_Comandos.md).

## Alta de usuarios

No existe auto-registro. Un correo enviado desde una dirección no registrada (o desde una cuenta
inactiva) siempre recibe la misma plantilla: debe contactar al propietario del negocio para que lo
registre. El PROPIETARIO da de alta usuarios con `INS_USUARIO`, indicando el rol (`PROPIETARIO`,
`TRABAJADOR` o `PROVEEDOR`); si el rol es `PROVEEDOR`, el usuario queda asociado a un proveedor
existente (`id_proveedor` obligatorio en ese caso) y solo podrá operar sobre los datos de ese proveedor.

## Alta de ofertas por proveedor

Cada proveedor gestiona, desde su propio correo registrado, el catálogo de productos internos que está
dispuesto a abastecer y a qué costo:

1. El proveedor envía `INS_OFERTA["id_producto","costo_unitario","tiempo_reposicion_dias?","cantidad_minima?"]`.
   No puede registrar dos ofertas para el mismo producto: si ya existe una, debe usar `UPD_OFERTA`.
2. Para ajustar costo, tiempo de reposición, cantidad mínima o disponibilidad, envía
   `UPD_OFERTA["id_oferta","costo_unitario","tiempo_reposicion_dias?","cantidad_minima?","disponible(SI|NO)?"]`.
   Al actualizar el costo, el sistema conserva el costo anterior junto al nuevo (histórico de precio de
   oferta) y registra el momento del cambio.
3. `DEL_OFERTA["id_oferta"]` elimina la oferta.
4. `LIS_OFERTA[""]` lista únicamente las ofertas propias del proveedor que envía el correo.

Un mismo producto interno puede ser ofertado por varios proveedores a distinto costo, o ser exclusivo de
uno solo; ambos casos son válidos y se usan, por ejemplo, para decidir a quién comprarle.

## Comparación de ofertas

El PROPIETARIO compara todas las ofertas vigentes de un producto con `COMPARAR_OFERTAS["id_producto"]`:
el sistema devuelve las ofertas ordenadas por costo, señalando la mejor oferta disponible. Este comando
es puramente informativo: no crea ni modifica ningún registro.

## Solicitud de compra y atención del proveedor

Antes de comprometer una compra formal, el PROPIETARIO puede pedirle a un proveedor una cotización o
disponibilidad concreta:

1. `INS_SOLICITUD["id_proveedor","id_producto","cantidad","id_almacen"]` — requiere que el proveedor
   tenga una oferta disponible para ese producto (si no la tiene, se rechaza). Queda en estado
   `PENDIENTE` con un costo estimado (calculado con el costo de la oferta vigente) y se notifica al
   proveedor por correo.
2. El proveedor responde con una de dos acciones, siempre sobre solicitudes dirigidas a él:
   - `ATENDER_SOLICITUD["id_solicitud","costo_ofrecido"]` → la solicitud pasa a `ATENDIDA`.
   - `RECHAZAR_SOLICITUD["id_solicitud","motivo"]` → la solicitud pasa a `RECHAZADA`.
3. `LIS_SOLICITUD[""]` es el mismo comando para PROPIETARIO y PROVEEDOR: el PROPIETARIO ve todas las
   solicitudes, el PROVEEDOR solo las que le corresponden.

Una solicitud ya `ATENDIDA` o `RECHAZADA` no puede volver a atenderse ni rechazarse.

Cuando el PROPIETARIO crea una compra (`INS_COMPRA`) para un proveedor que tiene exactamente una
solicitud en estado `ATENDIDA` pendiente de convertir, el sistema la enlaza automáticamente a la nueva
compra y la marca `CONVERTIDA`.

## Compra: creación, detalle, finalización y recepción

El ciclo de vida de una compra tiene cuatro pasos, todos ejecutados por el PROPIETARIO:

1. **Creación** — `INS_COMPRA["id_proveedor","id_almacen"]`. Requiere que el proveedor tenga al menos
   una oferta disponible. Queda en estado `PENDIENTE`, con almacén de destino ya definido (el almacén
   donde se recibirá la mercadería).
2. **Detalle (snapshot de costo)** — `INS_DETALLE_COMPRA["id_compra","id_oferta","cantidad"]`, repetido
   por cada producto. La oferta debe pertenecer al proveedor de la compra y estar disponible. El costo
   unitario de la oferta se copia como una fotografía inmutable dentro del detalle: si el proveedor
   actualiza después el costo de su oferta, los detalles ya agregados no cambian.
3. **Finalización** — `FINALIZAR_COMPRA["id_compra"]`. Exige que la compra tenga al menos un detalle.
   Calcula el total sumando los subtotales, cambia el estado a `EN_ESPERA` y envía automáticamente al
   proveedor, por correo, la orden de compra con el detalle completo.
4. **Recepción** — `RECIBIR_COMPRA["id_compra"]`. Solo procede si la compra está `EN_ESPERA`. Ingresa al
   inventario del almacén de destino la cantidad de cada detalle, registra un movimiento `INGRESO` por
   cada producto (asociado a la compra) y cambia el estado a `RECIBIDA`; notifica a todos los usuarios
   PROPIETARIO activos. Es **idempotente**: si la compra ya tiene el inventario ingresado
   (`inventarioIngresado=true`), repetir el comando no vuelve a sumar stock ni a duplicar movimientos.

`LIS_COMPRA[""]` y `GET_COMPRA["id"]` están disponibles también para el PROVEEDOR, restringidos a sus
propias compras (ownership por `proveedor_id`).

Los estados `RECIBIDA` y `PAGADA` de una compra son independientes entre sí y pueden darse en cualquier
orden: una compra puede pagarse antes o después de recibirse.

## Pago a proveedor con QR Pago Fácil

1. `PAGAR_COMPRA["id_compra"]` (PROPIETARIO) genera un código QR de cobro a través de Pago Fácil por el
   saldo pendiente de la compra (total menos lo ya pagado). Si había un pago `PENDIENTE` previo de la
   misma compra, se descarta antes de crear el nuevo. Se crea un registro `Pago` en estado `PENDIENTE`
   con referencia `TJ{id_compra}-{id_pago}`, se persisten el `transactionId` y la fecha de vencimiento
   devueltos por Pago Fácil, y se responde por correo con la imagen del QR adjunta (PNG).
2. **Confirmación por callback** — Pago Fácil notifica el resultado mediante:

   ```
   POST /pagos/pagofacil/callback
   ```

   Endpoint público, sin autenticación ni rol, exento de CSRF. Contrato exacto:
   - Solo se procesa si el campo `Estado` (o `estado`) llega igual a **2**; cualquier otro valor se
     responde igual mas sin ningún efecto.
   - La referencia del pago se busca, en orden de prioridad, en `PedidoID`, `pedidoId` o
     `paymentNumber`; si ninguno viene informado, se usa `transaccion` para buscar por `transactionId`.
   - No se leen ni validan monto, fecha, hora ni método de pago del payload.
   - Si el pago existe y aún no estaba `PAGADO`, se marca `PAGADO` (con lock pesimista sobre la fila),
     se guarda la fecha de pago y una versión saneada del payload (sin secretos); se recarga la compra y,
     si el total pagado ya cubre el total de la compra, la compra pasa a `PAGADA`. El callback nunca
     toca inventario ni envía correos dentro de su propia transacción.
   - La respuesta HTTP es **siempre** `200 OK` con el mismo cuerpo JSON, exista o no el pago, y aunque
     `Estado` sea distinto de 2:
     ```json
     {"error":0,"status":1,"message":"Notificación recibida","values":true}
     ```
     Cualquier fallo interno solo se registra en el log; nunca cambia la respuesta.
   - Idempotencia: la transición a `PAGADO` está protegida por el chequeo de estado, restricciones
     `UNIQUE` sobre referencia y `transactionId`, y el lock pesimista, de modo que dos callbacks
     concurrentes para el mismo pago producen una única transición.
3. `VERIFICAR_PAGO["id_compra"]` (PROPIETARIO; PROVEEDOR solo sobre sus propias compras) consulta de
   forma activa el estado de la transacción ante Pago Fácil (fuera del callback) y, si Pago Fácil
   confirma el pago, aplica la misma lógica de marcado que el callback.
4. `LIS_PAGO[""]` — PROPIETARIO ve todos los pagos; PROVEEDOR solo los recibidos por sus propias
   compras.

El pago nunca mueve inventario: la recepción de mercadería (`RECIBIR_COMPRA`) y el pago
(`PAGAR_COMPRA`/callback) son procesos independientes.

## Alertas de stock

Cada vez que una salida, un ajuste negativo o un traslado deja el stock total de un producto (sumado en
todos los almacenes) por debajo o igual a su stock mínimo configurado, el sistema genera una alerta
automática (`AlertaStock`), sin necesidad de ningún comando. No se crea una alerta nueva si ya existe una
pendiente para el mismo producto. El PROPIETARIO o TRABAJADOR consulta las alertas pendientes con
`LIS_ALERTA[""]` y las marca resueltas con `ATENDER_ALERTA["id_alerta"]`.

## Traslados internos de stock

`TRASLADO_STOCK["id_producto","id_almacen_origen","id_almacen_destino","cantidad"]` (PROPIETARIO,
TRABAJADOR) mueve stock real entre dos almacenes distintos: descuenta del almacén de origen (verificando
que haya stock suficiente), sube en el almacén de destino (verificando su capacidad si tiene un límite
configurado) y registra un único movimiento de tipo `TRASLADO`. Puede disparar una alerta de stock sobre
el almacén de origen si el traslado deja el producto por debajo de su mínimo.

## Reportes

- `REP_INVENTARIO[""]` (PROPIETARIO, TRABAJADOR): PDF con el catálogo de productos, su stock por
  almacén, stock mínimo y categoría.
- `REP_COMPRAS["YYYY-MM"]` (PROPIETARIO; PROVEEDOR con una variante filtrada a sus propias compras):
  PDF con las compras del mes indicado.

Si no hay datos para el período solicitado, o el formato del mes es inválido, el sistema responde con un
mensaje de error en español y no adjunta ningún archivo.

## Aislamiento por proveedor

Un usuario con rol PROVEEDOR nunca puede indicar `id_proveedor` como parámetro en los comandos
restringidos a su propio ámbito (ofertas, solicitudes, compras, pagos, reportes de compras): el
proveedor se toma siempre del contexto autenticado del remitente, nunca de un parámetro del correo. La
separación se aplica en tres capas:

1. **Comando**: el catálogo (`ComandoRegistry`) marca cada comando restringido (`scopedPorProveedor`);
   el `CommandProcessor` propaga el `proveedorId` del contexto autenticado a la capa de servicio.
2. **Servicio**: los listados reciben el `proveedorId` y filtran (por ejemplo `LIS_COMPRA`, `LIS_PAGO`,
   `LIS_OFERTA`, `LIS_SOLICITUD`); el acceso a un recurso puntual por id (`GET_COMPRA`,
   `VERIFICAR_PAGO`, `ATENDER_SOLICITUD`, etc.) verifica ownership y rechaza el acceso si el recurso
   pertenece a otro proveedor.
3. **Repositorio**: las consultas usadas en las ramas de rol PROVEEDOR reciben el `proveedorId`
   explícito en la firma del método (por ejemplo `findByProveedorId`, `findByCompraProveedorId`), nunca
   un `findAll()` sin filtrar.

Ver la matriz completa de comandos por rol en [docs/matriz-roles.md](docs/matriz-roles.md).
