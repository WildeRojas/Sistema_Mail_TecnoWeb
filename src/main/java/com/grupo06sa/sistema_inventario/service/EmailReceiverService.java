package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.CorreoProcesado;
import com.grupo06sa.sistema_inventario.entity.EstadoCorreo;
import com.grupo06sa.sistema_inventario.processor.CommandProcessor;
import com.grupo06sa.sistema_inventario.processor.DbErrorTranslator;
import com.grupo06sa.sistema_inventario.repository.CorreoProcesadoRepository;
import com.grupo06sa.sistema_inventario.util.CommandParser;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.HtmlBuilderUtil;
import com.grupo06sa.sistema_inventario.util.MimeDecoderUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class EmailReceiverService {
    private static final Logger logger = LoggerFactory.getLogger(EmailReceiverService.class);
    private static final Pattern FROM_PATTERN = Pattern.compile("<([^>]+)>");
    private static final int MAX_INTENTOS_LECTURA = 5;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private final EmailSenderService emailSenderService;
    private final CommandProcessor commandProcessor;
    private final CorreoProcesadoRepository correoProcesadoRepository;

    private final Map<String, Integer> contadorReintentosLectura = new ConcurrentHashMap<>();

    @Value("${mail.server}")
    private String mailServer;

    @Value("${mail.pop3.port}")
    private int pop3Port;

    @Value("${mail.user}")
    private String mailUser;

    @Value("${mail.password}")
    private String mailPassword;

    public EmailReceiverService(
        EmailSenderService emailSenderService,
        CommandProcessor commandProcessor,
        CorreoProcesadoRepository correoProcesadoRepository
    ) {
        this.emailSenderService = emailSenderService;
        this.commandProcessor = commandProcessor;
        this.correoProcesadoRepository = correoProcesadoRepository;
    }

    @Scheduled(fixedDelayString = "${mail.receiver.poll.fixed-delay-ms:10000}")
    public void checkInbox() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mailServer, pop3Port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                )
            ) {
                String greeting = reader.readLine();
                if (greeting == null || !greeting.startsWith("+OK")) {
                    logger.warn("El servidor POP3 rechazó la conexión: {}", greeting);
                    return;
                }

                if (!sendOkCommand(reader, writer, "USER " + mailUser)) {
                    return;
                }
                if (!sendOkCommand(reader, writer, "PASS " + mailPassword)) {
                    return;
                }

                int messageCount = readMessageCount(reader, writer);
                if (messageCount <= 0) {
                    sendCommand(writer, "QUIT");
                    return;
                }

                Map<Integer, String> uidsPorIndice = readUidl(reader, writer);
                podarContadoresReintento(new HashSet<>(uidsPorIndice.values()));

                for (int i = 1; i <= messageCount; i++) {
                    String uid = uidsPorIndice.get(i);
                    try {
                        procesarMensaje(reader, writer, i, uid);
                    } catch (Exception ex) {
                        logger.error("Error inesperado procesando el mensaje {} (uid={})", i, uid, ex);
                    }
                }

                sendCommand(writer, "QUIT");
            }
        } catch (IOException ex) {
            logger.error("Fallo al conectar o leer el buzón POP3; se reintentará en el próximo ciclo programado", ex);
        }
    }

    private int readMessageCount(BufferedReader reader, PrintWriter writer) throws IOException {
        sendCommand(writer, "STAT");
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            logger.warn("El comando POP3 STAT falló: {}", response);
            return 0;
        }

        String[] parts = response.split("\\s+");
        if (parts.length < 2) {
            return 0;
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Map<Integer, String> readUidl(BufferedReader reader, PrintWriter writer) throws IOException {
        sendCommand(writer, "UIDL");
        String response = reader.readLine();
        Map<Integer, String> resultado = new HashMap<>();
        if (response == null || !response.startsWith("+OK")) {
            logger.debug("El servidor POP3 no soporta UIDL; se usará un identificador alternativo por mensaje.");
            return resultado;
        }

        String line;
        while ((line = reader.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2) {
                try {
                    resultado.put(Integer.parseInt(parts[0]), parts[1]);
                } catch (NumberFormatException ex) {
                    logger.debug("Línea UIDL con formato inesperado, se ignora: {}", line);
                }
            }
        }

        return resultado;
    }

    private void podarContadoresReintento(Set<String> uidsActuales) {
        contadorReintentosLectura.keySet().removeIf(clave -> !clave.startsWith("idx:") && !uidsActuales.contains(clave));
    }

    private List<String> retrieveMessage(BufferedReader reader, PrintWriter writer, int index) throws IOException {
        sendCommand(writer, "RETR " + index);
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            throw new IOException("El comando POP3 RETR " + index + " falló: " + response);
        }

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }

            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            lines.add(line);
        }

        return lines;
    }

    private void procesarMensaje(BufferedReader reader, PrintWriter writer, int index, String uid) {
        List<String> lines;
        try {
            lines = retrieveMessage(reader, writer, index);
        } catch (IOException ex) {
            manejarFalloLectura(reader, writer, index, uid, ex);
            return;
        }
        if (uid != null) {
            contadorReintentosLectura.remove(uid);
        }

        Map<String, String> headers = parseHeaders(lines);
        CorreoEntrante correo = construirCorreoEntrante(headers);

        if (esAutoGenerado(correo)) {
            logger.info(
                "Correo ignorado por ser auto-generado (evita bucle de auto-respuesta): remitente={}, asunto={}",
                correo.from(), correo.subject()
            );
            borrarMensaje(reader, writer, index);
            return;
        }

        if (!CommandParser.esComando(correo.subject())) {
            logger.info("Correo ignorado por no tener formato de comando: asunto={}", correo.subject());
            borrarMensaje(reader, writer, index);
            return;
        }

        String identificador = resolverIdentificador(correo, uid, lines);
        procesarComando(reader, writer, index, correo, identificador);
    }

    private void manejarFalloLectura(BufferedReader reader, PrintWriter writer, int index, String uid, IOException ex) {
        String clave = uid != null ? uid : ("idx:" + index);
        int intentos = contadorReintentosLectura.merge(clave, 1, Integer::sum);
        if (intentos >= MAX_INTENTOS_LECTURA) {
            logger.error(
                "Mensaje venenoso descartado tras {} intentos fallidos de lectura (clave={})",
                intentos, clave, ex
            );
            contadorReintentosLectura.remove(clave);
            borrarMensaje(reader, writer, index);
        } else {
            logger.warn(
                "No se pudo leer el mensaje {} (intento {}/{}); se reintentará en el próximo ciclo: {}",
                index, intentos, MAX_INTENTOS_LECTURA, ex.getMessage()
            );
        }
    }

    private void procesarComando(BufferedReader reader, PrintWriter writer, int index, CorreoEntrante correo, String identificador) {
        try {
            if (correoProcesadoRepository.existsByMessageId(identificador)) {
                logger.info("El correo ya había sido procesado (id={}); no se reejecuta el comando.", identificador);
                borrarMensaje(reader, writer, index);
                return;
            }

            CommandRequest request = CommandParser.parse(correo.subject());
            String nombreComandoLog = request.getEstado() == CommandRequest.Estado.RECONOCIDO
                ? request.getCommand()
                : comandoSinParametros(correo.subject());
            logger.info("Comando recibido: remitente={}, comando={}", correo.from(), nombreComandoLog);

            CommandResult response;
            try {
                response = commandProcessor.process(request, correo.from());
            } catch (Exception ex) {
                DbErrorTranslator.Traduccion traduccion = DbErrorTranslator.traducir(ex);
                response = CommandResult.text(
                    HtmlBuilderUtil.buildErrorTemplate(traduccion.titulo(), traduccion.mensaje()),
                    traduccion.tipo()
                );
            }

            logger.info("Resultado del comando {} de {}: {}", nombreComandoLog, correo.from(), response.getTipo());

            if (response.getTipo() == CommandResult.Tipo.ERROR_BD) {
                logger.warn(
                    "Base de datos no disponible al procesar el comando (id={}); se responderá el error por "
                        + "correo y no se borrará el mensaje, para reintentar cuando la base de datos vuelva.",
                    identificador
                );
                enviarRespuesta(correo.from(), response);
                return;
            }

            CorreoProcesado registro = new CorreoProcesado();
            registro.setMessageId(identificador);
            registro.setRemitente(correo.from());
            registro.setComando(request.getEstado() == CommandRequest.Estado.RECONOCIDO ? request.getCommand() : null);
            registro.setEstado(response.getTipo() == CommandResult.Tipo.OK ? EstadoCorreo.PROCESADO : EstadoCorreo.ERROR);
            registro.setCreatedAt(LocalDateTime.now());
            correoProcesadoRepository.save(registro);

            enviarRespuesta(correo.from(), response);
            borrarMensaje(reader, writer, index);
        } catch (Exception ex) {
            DbErrorTranslator.Traduccion traduccion = DbErrorTranslator.traducir(ex);
            logger.warn(
                "No se pudo confirmar en base de datos el procesamiento del correo (id={}); se responderá el "
                    + "error por correo y no se borrará el mensaje: {}",
                identificador, traduccion.mensaje()
            );
            CommandResult respuestaError = CommandResult.text(
                HtmlBuilderUtil.buildErrorTemplate(traduccion.titulo(), traduccion.mensaje()),
                traduccion.tipo()
            );
            enviarRespuesta(correo.from(), respuestaError);
        }
    }

    private String comandoSinParametros(String subject) {
        if (subject == null) {
            return "";
        }
        String limpio = subject.trim();
        int corte = limpio.length();
        int idxCorchete = limpio.indexOf('[');
        if (idxCorchete >= 0) {
            corte = Math.min(corte, idxCorchete);
        }
        int idxParentesis = limpio.indexOf('(');
        if (idxParentesis >= 0) {
            corte = Math.min(corte, idxParentesis);
        }
        String nombre = limpio.substring(0, corte).trim();
        return nombre.isEmpty() ? limpio : nombre;
    }

    private void enviarRespuesta(String to, CommandResult response) {
        if (to == null || to.isBlank()) {
            logger.warn("No se puede responder: la dirección del remitente está vacía.");
            return;
        }

        try {
            if (response.getAttachment() != null) {
                emailSenderService.sendEmailWithAttachment(to, "Respuesta", response.getBody(), response.getAttachment());
            } else {
                emailSenderService.sendEmail(to, "Respuesta", response.getBody());
            }
        } catch (Exception ex) {
            logger.error("No se pudo entregar ni encolar en el spool la respuesta para {}", to, ex);
        }
    }

    private void borrarMensaje(BufferedReader reader, PrintWriter writer, int index) {
        try {
            boolean confirmado = sendOkCommand(reader, writer, "DELE " + index);
            if (!confirmado) {
                logger.warn(
                    "El servidor POP3 no confirmó el borrado del mensaje {}; el registro de idempotencia evita "
                        + "reprocesarlo si sigue presente en el próximo ciclo.",
                    index
                );
            }
        } catch (IOException ex) {
            logger.warn("No se pudo enviar DELE para el mensaje {}", index, ex);
        }
    }

    private String resolverIdentificador(CorreoEntrante correo, String uid, List<String> lines) {
        String messageId = limpiarMessageId(correo.messageId());
        if (messageId != null) {
            return "mid:" + messageId;
        }
        if (uid != null && !uid.isBlank()) {
            return "uidl:" + uid;
        }
        return "hash:" + hashEncabezados(lines);
    }

    private String limpiarMessageId(String raw) {
        if (raw == null) {
            return null;
        }
        String valor = raw.trim();
        if (valor.length() >= 2 && valor.startsWith("<") && valor.endsWith(">")) {
            valor = valor.substring(1, valor.length() - 1);
        }
        return valor.isBlank() ? null : valor;
    }

    private String hashEncabezados(List<String> lines) {
        StringBuilder bloque = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }
            bloque.append(line).append('\n');
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bloque.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }

    private boolean esAutoGenerado(CorreoEntrante correo) {
        String autoSubmitted = correo.autoSubmitted();
        if (autoSubmitted != null && !autoSubmitted.trim().equalsIgnoreCase("no")) {
            return true;
        }

        String precedence = correo.precedence();
        if (precedence != null) {
            String p = precedence.trim().toLowerCase(Locale.ROOT);
            if (p.equals("bulk") || p.equals("auto_reply") || p.equals("list")) {
                return true;
            }
        }

        String returnPath = correo.returnPath();
        return returnPath != null && returnPath.trim().equals("<>");
    }

    private CorreoEntrante construirCorreoEntrante(Map<String, String> headers) {
        String from = extractFrom(headers.get("from"));
        String subject = MimeDecoderUtil.decode(headers.getOrDefault("subject", ""));
        return new CorreoEntrante(
            from,
            subject,
            headers.get("message-id"),
            headers.get("auto-submitted"),
            headers.get("precedence"),
            headers.get("return-path")
        );
    }

    private Map<String, String> parseHeaders(List<String> lines) {
        Map<String, StringBuilder> crudo = new HashMap<>();
        String claveActual = null;

        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }

            if ((line.startsWith(" ") || line.startsWith("\t")) && claveActual != null) {
                crudo.get(claveActual).append(' ').append(line.trim());
                continue;
            }

            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }

            String headerName = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(idx + 1).trim();
            claveActual = headerName;
            crudo.put(headerName, new StringBuilder(value));
        }

        Map<String, String> resultado = new HashMap<>();
        crudo.forEach((clave, valor) -> resultado.put(clave, valor.toString()));
        return resultado;
    }

    private String extractFrom(String rawFrom) {
        if (rawFrom == null || rawFrom.isBlank()) {
            return null;
        }

        Matcher matcher = FROM_PATTERN.matcher(rawFrom);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return rawFrom.trim();
    }

    private void sendCommand(PrintWriter writer, String command) {
        writer.print(command + "\r\n");
        writer.flush();
    }

    private boolean sendOkCommand(BufferedReader reader, PrintWriter writer, String command) throws IOException {
        sendCommand(writer, command);
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            logger.warn("El comando POP3 {} falló: {}", enmascararCredencial(command), response);
            return false;
        }

        return true;
    }

    private String enmascararCredencial(String command) {
        if (command.startsWith("USER ") || command.startsWith("PASS ")) {
            return command.substring(0, 4) + "[oculto]";
        }
        return command;
    }

    private record CorreoEntrante(
        String from,
        String subject,
        String messageId,
        String autoSubmitted,
        String precedence,
        String returnPath
    ) {
    }
}
