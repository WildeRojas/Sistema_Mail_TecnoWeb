# Flujo del sistema inventario

## Formato de comandos

- Los comandos se envian en el asunto del correo.
- Formato: COMANDO["param1","param2",...]
- Los parametros siempre van entre comillas dobles.
- Los datos se guardan exactamente como se escriben (acepta caracteres especiales, tildes, etc.).

## Registro de cliente nuevo (cualquier persona)

- Cualquier persona que no este registrada puede auto-registrarse como CLIENTE.
- Enviar: `REGISTRO_CLIENTE["nombre","apellido","contrasena","telefono"]`
- El email del remitente se toma automaticamente como email de la cuenta.
- Si ya tiene cuenta con ese email, el sistema lo informa.
- Si el usuario envia cualquier otro comando sin estar registrado, recibe un correo automatico explicandole como registrarse con `REGISTRO_CLIENTE`.

## Flujo de venta con Pago Facil (cliente)

1) Crear pedido
- Enviar: `INS_PEDIDO["direccion"]`
- Estado inicial: PENDIENTE

2) Agregar detalles
- Enviar: `INS_DETALLE["id_pedido","id_producto","cantidad"]`
- Se acumula el total del pedido

3) Solicitar pago (QR)
- Enviar: `PAGAR_PEDIDO["id_pedido","metodo_pago"]`
- El sistema genera el QR con Pago Facil, cambia el pedido a ESPERANDO_PAGO
- Se responde con un correo que incluye la imagen del QR desencriptada en Base64

4) Confirmacion de pago (webhook)
- Pago Facil envia un POST a `/api/pagofacil/webhook`
- Si el pago es exitoso:
  - El pedido pasa a PAGADO
  - Se descuenta stock y se registra movimiento SALIDA
  - Se envia el comprobante de pago/factura al cliente por correo

5) Comprobacion de pago manual (comando)
- Enviar: `COMPROBAR_PAGO["id_pedido","numero_cuenta_destinatario","monto_cancelado"]`
- Valida que el pedido pertenezca al remitente, este en ESPERANDO_PAGO y el monto coincida
- Si es exitoso:
  - Registra el pago en el sistema
  - El pedido pasa a PAGADO
  - Se descuenta el stock correspondiente y se registra movimiento SALIDA
  - Se envia el comprobante de pago al cliente por correo

## Flujo de compra e interacción con Proveedores (admin y proveedor)

1) Crear compra (admin)
- Enviar: `INS_COMPRA["id_proveedor"]`
- Estado inicial: PENDIENTE

2) Agregar detalles (admin)
- Enviar: `INS_DETALLE_COMPRA["id_compra","id_producto","cantidad","precio_compra"]`
- No afecta el stock todavia

3) Finalizar compra (admin)
- Enviar: `FINALIZAR_COMPRA["id_compra"]`
- Calcula el total acumulado de la compra y cambia su estado a EN_ESPERA
- Envia automaticamente un correo al proveedor asociado con el detalle de la orden en HTML

4) Consultar ordenes pendientes (proveedor)
- Enviar: `VER_MIS_ORDENES["*"]` desde el correo registrado del proveedor
- El sistema responde con el listado en formato HTML de todas sus compras pendientes en estado EN_ESPERA

5) Confirmar entrega de productos (proveedor)
- Enviar: `CONFIRMAR_ENTREGA["id_compra"]` desde el correo registrado del proveedor
- Valida que la compra pertenezca al proveedor remitente y este en estado EN_ESPERA
- Si es valido:
  - Se suma el stock de cada producto y se registran los movimientos de INGRESO (ENTRADA) en el inventario
  - La compra cambia a estado ENTREGADO
  - Se envia una notificacion automatica por correo a todos los administradores

## Alertas proactivas de stock minimo

- Cuando el pago final descuenta stock y deja el valor <= stock_minimo,
  se envia un correo automatico al ADMINISTRADOR.

## Reportes PDF (admin)

- `REP_INVENTARIO["*"]`: productos, stock actual, stock minimo y categoria.
- `REP_VENTAS["YYYY-MM"]`: pedidos PAGADOS del mes y total de ingresos.

## Traslados internos (admin)

- `TRASLADO_STOCK["id_producto","id_almacen_origen","id_almacen_destino","cantidad"]`
- Valida almacenes distintos y cantidad > 0
- Registra movimiento TRASLADO

## Almacenes (admin)

- Crear: `INS_ALMACEN["nombre","capacidad","direccion","coordenadas_gps"]`
- Ver: `GET_ALMACEN["id"]`
- Listar: `LIS_ALMACEN["*"]`
- Actualizar: `UPD_ALMACEN["id","nombre","capacidad","direccion","coordenadas_gps"]`
- Eliminar: `DEL_ALMACEN["id"]`

## Gestion de Proveedores (admin)

- Listar: `LIS_PROVEEDOR["*"]`
- Ver detalle: `GET_PROVEEDOR["id"]`
- Registrar: `INS_PROVEEDOR["nombre","telefono","correo","direccion"]`
- Actualizar: `UPD_PROVEEDOR["id","nombre","telefono","correo","direccion"]`
- Eliminar: `DEL_PROVEEDOR["id"]`

## Gestion de Usuarios (admin)

- Listar: `LIS_USUARIO["*"]`
- Ver detalle: `GET_USUARIO["id"]`
- Registrar: `INS_USUARIO["nombre","apellido","contrasena","telefono","email","foto","rol"]`
  - *Validación*: Si el parámetro `email` contiene algún espacio en blanco, el comando lanzará un error y detendrá la inserción.
- Actualizar: `UPD_USUARIO["id","nombre","apellido","contrasena","telefono","email","foto","rol"]`
- Eliminar: `DEL_USUARIO["id"]`

Nota: Los comandos `LIS_USUARIO` y `GET_USUARIO` están ocultos en el menú de `HELP` de los clientes por motivos de seguridad, siendo visibles únicamente para el rol `ADMINISTRADOR`.

Nota: Los datos se insertan exactamente como se escriben. Acepta caracteres especiales, tildes y símbolos.

