# Comandos del Sistema Mail — Tiendas Junior

Catálogo completo de comandos, tomado de `processor/ComandoRegistry.java` (fuente de verdad). Cada
comando se envía en el **asunto** del correo con la forma:

```
COMANDO["parametro1","parametro2",...]
```

Notación de rol usada en este documento: **P** = PROPIETARIO, **T** = TRABAJADOR, **V** = PROVEEDOR.
Un parámetro marcado con `?` es opcional (se puede omitir el parámetro completo al final de la lista).

## Reglas de sintaxis y normalización

Antes de intentar reconocer el comando, el asunto se normaliza (`util/CommandParser`):

- Se decodifican encoded-words MIME (Base64 y Quoted-Printable; en la rama `Q`, `_` equivale a espacio y
  `=XX` a un byte codificado).
- Se normaliza a Unicode NFC.
- Se retiran, de forma iterativa y sin distinguir mayúsculas/minúsculas, los prefijos de
  respuesta/reenvío `RE:`, `RV:`, `FW:` y `FWD:` (con o sin espacio después de los dos puntos). Por
  ejemplo, `Re: FW: LIS_PROVEEDOR[""]` se reconoce igual que `LIS_PROVEEDOR[""]`.
- Se colapsan los espacios repetidos fuera de las comillas de los parámetros; el contenido de cada
  parámetro se preserva exactamente como se escribió (tildes, mayúsculas/minúsculas, símbolos).
- El **nombre** del comando no distingue mayúsculas/minúsculas (`lis_proveedor` se reconoce igual que
  `LIS_PROVEEDOR`), pero se normaliza siempre a mayúsculas para el match contra el catálogo.

Reglas del formato `COMANDO["p1","p2",...]`:

- Cada parámetro va entre comillas dobles. Dentro de un parámetro se puede escapar una comilla o una
  barra invertida con `\"` / `\\`.
- Los parámetros se separan con coma.
- Comandos sin parámetros aceptan varias formas equivalentes: `HELP`, `HELP[]`, `HELP[""]`, `HELP()`,
  `HELP( )` (con espacios adicionales tolerados). Un parámetro no vacío enviado a un comando que no
  admite parámetros produce el error "parámetro no permitido".
- El parser detecta y reporta en español, sin caerse nunca: parámetros faltantes o de más, comillas sin
  cerrar, corchetes o paréntesis incompletos, separadores incorrectos (algo distinto de una coma), y
  asuntos que exceden la longitud máxima permitida (2000 caracteres).

## HELP

`HELP` no admite parámetros. Variantes aceptadas: `HELP`, `HELP[""]`, `HELP[]`, `HELP()`.

Devuelve la lista de comandos autorizados para el rol del remitente. Si el remitente no está registrado
en el sistema (o su cuenta está inactiva), responde con una plantilla indicando que debe contactar al
propietario del negocio para que lo registre — el mismo mensaje que produce cualquier otro comando
enviado por un remitente no registrado.

```
HELP
```

## Comando desconocido y parámetros inválidos

- **Comando no reconocido**: si el remitente sí está registrado, el sistema calcula hasta 3 sugerencias
  por distancia de Levenshtein, limitadas a los comandos autorizados para su propio rol, y las muestra
  como sintaxis de uso (por ejemplo `LIS_PRODUCTO[""]`). Si ninguna alternativa se parece
  razonablemente al nombre recibido, no sugiere nada. Si el remitente no está registrado, se le indica
  únicamente cómo contactar al propietario, sin exponer nombres de comandos internos.
- **Privilegios insuficientes**: si el comando existe pero el rol del remitente no está autorizado, se
  responde que no tiene autorización para ejecutarlo con su rol actual.
- **Aridad inválida**: si la cantidad de parámetros no está entre el mínimo y el máximo declarados para
  el comando, se informa cuántos parámetros espera (o que no admite ninguno) junto con la sintaxis de
  uso exacta.
