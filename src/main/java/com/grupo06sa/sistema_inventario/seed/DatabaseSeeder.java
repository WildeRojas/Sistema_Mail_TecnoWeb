package com.grupo06sa.sistema_inventario.seed;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.Rol;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    public DatabaseSeeder(
        RolRepository rolRepository,
        TipoOperacionRepository tipoOperacionRepository,
        CategoriaRepository categoriaRepository,
        ProveedorRepository proveedorRepository,
        AlmacenRepository almacenRepository,
        UsuarioRepository usuarioRepository,
        ProductoRepository productoRepository
    ) {
        this.rolRepository = rolRepository;
        this.tipoOperacionRepository = tipoOperacionRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.almacenRepository = almacenRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(String... args) {
        if (rolRepository.count() > 0) {
            return;
        }

        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        Rol rolEmpleado = new Rol();
        rolEmpleado.setNombre("EMPLEADO");
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        rolRepository.saveAll(List.of(rolAdmin, rolEmpleado, rolCliente));

        TipoOperacion entrada = new TipoOperacion();
        entrada.setNombre("ENTRADA");
        TipoOperacion salida = new TipoOperacion();
        salida.setNombre("SALIDA");
        TipoOperacion traslado = new TipoOperacion();
        traslado.setNombre("TRASLADO");
        tipoOperacionRepository.saveAll(List.of(entrada, salida, traslado));

        Categoria mochilas = new Categoria();
        mochilas.setNombre("Mochilas");
        mochilas.setImagen("https://example.com/categorias/mochilas.jpg");
        Categoria maletas = new Categoria();
        maletas.setNombre("Maletas");
        maletas.setImagen("https://example.com/categorias/maletas.jpg");
        Categoria bolsones = new Categoria();
        bolsones.setNombre("Bolsones");
        bolsones.setImagen("https://example.com/categorias/bolsones.jpg");
        Categoria carteras = new Categoria();
        carteras.setNombre("Carteras");
        carteras.setImagen("https://example.com/categorias/carteras.jpg");
        Categoria articulosViaje = new Categoria();
        articulosViaje.setNombre("Articulos de Viaje");
        articulosViaje.setImagen("https://example.com/categorias/articulos.jpg");
        categoriaRepository.saveAll(List.of(mochilas, maletas, bolsones, carteras, articulosViaje));

        Proveedor samsonite = new Proveedor();
        samsonite.setNombre("Distribuidora Samsonite");
        samsonite.setTelefono("70000001");
        samsonite.setCorreo("ventas@samsonite.demo");
        samsonite.setDireccion("Av. Principal 123");
        Proveedor amazonas = new Proveedor();
        amazonas.setNombre("Sebastian");
        amazonas.setTelefono("70000002");
        amazonas.setCorreo("sebamendex11@gmal.com");
        amazonas.setDireccion("Calle Comercio 456");
        proveedorRepository.saveAll(List.of(samsonite, amazonas));
        
        Almacen central = new Almacen();
        central.setNombre("Almacen Central");
        central.setCapacidad(1000);
        central.setImagen("https://example.com/almacenes/central.jpg");
        central.setCoordenadasGps("-17.7833,-63.1833");
        central.setDireccion("Zona Industrial");
        Almacen sucursal = new Almacen();
        sucursal.setNombre("Sucursal Centro");
        sucursal.setCapacidad(300);
        sucursal.setImagen("https://example.com/almacenes/sucursal.jpg");
        sucursal.setCoordenadasGps("-17.7830,-63.1820");
        sucursal.setDireccion("Centro");
        almacenRepository.saveAll(List.of(central, sucursal));

        Usuario admin = new Usuario();
        admin.setNombre("Wilder Ario");
        admin.setApellido("Rojas");
        admin.setEmail("rojasjhunior025@gmail.com");
        admin.setContrasena("jhunior789");
        admin.setTelefono("78787845");
        admin.setFoto("https://example.com/usuarios/admin.jpg");
        admin.setRol(rolAdmin);
        Usuario cliente = new Usuario();
        cliente.setNombre("Jhunior");
        cliente.setApellido("Rojas Mamani");
        cliente.setEmail("wilderariorojas@gmail.com");
        cliente.setContrasena("cliente123");
        cliente.setTelefono("70001002");
        cliente.setFoto("https://example.com/usuarios/cliente.jpg");
        cliente.setRol(rolCliente);
        usuarioRepository.saveAll(List.of(admin, cliente));

        Producto maletaCabina = new Producto();
        maletaCabina.setCodigo("MLT-001");
        maletaCabina.setNombre("Maleta de Cabina 10kg");
        maletaCabina.setPrecio(1200.0);
        maletaCabina.setStockActual(15);
        maletaCabina.setStockMinimo(5);
        maletaCabina.setImagen("https://example.com/productos/maleta-cabina.jpg");
        maletaCabina.setCategoria(maletas);
        maletaCabina.setProveedor(samsonite);

        Producto mochilaEscolar = new Producto();
        mochilaEscolar.setCodigo("MCH-101");
        mochilaEscolar.setNombre("Mochila Escolar");
        mochilaEscolar.setPrecio(350.0);
        mochilaEscolar.setStockActual(40);
        mochilaEscolar.setStockMinimo(10);
        mochilaEscolar.setImagen("https://example.com/productos/mochila-escolar.jpg");
        mochilaEscolar.setCategoria(mochilas);
        mochilaEscolar.setProveedor(samsonite);

        Producto carteraCuero = new Producto();
        carteraCuero.setCodigo("CRT-210");
        carteraCuero.setNombre("Cartera de Cuero");
        carteraCuero.setPrecio(680.0);
        carteraCuero.setStockActual(20);
        carteraCuero.setStockMinimo(6);
        carteraCuero.setImagen("https://example.com/productos/cartera-cuero.jpg");
        carteraCuero.setCategoria(carteras);
        carteraCuero.setProveedor(amazonas);

        productoRepository.saveAll(List.of(maletaCabina, mochilaEscolar, carteraCuero));
    }
}
