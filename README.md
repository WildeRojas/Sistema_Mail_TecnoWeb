# Sistema Inventario (SMTP)

## Formato de comandos

- Los comandos se envian en el asunto del correo.
- Formato: COMANDO["param1","param2",...]
- Los parametros siempre van entre comillas dobles.
- El comando debe ir en MAYUSCULAS y con guiones bajos si aplica.

Ejemplo:

```
INS_PEDIDO["Av. Busch 123"]
```

El buzon que procesa los comandos y el servidor POP3 se configuran en [src/main/resources/application.properties](src/main/resources/application.properties).

## Flujo completo: pedido -> detalle -> pago (cliente)

1) Cliente crea el pedido
- El cliente envia un correo con el asunto:
  `INS_PEDIDO["direccion"]`.
- El sistema lee el inbox POP3, valida el formato del asunto y crea el pedido con:
  - Estado = PENDIENTE
  - Total = 0.0
  - Fecha = ahora
  - Usuario = el remitente
- Se responde por correo con el ID del pedido y su estado.

2) Cliente agrega cantidades (detalles)
- Por cada producto, el cliente envia:
  `INS_DETALLE["id_pedido","id_producto","cantidad"]`.
- El sistema valida:
  - Que el pedido exista y pertenezca al cliente.
  - Que el pedido este en estado PENDIENTE.
  - Que el producto exista y tenga precio.
  - Que la cantidad sea positiva.
- Se crea el detalle y se actualiza el total del pedido.
- Se responde por correo con el detalle agregado y el nuevo total.

3) Cliente paga el pedido
- El cliente envia:
  `PAGAR_PEDIDO["id_pedido","metodo_pago"]`.
- Metodos validos: `QR` o `TARJETA`.
- El sistema valida:
  - Pedido del cliente y en estado PENDIENTE.
  - Que el pedido tenga detalles.
  - Stock suficiente para cada producto.
- Acciones que ejecuta:
  - Calcula el total definitivo.
  - Descuenta stock de cada producto.
  - Registra movimientos de inventario (SALIDA).
  - Crea el pago con monto y metodo.
  - Cambia el estado del pedido a PAGADO.
- Se responde por correo con la factura del pedido.

## Flujo completo: compra -> detalle -> finalizar (admin)

1) Admin crea la compra
- El admin envia un correo con el asunto:
  `INS_COMPRA["id_proveedor"]`.
- El sistema crea la compra con:
  - Estado = PENDIENTE
  - Total = 0.0
  - Fecha = ahora
  - Usuario = el admin remitente
- Se responde por correo con el ID de la compra.

2) Admin agrega detalles de compra
- Por cada producto, el admin envia:
  `INS_DETALLE_COMPRA["id_compra","id_producto","cantidad","precio_compra"]`.
- El sistema valida:
  - Que la compra exista y este en estado PENDIENTE.
  - Que el producto exista.
  - Que cantidad y precio_compra sean validos.
- Se crea el detalle, sin afectar stock.

3) Admin finaliza la compra
- El admin envia:
  `FINALIZAR_COMPRA["id_compra"]`.
- El sistema valida que la compra siga en PENDIENTE y tenga detalles.
- Acciones que ejecuta:
  - Calcula el total final (cantidad * precio_compra).
  - Cambia estado a PAGADO.
  - Registra movimientos de inventario (ENTRADA).
  - Suma la cantidad al stock_actual de cada producto.
- Se responde por correo con el resumen y total de la compra.

## Alertas proactivas de stock minimo

- Cuando un pedido se paga y el stock_actual queda menor o igual al stock_minimo,
  el sistema envia un correo al ADMINISTRADOR con la alerta y el nombre del producto.

## Reportes PDF (admin)

- `REP_INVENTARIO["*"]`: adjunta un PDF con productos, stock actual, stock minimo y categoria.
- `REP_VENTAS["YYYY-MM"]`: adjunta un PDF con pedidos PAGADOS del mes y el total de ingresos.
- Si no hay datos o el formato es invalido, el sistema responde con un error amigable.

## Traslado interno de stock (admin)

1) Admin registra el traslado
- Envia: `TRASLADO_STOCK["id_producto","id_almacen_origen","id_almacen_destino","cantidad"]`.
- Valida almacenes distintos, existencia y cantidad > 0.
- Si hay stock suficiente, registra el movimiento de tipo TRASLADO.

## Permisos (resumen)

- ADMINISTRADOR: puede crear/editar/eliminar usuarios, categorias y productos.
- CLIENTE: puede listar/ver catalogos y crear/pagar pedidos.

## Lista de comandos (copiar/pegar) con ejemplos

Ayuda

Formato
```
HELP[""]
```
Ejemplo
```
HELP["*"]
```

Usuarios (ADMIN)

Formato
```
LIS_USUARIO["*"]
```
Ejemplo
```
LIS_USUARIO["*"]
```

Formato
```
GET_USUARIO["id"]
```
Ejemplo
```
GET_USUARIO["3"]
```