- Un único parámetro vacío (por ejemplo `LIS_PRODUCTO[""]`) cuenta como "sin parámetros" a efectos de
  esta validación.

## Usuarios (P)

```
LIS_USUARIO[""]
GET_USUARIO["id"]
INS_USUARIO["nombre","apellido","contrasena","telefono","email","rol","id_proveedor?"]
UPD_USUARIO["id","nombre","apellido","contrasena","telefono","email","rol","id_proveedor?"]
DEL_USUARIO["id"]
```

`rol` acepta `PROPIETARIO`, `TRABAJADOR` o `PROVEEDOR`. Si `rol` es `PROVEEDOR`, `id_proveedor` es
obligatorio (se valida en el servicio, no en la aridad del comando). El `email` no puede contener
espacios en blanco.

Ejemplos:
```
LIS_USUARIO[""]
GET_USUARIO["3"]
INS_USUARIO["Ana","Fernandez","Clave2026","70011122","ana.fernandez@tiendasjunior.local","TRABAJADOR"]
INS_USUARIO["Sebastian","Mendez","Clave2026","70000002","sebamendex11@gmal.com","PROVEEDOR","2"]
UPD_USUARIO["3","Ana","Fernandez","ClaveNueva2026","70011122","ana.fernandez@tiendasjunior.local","TRABAJADOR"]
DEL_USUARIO["3"]
```

## Proveedores

INS/UPD/DEL: **P**. LIS/GET: **P, T**.

```
LIS_PROVEEDOR[""]
GET_PROVEEDOR["id"]
INS_PROVEEDOR["nombre","telefono","correo","direccion","nit?"]
UPD_PROVEEDOR["id","nombre","telefono","correo","direccion","nit?"]
DEL_PROVEEDOR["id"]
```

Ejemplos:
```
LIS_PROVEEDOR[""]
GET_PROVEEDOR["1"]
INS_PROVEEDOR["Distribuidora Samsonite","70000001","ventas@samsonite.demo","Av. Principal 123","1023456011"]
UPD_PROVEEDOR["1","Distribuidora Samsonite","70000009","ventas@samsonite.demo","Av. Principal 123","1023456011"]
DEL_PROVEEDOR["1"]
```

## Categorías

INS/UPD/DEL: **P**. LIS/GET: **P, T**.

```
LIS_CATEGORIA[""]
GET_CATEGORIA["id"]
INS_CATEGORIA["nombre","imagen?"]
UPD_CATEGORIA["id","nombre","imagen?"]
DEL_CATEGORIA["id"]
```

Ejemplos:
```
LIS_CATEGORIA[""]
GET_CATEGORIA["2"]
INS_CATEGORIA["Mochilas","https://tiendasjunior.local/categorias/mochilas.jpg"]
UPD_CATEGORIA["2","Mochilas Escolares","https://tiendasjunior.local/categorias/mochilas.jpg"]
DEL_CATEGORIA["2"]
```

## Productos

Catálogo interno de la empresa (no tienen precio de venta ni proveedor fijo; la relación con
proveedores se hace vía ofertas). INS/UPD/DEL: **P**. LIS/GET: **P, T, V**; para el rol PROVEEDOR ambos
comandos quedan restringidos a los productos sobre los que tiene una oferta registrada (`GET_PRODUCTO`
de un producto sin oferta propia se rechaza por falta de acceso).

```
LIS_PRODUCTO[""]
GET_PRODUCTO["id"]
INS_PRODUCTO["codigo","nombre","costo_unitario","stock_minimo","id_categoria","imagen?"]
UPD_PRODUCTO["id","codigo","nombre","costo_unitario","stock_minimo","id_categoria","imagen?"]
DEL_PRODUCTO["id"]
```

Ejemplos:
```
LIS_PRODUCTO[""]
GET_PRODUCTO["10"]
INS_PRODUCTO["MCH-101","Mochila Escolar","200.00","10","1"]
UPD_PRODUCTO["10","MCH-101","Mochila Escolar Reforzada","210.00","10","1"]
DEL_PRODUCTO["10"]
```

