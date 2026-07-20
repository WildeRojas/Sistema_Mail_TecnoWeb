package com.grupo06sa.sistema_inventario.seed;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.DetalleCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoSolicitud;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.Rol;
import com.grupo06sa.sistema_inventario.entity.SolicitudCompra;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.DetalleCompraRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.SolicitudCompraRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DatabaseSeeder implements CommandLineRunner {
    private final RolRepository rolRepository;
    private final TipoOperacionRepository tipoOperacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorProductoRepository ofertaRepository;
    private final InventarioRepository inventarioRepository;
    private final SolicitudCompraRepository solicitudRepository;
    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
        RolRepository rolRepository,
        TipoOperacionRepository tipoOperacionRepository,
        CategoriaRepository categoriaRepository,
        ProveedorRepository proveedorRepository,
        AlmacenRepository almacenRepository,
        UsuarioRepository usuarioRepository,
        ProductoRepository productoRepository,
        ProveedorProductoRepository ofertaRepository,
        InventarioRepository inventarioRepository,
        SolicitudCompraRepository solicitudRepository,
        CompraRepository compraRepository,
        DetalleCompraRepository detalleCompraRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.rolRepository = rolRepository;
        this.tipoOperacionRepository = tipoOperacionRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.almacenRepository = almacenRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.ofertaRepository = ofertaRepository;
        this.inventarioRepository = inventarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (rolRepository.count() > 0) {
            return;
        }

        Rol rolPropietario = new Rol();
        rolPropietario.setNombre(RolNombre.PROPIETARIO.name());
        Rol rolTrabajador = new Rol();
        rolTrabajador.setNombre(RolNombre.TRABAJADOR.name());
        Rol rolProveedor = new Rol();
        rolProveedor.setNombre(RolNombre.PROVEEDOR.name());
        rolRepository.saveAll(List.of(rolPropietario, rolTrabajador, rolProveedor));

        TipoOperacion ingreso = new TipoOperacion();
        ingreso.setNombre("INGRESO");
        TipoOperacion salida = new TipoOperacion();
        salida.setNombre("SALIDA");
        TipoOperacion traslado = new TipoOperacion();
        traslado.setNombre("TRASLADO");
        TipoOperacion ajuste = new TipoOperacion();
        ajuste.setNombre("AJUSTE");
        tipoOperacionRepository.saveAll(List.of(ingreso, salida, traslado, ajuste));

        Categoria mochilas = new Categoria();
        mochilas.setNombre("Mochilas");
        mochilas.setImagen("https://example.com/categorias/mochilas.jpg");
        Categoria maletas = new Categoria();
        maletas.setNombre("Maletas");
        maletas.setImagen("https://example.com/categorias/maletas.jpg");
        Categoria carteras = new Categoria();
        carteras.setNombre("Carteras");
        carteras.setImagen("https://example.com/categorias/carteras.jpg");
        categoriaRepository.saveAll(List.of(mochilas, maletas, carteras));

        Proveedor samsonite = new Proveedor();
        samsonite.setNombre("Distribuidora Samsonite");
        samsonite.setTelefono("70000001");
        samsonite.setCorreo("ventas@samsonite.demo");
        samsonite.setDireccion("Av. Principal 123");
        samsonite.setNit("1023456011");

        Proveedor importadora = new Proveedor();
        importadora.setNombre("Sebastian Import");
        importadora.setTelefono("70000002");
        importadora.setCorreo("sebamendex11@gmal.com");
        importadora.setDireccion("Calle Comercio 456");
        importadora.setNit("1023456022");
        proveedorRepository.saveAll(List.of(samsonite, importadora));

        Almacen central = new Almacen();
        central.setNombre("Almacen Central");
        central.setCapacidad(new BigDecimal("1000"));
        central.setCoordenadasGps("-17.7833,-63.1833");
        central.setDireccion("Zona Industrial");
        Almacen sucursal = new Almacen();
        sucursal.setNombre("Sucursal Centro");
        sucursal.setCapacidad(new BigDecimal("300"));
        sucursal.setCoordenadasGps("-17.7830,-63.1820");
        sucursal.setDireccion("Centro");
        almacenRepository.saveAll(List.of(central, sucursal));

        Usuario propietario = new Usuario();
        propietario.setNombre("Wilder Ario");
        propietario.setApellido("Rojas");
        propietario.setEmail("rojasjhunior025@gmail.com");
        propietario.setContrasena(passwordEncoder.encode("jhunior789"));
        propietario.setTelefono("78787845");
        propietario.setActivo(true);
        propietario.setRol(rolPropietario);

        Usuario trabajador = new Usuario();
        trabajador.setNombre("Jhunior");
        trabajador.setApellido("Rojas Mamani");
        trabajador.setEmail("wilderariorojas@gmail.com");
        trabajador.setContrasena(passwordEncoder.encode("trabajador123"));
        trabajador.setTelefono("70001002");
        trabajador.setActivo(true);
        trabajador.setRol(rolTrabajador);

        Usuario usuarioSamsonite = new Usuario();
        usuarioSamsonite.setNombre("Samsonite");
        usuarioSamsonite.setApellido("Ventas");
        usuarioSamsonite.setEmail(samsonite.getCorreo());
        usuarioSamsonite.setContrasena(passwordEncoder.encode("proveedor123"));
        usuarioSamsonite.setTelefono(samsonite.getTelefono());
        usuarioSamsonite.setActivo(true);
        usuarioSamsonite.setRol(rolProveedor);
        usuarioSamsonite.setProveedor(samsonite);

        Usuario usuarioImportadora = new Usuario();
        usuarioImportadora.setNombre("Sebastian");
        usuarioImportadora.setApellido("Mendez");
        usuarioImportadora.setEmail(importadora.getCorreo());
        usuarioImportadora.setContrasena(passwordEncoder.encode("proveedor123"));
        usuarioImportadora.setTelefono(importadora.getTelefono());
        usuarioImportadora.setActivo(true);
        usuarioImportadora.setRol(rolProveedor);
        usuarioImportadora.setProveedor(importadora);

        usuarioRepository.saveAll(List.of(propietario, trabajador, usuarioSamsonite, usuarioImportadora));

        Producto maletaCabina = new Producto();
        maletaCabina.setCodigo("MLT-001");
        maletaCabina.setNombre("Maleta de Cabina 10kg");
        maletaCabina.setCostoUnitario(new BigDecimal("800.00"));
        maletaCabina.setStockMinimo(new BigDecimal("5"));
        maletaCabina.setImagen("https://example.com/productos/maleta-cabina.jpg");
        maletaCabina.setCategoria(maletas);

        Producto mochilaEscolar = new Producto();
        mochilaEscolar.setCodigo("MCH-101");
        mochilaEscolar.setNombre("Mochila Escolar");
        mochilaEscolar.setCostoUnitario(new BigDecimal("200.00"));
        mochilaEscolar.setStockMinimo(new BigDecimal("10"));
        mochilaEscolar.setImagen("https://example.com/productos/mochila-escolar.jpg");
        mochilaEscolar.setCategoria(mochilas);

        Producto carteraCuero = new Producto();
        carteraCuero.setCodigo("CRT-210");
        carteraCuero.setNombre("Cartera de Cuero");
        carteraCuero.setCostoUnitario(new BigDecimal("400.00"));
        carteraCuero.setStockMinimo(new BigDecimal("6"));
        carteraCuero.setImagen("https://example.com/productos/cartera-cuero.jpg");
        carteraCuero.setCategoria(carteras);

        productoRepository.saveAll(List.of(maletaCabina, mochilaEscolar, carteraCuero));

        ProveedorProducto ofertaMaletaSamsonite = new ProveedorProducto();
        ofertaMaletaSamsonite.setProveedor(samsonite);
        ofertaMaletaSamsonite.setProducto(maletaCabina);
        ofertaMaletaSamsonite.setCostoUnitarioActual(new BigDecimal("850.00"));
        ofertaMaletaSamsonite.setTiempoReposicionDias(5);
        ofertaMaletaSamsonite.setCantidadMinimaPedido(BigDecimal.ONE);
        ofertaMaletaSamsonite.setDisponible(true);
        ofertaMaletaSamsonite.setCostoActualizadoAt(LocalDateTime.now());

        ProveedorProducto ofertaMaletaImportadora = new ProveedorProducto();
        ofertaMaletaImportadora.setProveedor(importadora);
        ofertaMaletaImportadora.setProducto(maletaCabina);
        ofertaMaletaImportadora.setCostoUnitarioActual(new BigDecimal("820.00"));
        ofertaMaletaImportadora.setTiempoReposicionDias(8);
        ofertaMaletaImportadora.setCantidadMinimaPedido(BigDecimal.ONE);
        ofertaMaletaImportadora.setDisponible(true);
        ofertaMaletaImportadora.setCostoActualizadoAt(LocalDateTime.now());

        ProveedorProducto ofertaMochilaSamsonite = new ProveedorProducto();
        ofertaMochilaSamsonite.setProveedor(samsonite);
        ofertaMochilaSamsonite.setProducto(mochilaEscolar);
        ofertaMochilaSamsonite.setCostoUnitarioActual(new BigDecimal("220.00"));
        ofertaMochilaSamsonite.setTiempoReposicionDias(3);
        ofertaMochilaSamsonite.setCantidadMinimaPedido(new BigDecimal("5"));
        ofertaMochilaSamsonite.setDisponible(true);
        ofertaMochilaSamsonite.setCostoActualizadoAt(LocalDateTime.now());

        ProveedorProducto ofertaCarteraImportadora = new ProveedorProducto();
        ofertaCarteraImportadora.setProveedor(importadora);
        ofertaCarteraImportadora.setProducto(carteraCuero);
        ofertaCarteraImportadora.setCostoUnitarioActual(new BigDecimal("430.00"));
        ofertaCarteraImportadora.setTiempoReposicionDias(10);
        ofertaCarteraImportadora.setCantidadMinimaPedido(new BigDecimal("2"));
        ofertaCarteraImportadora.setDisponible(true);
        ofertaCarteraImportadora.setCostoActualizadoAt(LocalDateTime.now());

        ofertaRepository.saveAll(List.of(
            ofertaMaletaSamsonite, ofertaMaletaImportadora, ofertaMochilaSamsonite, ofertaCarteraImportadora
        ));

        guardarInventario(maletaCabina, central, new BigDecimal("15"));
        guardarInventario(mochilaEscolar, central, new BigDecimal("40"));
        guardarInventario(mochilaEscolar, sucursal, new BigDecimal("10"));
        guardarInventario(carteraCuero, central, new BigDecimal("20"));

        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setProveedor(samsonite);
        solicitud.setProducto(maletaCabina);
        solicitud.setAlmacen(central);
        solicitud.setCantidad(new BigDecimal("10"));
        solicitud.setCostoEstimado(new BigDecimal("8500.00"));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setSolicitante(propietario);
        solicitud.setCreatedAt(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        Compra compra = new Compra();
        compra.setProveedor(importadora);
        compra.setAlmacen(central);
        compra.setEstado(EstadoCompra.EN_ESPERA);
        compra.setFecha(LocalDateTime.now());
        compra.setInventarioIngresado(false);
        Compra compraGuardada = compraRepository.save(compra);

        DetalleCompra detalle = new DetalleCompra();
        detalle.setCompra(compraGuardada);
        detalle.setProducto(maletaCabina);
        detalle.setProveedorProducto(ofertaMaletaImportadora);
        detalle.setCantidad(new BigDecimal("5"));
        detalle.setCostoUnitario(new BigDecimal("820.00"));
        detalle.setSubtotal(new BigDecimal("4100.00"));
        detalleCompraRepository.save(detalle);

        compraGuardada.setTotal(new BigDecimal("4100.00"));
        compraRepository.save(compraGuardada);
    }

    private void guardarInventario(Producto producto, Almacen almacen, BigDecimal cantidad) {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setAlmacen(almacen);
        inventario.setCantidad(cantidad);
        inventarioRepository.save(inventario);
    }
}
