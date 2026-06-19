# Comandos del sistema inventario

## Formato

- Enviar el comando en el asunto del correo.
- Formato: COMANDO["param1","param2",...]
- Los datos se guardan exactamente como se escriben (acepta tildes, caracteres especiales, etc.)

## Auto-registro (cualquier persona)

Formato
```
REGISTRO_CLIENTE["nombre","apellido","contrasena","telefono"]
```
Ejemplo
```
REGISTRO_CLIENTE["Maria","Lopez","MiClave123","75554321"]
```

## Ayuda

Formato
```
HELP[""]
```
Ejemplo
```
HELP["*"]
```

## Usuarios (admin)

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

## Categorias (admin)

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

## Productos (admin)

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

## Pedidos (cliente)

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

Formato
```
COMPROBAR_PAGO["id_pedido","numero_cuenta_destinatario","monto_cancelado"]
```
Ejemplo
```
COMPROBAR_PAGO["1","12345678","150.0"]
```

## Compras (admin)

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

## Proveedores (admin)

Formato
```
LIS_PROVEEDOR["*"]
```
Ejemplo
```
LIS_PROVEEDOR["*"]
```

Formato
```
GET_PROVEEDOR["id"]
```
Ejemplo
```
GET_PROVEEDOR["3"]
```

Formato
```
INS_PROVEEDOR["nombre","telefono","correo","direccion"]
```
Ejemplo
```
INS_PROVEEDOR["Distribuidora ABC","70123456","proveedor@abc.com","Av. Principal 123"]
```

Formato
```
UPD_PROVEEDOR["id","nombre","telefono","correo","direccion"]
```
Ejemplo
```
UPD_PROVEEDOR["3","Distribuidora ABC","70123456","nuevo@abc.com","Av. Beni 456"]
```

Formato
```
DEL_PROVEEDOR["id"]
```
Ejemplo
```
DEL_PROVEEDOR["3"]
```

## Proveedores (proveedor)

Formato
```
VER_MIS_ORDENES["*"]
```
Ejemplo
```
VER_MIS_ORDENES["*"]
```

Formato
```
CONFIRMAR_ENTREGA["id_compra"]
```
Ejemplo
```
CONFIRMAR_ENTREGA["5"]
```

## Reportes (admin)

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

## Almacenes (admin)

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

## Traslados (admin)

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