## Almacenes

INS/UPD/DEL: **P**. LIS/GET: **P, T**.

```
LIS_ALMACEN[""]
GET_ALMACEN["id"]
INS_ALMACEN["nombre","capacidad","direccion","coordenadas_gps"]
UPD_ALMACEN["id","nombre","capacidad","direccion","coordenadas_gps"]
DEL_ALMACEN["id"]
```

`capacidad` menor o igual a 0 se interpreta como "sin límite". `GET_ALMACEN` muestra también la
ocupación actual. `DEL_ALMACEN` se rechaza si el almacén todavía tiene inventario asociado.

Ejemplos:
```
LIS_ALMACEN[""]
GET_ALMACEN["1"]
INS_ALMACEN["Sucursal Norte","300","Av. Beni 123","-17.78,-63.18"]
UPD_ALMACEN["1","Sucursal Norte","350","Av. Beni 123","-17.78,-63.18"]
DEL_ALMACEN["2"]
```

## Inventario y movimientos

**P, T** para todos los comandos de este módulo.

```
LIS_INVENTARIO[""]
INS_MOVIMIENTO["tipo(INGRESO|SALIDA|AJUSTE)","id_producto","id_almacen","cantidad","observacion?"]
LIS_MOVIMIENTO[""]
TRASLADO_STOCK["id_producto","id_almacen_origen","id_almacen_destino","cantidad"]
```

`LIS_INVENTARIO` lista el stock por producto y almacén. `INS_MOVIMIENTO` valida que el stock resultante
no quede negativo (para SALIDA/AJUSTE) y que no se exceda la capacidad del almacén (para
INGRESO/AJUSTE positivo). `TRASLADO_STOCK` exige almacenes de origen y destino distintos, cantidad
mayor a 0 y stock suficiente en el origen; mueve stock real entre las filas de inventario y registra un
movimiento de tipo TRASLADO.

Ejemplos:
```
LIS_INVENTARIO[""]
INS_MOVIMIENTO["AJUSTE","10","1","-2","Conteo físico mensual"]
LIS_MOVIMIENTO[""]
TRASLADO_STOCK["10","1","2","5"]
```

## Alertas de stock

**P, T**.

```
LIS_ALERTA[""]
ATENDER_ALERTA["id_alerta"]
```

Las alertas se generan automáticamente cuando una salida, ajuste negativo o traslado deja el stock
total de un producto (sumado en todos los almacenes) por debajo o igual a su stock mínimo. No se crea
una alerta nueva si ya existe una pendiente para el mismo producto.

Ejemplos:
```
LIS_ALERTA[""]
ATENDER_ALERTA["4"]
```

## Ofertas de proveedor (V)

Comandos exclusivos del rol PROVEEDOR, con ownership estricto: cada proveedor solo ve y modifica sus
propias ofertas.

```
LIS_OFERTA[""]
INS_OFERTA["id_producto","costo_unitario","tiempo_reposicion_dias?","cantidad_minima?"]
UPD_OFERTA["id_oferta","costo_unitario","tiempo_reposicion_dias?","cantidad_minima?","disponible(SI|NO)?"]
DEL_OFERTA["id_oferta"]
```

`INS_OFERTA` rechaza una segunda oferta del mismo proveedor sobre el mismo producto. `disponible`
acepta `SI` o `NO`.

Ejemplos (enviados desde el correo registrado del proveedor):
```
LIS_OFERTA[""]
INS_OFERTA["10","820.00","8","1"]
UPD_OFERTA["7","830.00","8","1","SI"]
DEL_OFERTA["7"]
```

## Comparación de ofertas (P)

```
COMPARAR_OFERTAS["id_producto"]
```

Lista todas las ofertas registradas para un producto, ordenadas por costo, marcando la mejor oferta
disponible.

Ejemplo:
```
COMPARAR_OFERTAS["10"]
```

## Solicitudes de compra

