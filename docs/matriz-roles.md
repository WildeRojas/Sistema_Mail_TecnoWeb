# Matriz de comandos por rol

Derivada directamente de `src/main/java/com/grupo06sa/sistema_inventario/processor/ComandoRegistry.java`
(fuente de verdad). Notación: **P** = PROPIETARIO, **T** = TRABAJADOR, **V** = PROVEEDOR. La columna
"Aislamiento por proveedor_id" indica si el comando está marcado `scopedPorProveedor` en el registro: en
ese caso, un remitente PROVEEDOR nunca puede pasar `id_proveedor` como parámetro, y el sistema resuelve
el alcance a partir de su propio contexto autenticado (ver reglas al final de este documento).

## Usuarios

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_USUARIO[""]` | 0 | Sí | — | — | No aplica |
| `GET_USUARIO["id"]` | 1 | Sí | — | — | No aplica |
| `INS_USUARIO[...]` | 6–7 | Sí | — | — | No aplica |
| `UPD_USUARIO[...]` | 7–8 | Sí | — | — | No aplica |
| `DEL_USUARIO["id"]` | 1 | Sí | — | — | No aplica |

## Proveedores

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_PROVEEDOR[""]` | 0 | Sí | Sí | — | No aplica |
| `GET_PROVEEDOR["id"]` | 1 | Sí | Sí | — | No aplica |
| `INS_PROVEEDOR[...]` | 4–5 | Sí | — | — | No aplica |
| `UPD_PROVEEDOR[...]` | 5–6 | Sí | — | — | No aplica |
| `DEL_PROVEEDOR["id"]` | 1 | Sí | — | — | No aplica |

## Categorías

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_CATEGORIA[""]` | 0 | Sí | Sí | — | No aplica |
| `GET_CATEGORIA["id"]` | 1 | Sí | Sí | — | No aplica |
| `INS_CATEGORIA[...]` | 1–2 | Sí | — | — | No aplica |
| `UPD_CATEGORIA[...]` | 2–3 | Sí | — | — | No aplica |
| `DEL_CATEGORIA["id"]` | 1 | Sí | — | — | No aplica |

## Productos (catálogo interno)

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_PRODUCTO[""]` | 0 | Sí | Sí | Sí | Sí — V solo ve productos con oferta propia |
| `GET_PRODUCTO["id"]` | 1 | Sí | Sí | Sí | Sí — V solo si tiene oferta sobre ese producto |
| `INS_PRODUCTO[...]` | 5–6 | Sí | — | — | No aplica |
| `UPD_PRODUCTO[...]` | 6–7 | Sí | — | — | No aplica |
| `DEL_PRODUCTO["id"]` | 1 | Sí | — | — | No aplica |

## Almacenes

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_ALMACEN[""]` | 0 | Sí | Sí | — | No aplica |
| `GET_ALMACEN["id"]` | 1 | Sí | Sí | — | No aplica |
| `INS_ALMACEN[...]` | 4 | Sí | — | — | No aplica |
| `UPD_ALMACEN[...]` | 5 | Sí | — | — | No aplica |
| `DEL_ALMACEN["id"]` | 1 | Sí | — | — | No aplica |

## Inventario y movimientos

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_INVENTARIO[""]` | 0 | Sí | Sí | — | No aplica |
| `INS_MOVIMIENTO[...]` | 4–5 | Sí | Sí | — | No aplica |
| `LIS_MOVIMIENTO[""]` | 0 | Sí | Sí | — | No aplica |
| `TRASLADO_STOCK[...]` | 4 | Sí | Sí | — | No aplica |

