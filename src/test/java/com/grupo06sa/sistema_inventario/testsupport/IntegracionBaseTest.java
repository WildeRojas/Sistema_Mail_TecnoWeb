package com.grupo06sa.sistema_inventario.testsupport;

import com.grupo06sa.sistema_inventario.entity.Almacen;
import com.grupo06sa.sistema_inventario.entity.Categoria;
import com.grupo06sa.sistema_inventario.entity.Inventario;
import com.grupo06sa.sistema_inventario.entity.Producto;
import com.grupo06sa.sistema_inventario.entity.Proveedor;
import com.grupo06sa.sistema_inventario.entity.ProveedorProducto;
import com.grupo06sa.sistema_inventario.entity.Rol;
import com.grupo06sa.sistema_inventario.entity.TipoOperacion;
import com.grupo06sa.sistema_inventario.entity.Usuario;
import com.grupo06sa.sistema_inventario.processor.CommandProcessor;
import com.grupo06sa.sistema_inventario.repository.AlertaStockRepository;
import com.grupo06sa.sistema_inventario.repository.AlmacenRepository;
import com.grupo06sa.sistema_inventario.repository.CategoriaRepository;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.CorreoProcesadoRepository;
import com.grupo06sa.sistema_inventario.repository.DetalleCompraRepository;
import com.grupo06sa.sistema_inventario.repository.InventarioRepository;
import com.grupo06sa.sistema_inventario.repository.MovimientoInventarioRepository;
import com.grupo06sa.sistema_inventario.repository.PagoRepository;
import com.grupo06sa.sistema_inventario.repository.ProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorProductoRepository;
import com.grupo06sa.sistema_inventario.repository.ProveedorRepository;
import com.grupo06sa.sistema_inventario.repository.RolRepository;
import com.grupo06sa.sistema_inventario.repository.SolicitudCompraRepository;
import com.grupo06sa.sistema_inventario.repository.TipoOperacionRepository;
import com.grupo06sa.sistema_inventario.repository.UsuarioRepository;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegracionBaseTest {

    protected static final GreenMailExtension GREEN_MAIL;
    protected static final WireMockServer WIRE_MOCK;

    private static final AtomicLong SECUENCIA = new AtomicLong(System.nanoTime());

    static {
        ServerSetup smtpSetup = new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        ServerSetup pop3Setup = new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_POP3);
        GREEN_MAIL = new GreenMailExtension(new ServerSetup[] {smtpSetup, pop3Setup})
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication())
            .withPerMethodLifecycle(false);
        try {
            GREEN_MAIL.beforeAll(null);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo iniciar el servidor GreenMail para las pruebas.", ex);
        }

        WIRE_MOCK = new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .globalTemplating(true)
            .usingFilesUnderDirectory("src/test/resources/wiremock"));
        WIRE_MOCK.start();
    }

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("mail.server", () -> "127.0.0.1");
        registry.add("mail.smtp.port", () -> GREEN_MAIL.getSmtp().getPort());
        registry.add("mail.pop3.port", () -> GREEN_MAIL.getPop3().getPort());
        registry.add("mail.user", () -> "bot@tiendasjunior.local");
        registry.add("mail.password", () -> "bot");

        registry.add("pagofacil.url.base", () -> "http://localhost:" + WIRE_MOCK.port());
    }

    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected ProveedorRepository proveedorRepository;
    @Autowired protected CategoriaRepository categoriaRepository;
    @Autowired protected ProductoRepository productoRepository;
    @Autowired protected AlmacenRepository almacenRepository;
    @Autowired protected ProveedorProductoRepository ofertaRepository;
    @Autowired protected InventarioRepository inventarioRepository;
    @Autowired protected TipoOperacionRepository tipoOperacionRepository;
    @Autowired protected SolicitudCompraRepository solicitudRepository;
    @Autowired protected CompraRepository compraRepository;
    @Autowired protected DetalleCompraRepository detalleCompraRepository;
    @Autowired protected PagoRepository pagoRepository;
    @Autowired protected AlertaStockRepository alertaStockRepository;
    @Autowired protected MovimientoInventarioRepository movimientoRepository;
    @Autowired protected CorreoProcesadoRepository correoProcesadoRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected CommandProcessor commandProcessor;
    @Autowired protected com.grupo06sa.sistema_inventario.service.EmailReceiverService emailReceiverService;
    @Autowired protected com.grupo06sa.sistema_inventario.service.EmailSenderService emailSenderService;
    @Autowired protected com.grupo06sa.sistema_inventario.service.MailSpoolService mailSpoolService;

    @LocalServerPort
    protected int serverPort;

    protected record RespuestaHttp(int status, String body) {
    }

    protected RespuestaHttp postJson(String path, String jsonBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + serverPort + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new RespuestaHttp(response.statusCode(), response.body());
    }

    @BeforeEach
    void baseSetUp() throws Exception {
        asegurarRolesYTiposDeOperacion();
        GREEN_MAIL.purgeEmailFromAllMailboxes();
        WIRE_MOCK.resetAll();
    }

    private void asegurarRolesYTiposDeOperacion() {
        for (RolNombre nombre : RolNombre.values()) {
            if (rolRepository.findByNombreIgnoreCase(nombre.name()).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombre.name());
                rolRepository.save(rol);
            }
        }
        for (String tipo : new String[] {"INGRESO", "SALIDA", "TRASLADO", "AJUSTE"}) {
            if (tipoOperacionRepository.findByNombreIgnoreCase(tipo).isEmpty()) {
                TipoOperacion tipoOperacion = new TipoOperacion();
                tipoOperacion.setNombre(tipo);
                tipoOperacionRepository.save(tipoOperacion);
            }
        }
    }

    protected static long siguienteSecuencia() {
        return SECUENCIA.incrementAndGet();
    }

    protected Rol rol(RolNombre nombre) {
        return rolRepository.findByNombreIgnoreCase(nombre.name())
            .orElseThrow(() -> new IllegalStateException("Rol no sembrado: " + nombre));
    }

    protected Usuario crearUsuario(String nombrePrefijo, String email, RolNombre rolNombre, Proveedor proveedor, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombrePrefijo);
        usuario.setApellido("Prueba");
        usuario.setEmail(email);
        usuario.setContrasena(passwordEncoder.encode("clave123"));
        usuario.setTelefono("70000000");
        usuario.setActivo(activo);
        usuario.setRol(rol(rolNombre));
        usuario.setProveedor(proveedor);
        return usuarioRepository.save(usuario);
    }

    protected Usuario crearPropietario(String email) {
        return crearUsuario("Propietario", email, RolNombre.PROPIETARIO, null, true);
    }

    protected Usuario crearTrabajador(String email) {
        return crearUsuario("Trabajador", email, RolNombre.TRABAJADOR, null, true);
    }

    protected Usuario crearUsuarioProveedor(String email, Proveedor proveedor) {
        return crearUsuario("UsuarioProveedor", email, RolNombre.PROVEEDOR, proveedor, true);
    }

    protected Proveedor crearProveedor(String nombre, String correo) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre);
        proveedor.setTelefono("70000000");
        proveedor.setCorreo(correo);
        proveedor.setDireccion("Direccion de prueba");
        proveedor.setNit("NIT-" + siguienteSecuencia());
        return proveedorRepository.save(proveedor);
    }

    protected Categoria crearCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setImagen(null);
        return categoriaRepository.save(categoria);
    }

    protected Producto crearProducto(String nombre, BigDecimal costoUnitario, BigDecimal stockMinimo, Categoria categoria) {
        Producto producto = new Producto();
        producto.setCodigo("COD-" + siguienteSecuencia());
        producto.setNombre(nombre);
        producto.setCostoUnitario(costoUnitario);
        producto.setStockMinimo(stockMinimo);
        producto.setCategoria(categoria);
        return productoRepository.save(producto);
    }

    protected Almacen crearAlmacen(String nombre, BigDecimal capacidad) {
        Almacen almacen = new Almacen();
        almacen.setNombre(nombre);
        almacen.setCapacidad(capacidad);
        almacen.setDireccion("Direccion de almacen de prueba");
        almacen.setCoordenadasGps("-17.0,-63.0");
        return almacenRepository.save(almacen);
    }

    protected ProveedorProducto crearOferta(Proveedor proveedor, Producto producto, BigDecimal costo) {
        ProveedorProducto oferta = new ProveedorProducto();
        oferta.setProveedor(proveedor);
        oferta.setProducto(producto);
        oferta.setCostoUnitarioActual(costo);
        oferta.setTiempoReposicionDias(3);
        oferta.setCantidadMinimaPedido(BigDecimal.ONE);
        oferta.setDisponible(true);
        return ofertaRepository.save(oferta);
    }

    protected Inventario crearInventario(Producto producto, Almacen almacen, BigDecimal cantidad) {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setAlmacen(almacen);
        inventario.setCantidad(cantidad);
        return inventarioRepository.save(inventario);
    }

    protected String emailUnico(String prefijo) {
        return prefijo + "." + siguienteSecuencia() + "@test.local";
    }

    protected void enviarCorreo(String from, String to, String subject, String cuerpo, String messageId) throws MessagingException {
        enviarCorreo(from, to, subject, cuerpo, messageId, java.util.Map.of());
    }

    protected void enviarCorreo(
        String from, String to, String subject, String cuerpo, String messageId, java.util.Map<String, String> cabecerasAdicionales
    ) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", String.valueOf(GREEN_MAIL.getSmtp().getPort()));
        Session session = Session.getInstance(props);

        MimeMessage message = new MimeMessage(session) {
            @Override
            protected void updateMessageID() throws MessagingException {

                if (getHeader("Message-ID") == null) {
                    super.updateMessageID();
                }
            }
        };
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject, "UTF-8");
        message.setText(cuerpo == null ? "Cuerpo de prueba" : cuerpo, "UTF-8");
        message.saveChanges();
        if (messageId != null) {
            message.setHeader("Message-ID", "<" + messageId + ">");
        }
        for (var entrada : cabecerasAdicionales.entrySet()) {
            message.setHeader(entrada.getKey(), entrada.getValue());
        }
        Transport.send(message);
    }

    protected void enviarCorreo(String from, String to, String subject) throws MessagingException {
        enviarCorreo(from, to, subject, null, "mid-" + siguienteSecuencia() + "@test.local");
    }

    protected String direccionBot() {
        return "bot@tiendasjunior.local";
    }

    protected byte[] utf8(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    protected int contarMensajesEnBuzonDelBot() throws IOException {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", GREEN_MAIL.getPop3().getPort()), 5000);
            socket.setSoTimeout(5000);
            try (
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
                )
            ) {
                reader.readLine();
                writer.print("USER " + direccionBot() + "\r\n");
                writer.flush();
                reader.readLine();
                writer.print("PASS bot\r\n");
                writer.flush();
                reader.readLine();
                writer.print("STAT\r\n");
                writer.flush();
                String stat = reader.readLine();
                writer.print("QUIT\r\n");
                writer.flush();
                reader.readLine();

                if (stat == null || !stat.startsWith("+OK")) {
                    return 0;
                }
                String[] partes = stat.trim().split("\\s+");
                return partes.length >= 2 ? Integer.parseInt(partes[1]) : 0;
            }
        }
    }

    protected String cuerpoDe(MimeMessage mensaje) throws Exception {
        Object contenido = mensaje.getContent();
        return contenido instanceof String texto ? texto : String.valueOf(contenido);
    }
}