```
INS_SOLICITUD["id_proveedor","id_producto","cantidad","id_almacen"]
LIS_SOLICITUD[""]
ATENDER_SOLICITUD["id_solicitud","costo_ofrecido"]
RECHAZAR_SOLICITUD["id_solicitud","motivo"]
```

- `INS_SOLICITUD` — **P**. Requiere que el proveedor tenga una oferta disponible para ese producto;
  notifica al proveedor por correo.
- `LIS_SOLICITUD` — **P, V** (el mismo comando; PROPIETARIO ve todas, PROVEEDOR solo las dirigidas a él).
- `ATENDER_SOLICITUD` / `RECHAZAR_SOLICITUD` — **V**, con ownership (solo sobre solicitudes dirigidas al
  proveedor del remitente).

Ejemplos:
```
INS_SOLICITUD["1","10","10","1"]
LIS_SOLICITUD[""]
ATENDER_SOLICITUD["5","8500.00"]
RECHAZAR_SOLICITUD["6","Sin stock disponible del proveedor por el momento"]
```

## Compras

```
INS_COMPRA["id_proveedor","id_almacen"]
INS_DETALLE_COMPRA["id_compra","id_oferta","cantidad"]
FINALIZAR_COMPRA["id_compra"]
RECIBIR_COMPRA["id_compra"]
LIS_COMPRA[""]
GET_COMPRA["id"]
```

- `INS_COMPRA`, `INS_DETALLE_COMPRA`, `FINALIZAR_COMPRA`, `RECIBIR_COMPRA` — **P**.
- `LIS_COMPRA` / `GET_COMPRA` — **P, V** (PROPIETARIO ve todas; PROVEEDOR solo las propias, con
  ownership en `GET_COMPRA`).

`INS_DETALLE_COMPRA` toma el costo unitario como una fotografía (snapshot) inmutable de la oferta en el
momento de agregarlo; la oferta debe pertenecer al proveedor de la compra y estar disponible.
`FINALIZAR_COMPRA` calcula el total y envía la orden de compra al proveedor por correo.
`RECIBIR_COMPRA` es idempotente: si el inventario de esa compra ya fue ingresado, repetir el comando no
duplica el stock.

Ejemplos:
```
INS_COMPRA["2","1"]
INS_DETALLE_COMPRA["5","7","5"]
FINALIZAR_COMPRA["5"]
RECIBIR_COMPRA["5"]
LIS_COMPRA[""]
GET_COMPRA["5"]
```

## Pagos (empresa paga al proveedor)

```
PAGAR_COMPRA["id_compra"]
VERIFICAR_PAGO["id_compra"]
LIS_PAGO[""]
```

- `PAGAR_COMPRA` — **P**. Genera un QR de Pago Fácil por el saldo pendiente de la compra y responde con
  la imagen del QR adjunta.
- `VERIFICAR_PAGO` — **P, V** (con ownership para PROVEEDOR). Consulta activamente el estado de la
  transacción ante Pago Fácil.
- `LIS_PAGO` — **P, V**: PROPIETARIO ve todos los pagos; PROVEEDOR solo los de sus propias compras.

Ejemplos:
```
PAGAR_COMPRA["5"]
VERIFICAR_PAGO["5"]
LIS_PAGO[""]
```

## Reportes (PDF)

```
REP_INVENTARIO[""]
REP_COMPRAS["YYYY-MM"]
```

- `REP_INVENTARIO` — **P, T**. Adjunta un PDF con productos, stock por almacén, stock mínimo y
  categoría.
- `REP_COMPRAS` — **P, V**. Adjunta un PDF con las compras del mes indicado; para el rol PROVEEDOR, el
  reporte queda filtrado a sus propias compras.

Ejemplos:
```
REP_INVENTARIO[""]
REP_COMPRAS["2026-07"]
```

Si no hay datos para el reporte solicitado, o el mes no tiene el formato `YYYY-MM`, el sistema responde
con un mensaje de error en español sin adjuntar nada.