## Alertas de stock

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_ALERTA[""]` | 0 | Sí | Sí | — | No aplica |
| `ATENDER_ALERTA["id_alerta"]` | 1 | Sí | Sí | — | No aplica |

## Ofertas de proveedor

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `LIS_OFERTA[""]` | 0 | — | — | Sí | Sí — solo ofertas propias |
| `INS_OFERTA[...]` | 2–4 | — | — | Sí | Sí — la oferta se crea para el proveedor del remitente |
| `UPD_OFERTA[...]` | 2–5 | — | — | Sí | Sí — ownership sobre la oferta |
| `DEL_OFERTA["id_oferta"]` | 1 | — | — | Sí | Sí — ownership sobre la oferta |

## Comparación de ofertas

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `COMPARAR_OFERTAS["id_producto"]` | 1 | Sí | — | — | No aplica |

## Solicitudes de compra

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `INS_SOLICITUD[...]` | 4 | Sí | — | — | No aplica |
| `LIS_SOLICITUD[""]` | 0 | Sí | — | Sí | Sí — V solo ve las suyas |
| `ATENDER_SOLICITUD[...]` | 2 | — | — | Sí | Sí — ownership sobre la solicitud |
| `RECHAZAR_SOLICITUD[...]` | 2 | — | — | Sí | Sí — ownership sobre la solicitud |

## Compras

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `INS_COMPRA[...]` | 2 | Sí | — | — | No aplica |
| `INS_DETALLE_COMPRA[...]` | 3 | Sí | — | — | No aplica |
| `FINALIZAR_COMPRA["id_compra"]` | 1 | Sí | — | — | No aplica |
| `RECIBIR_COMPRA["id_compra"]` | 1 | Sí | — | — | No aplica |
| `LIS_COMPRA[""]` | 0 | Sí | — | Sí | Sí — V solo ve las suyas |
| `GET_COMPRA["id"]` | 1 | Sí | — | Sí | Sí — ownership sobre la compra |

## Pagos (empresa → proveedor)

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `PAGAR_COMPRA["id_compra"]` | 1 | Sí | — | — | No aplica |
| `VERIFICAR_PAGO["id_compra"]` | 1 | Sí | — | Sí | Sí — ownership sobre la compra |
| `LIS_PAGO[""]` | 0 | Sí | — | Sí | Sí — V solo ve pagos de sus propias compras |

## Reportes

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `REP_INVENTARIO[""]` | 0 | Sí | Sí | — | No aplica |
| `REP_COMPRAS["YYYY-MM"]` | 1 | Sí | — | Sí | Sí — V recibe el reporte filtrado a sus compras |

## Ayuda

| Comando | Aridad | P | T | V | Aislamiento por proveedor_id |
|---|---|---|---|---|---|
| `HELP` | 0 | Sí | Sí | Sí | No aplica |

`HELP` está disponible para cualquier usuario registrado, sin importar su rol; el contenido devuelto se
segmenta según los comandos autorizados para ese rol.

## Reglas de aislamiento por proveedor_id

El aislamiento se aplica en tres capas independientes, todas necesarias:

1. **Comando** (`processor/ComandoRegistry` + `processor/CommandProcessor`): cada comando declara si
   está `scopedPorProveedor`. Cuando el remitente autenticado tiene rol PROVEEDOR y el comando está
   marcado así, el `proveedorId` de su propio contexto se propaga a la capa de servicio; un remitente
   PROVEEDOR nunca puede indicar `id_proveedor` como parámetro del correo para suplantar a otro
   proveedor.
2. **Servicio** (`service/*Service`): los métodos de listado reciben el `proveedorId` del contexto y
   filtran sobre él (por ejemplo `listar(ContextoAutenticado ctx)` en `CompraService`,
   `SolicitudCompraService`, `PagoService`, `OfertaService`). El acceso a un recurso puntual por id pasa
   por `SecurityService.assertOwnership(proveedorIdDelRecurso, ctx)`, que lanza
   `RoleAccessDeniedException` si el rol es PROVEEDOR y el recurso pertenece a otro proveedor.
3. **Repositorio** (`repository/*Repository`): las consultas usadas en las ramas de rol PROVEEDOR
   reciben siempre el `proveedorId` explícito en la firma del método (`findByProveedorId`,
   `findByIdAndProveedorId`, `findByCompraProveedorId`, `existsByProveedorIdAndProductoId`, etc.); nunca
   se usa un `findAll()` sin filtrar en esas ramas.

`SecurityService.autenticar(email)` resuelve el `ContextoAutenticado` (usuario, rol y `proveedorId` si
corresponde) a partir del email del remitente. Si el email no existe o la cuenta está inactiva, lanza
siempre el mismo mensaje genérico, sin distinguir entre ambos casos, para no revelar cuál de las dos
condiciones se cumplió.