Formato
```
INS_USUARIO["nombre","apellido","contrasena","telefono","email","foto","rol"]
```
Ejemplo
```
INS_USUARIO["Juan","Perez","Secreta123","70123456","juan.perez@mail.com","https://img.com/juan.jpg","CLIENTE"]
```

Formato
```
UPD_USUARIO["id","nombre","apellido","contrasena","telefono","email","foto","rol"]
```
Ejemplo
```
UPD_USUARIO["5","Juan","Perez","Nueva123","70123456","juan.perez@mail.com","https://img.com/juan2.jpg","ADMINISTRADOR"]
```

Formato
```
DEL_USUARIO["id"]
```
Ejemplo
```
DEL_USUARIO["5"]
```

Categorias (ADMIN)

Formato
```
LIS_CATEGORIA["*"]
```
Ejemplo
```
LIS_CATEGORIA["*"]
```

Formato
```
GET_CATEGORIA["id"]
```
Ejemplo
```
GET_CATEGORIA["2"]
```

Formato
```
INS_CATEGORIA["nombre","imagen"]
```
Ejemplo
```
INS_CATEGORIA["Lacteos","https://img.com/lacteos.png"]
```

Formato
```
UPD_CATEGORIA["id","nombre","imagen"]
```
Ejemplo
```
UPD_CATEGORIA["2","Lacteos y Quesos","https://img.com/lacteos2.png"]
```

Formato
```
DEL_CATEGORIA["id"]
```
Ejemplo
```
DEL_CATEGORIA["2"]
```

Productos (ADMIN)

Formato
```
LIS_PRODUCTO["*"]
```
Ejemplo
```
LIS_PRODUCTO["*"]
```

Formato
```
GET_PRODUCTO["id"]
```
Ejemplo
```
GET_PRODUCTO["10"]
```

Formato
```
INS_PRODUCTO["codigo","nombre","precio","stockMinimo","categoriaId","proveedorId","imagen"]
```
Ejemplo
```
INS_PRODUCTO["P-100","Leche entera","7.5","10","2","4","https://img.com/leche.png"]
```

Formato
```
UPD_PRODUCTO["id","codigo","nombre","precio","stockMinimo","categoriaId","proveedorId","imagen"]
```
Ejemplo
```
UPD_PRODUCTO["10","P-100","Leche descremada","7.0","8","2","4","https://img.com/leche2.png"]
```

Formato
```
DEL_PRODUCTO["id"]
```
Ejemplo
```
DEL_PRODUCTO["10"]
```

Pedidos (CLIENTE)

Formato
```
INS_PEDIDO["direccion"]
```
Ejemplo
```
INS_PEDIDO["Av. Busch 123"]
```

Formato
```
INS_DETALLE["id_pedido","id_producto","cantidad"]
```
Ejemplo
```
INS_DETALLE["1","10","3"]
```

Formato
```
PAGAR_PEDIDO["id_pedido","metodo_pago"]
```
Ejemplo
```
PAGAR_PEDIDO["1","QR"]
```

Compras (ADMIN)

Formato
```
INS_COMPRA["id_proveedor"]
```
Ejemplo
```
INS_COMPRA["2"]
```

Formato
```
INS_DETALLE_COMPRA["id_compra","id_producto","cantidad","precio_compra"]
```
Ejemplo
```
INS_DETALLE_COMPRA["5","10","12","250.0"]
```

Formato
```
FINALIZAR_COMPRA["id_compra"]
```
Ejemplo
```
FINALIZAR_COMPRA["5"]
```

Reportes (ADMIN)

Formato
```
REP_INVENTARIO["*"]
```
Ejemplo
```
REP_INVENTARIO["*"]
```

Formato
```
REP_VENTAS["YYYY-MM"]
```
Ejemplo
```
REP_VENTAS["2026-05"]
```

Almacenes (ADMIN)

Formato
```
LIS_ALMACEN["*"]
```
Ejemplo
```
LIS_ALMACEN["*"]
```

Formato
```
GET_ALMACEN["id"]
```
Ejemplo
```
GET_ALMACEN["1"]
```

Formato
```
INS_ALMACEN["nombre","capacidad","direccion","coordenadas_gps"]
```
Ejemplo
```
INS_ALMACEN["Sucursal Norte","300","Av. Beni 123","-17.78,-63.18"]
```

Formato
```
UPD_ALMACEN["id","nombre","capacidad","direccion","coordenadas_gps"]
```
Ejemplo
```
UPD_ALMACEN["2","Sucursal Norte","350","Av. Beni 123","-17.78,-63.18"]
```

Formato
```
DEL_ALMACEN["id"]
```
Ejemplo
```
DEL_ALMACEN["2"]
```

Traslados (ADMIN)

Formato
```
TRASLADO_STOCK["id_producto","id_almacen_origen","id_almacen_destino","cantidad"]
```
Ejemplo
```
TRASLADO_STOCK["10","1","2","5"]
```

Notas:
- `imagen`, `foto` y `rol` son opcionales en INS/UPD segun el caso.
- `metodo_pago` solo acepta `QR` o `TARJETA`.
