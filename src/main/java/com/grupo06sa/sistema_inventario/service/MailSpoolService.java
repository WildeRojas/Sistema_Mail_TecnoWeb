package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailSpoolService {
    private static final Logger logger = LoggerFactory.getLogger(MailSpoolService.class);
    private static final String SUFIJO_ARCHIVO = ".pendiente.properties";

    @Value("${app.mail.spool.dir:${java.io.tmpdir}/spool}")
    private String directorioSpool;

    public record MensajePendiente(Path archivo, String to, String subject, String body, EmailAttachment attachment) {
    }

    public void encolar(String to, String subject, String body, EmailAttachment attachment) {
        Path dir = Paths.get(directorioSpool);
        try {
            Files.createDirectories(dir);

            Properties props = new Properties();
            props.setProperty("to", to == null ? "" : to);
            props.setProperty("subject", subject == null ? "" : subject);
            props.setProperty("body", body == null ? "" : body);
            if (attachment != null) {
                props.setProperty(
                    "attachmentFileName",
                    attachment.getFileName() == null ? "" : attachment.getFileName()
                );
                props.setProperty(
                    "attachmentContentType",
                    attachment.getContentType() == null ? "" : attachment.getContentType()
                );
                byte[] datos = attachment.getData();
                props.setProperty(
                    "attachmentData",
                    Base64.getEncoder().encodeToString(datos == null ? new byte[0] : datos)
                );
            }

            String nombreBase = "correo-" + System.currentTimeMillis() + "-" + UUID.randomUUID();
            Path temporal = dir.resolve(nombreBase + ".tmp");
            Path definitivo = dir.resolve(nombreBase + SUFIJO_ARCHIVO);

            try (Writer writer = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                props.store(writer, "Correo pendiente de envio SMTP (spool durable)");
            }
            Files.move(temporal, definitivo, StandardCopyOption.ATOMIC_MOVE);

            logger.info("Correo encolado para reintento (destinatario={}, archivo={})", to, definitivo.getFileName());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo escribir el correo pendiente en el spool local", ex);
        }
    }

    public List<MensajePendiente> listarPendientes() {
        Path dir = Paths.get(directorioSpool);
        List<MensajePendiente> pendientes = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return pendientes;
        }

        try (var stream = Files.list(dir)) {
            List<Path> archivos = stream
                .filter(p -> p.getFileName().toString().endsWith(SUFIJO_ARCHIVO))
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();

            for (Path archivo : archivos) {
                try {
                    pendientes.add(leer(archivo));
                } catch (IOException ex) {
                    logger.warn("No se pudo leer el archivo de spool {}; se omite en este ciclo", archivo.getFileName(), ex);
                }
            }
        } catch (IOException ex) {
            logger.warn("No se pudo listar el directorio de spool {}", dir, ex);
        }

        return pendientes;
    }

    private MensajePendiente leer(Path archivo) throws IOException {
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            props.load(reader);
        }

        String to = props.getProperty("to", "");
        String subject = props.getProperty("subject", "");
        String body = props.getProperty("body", "");
        String fileName = props.getProperty("attachmentFileName");

        EmailAttachment attachment = null;
        if (fileName != null && !fileName.isBlank()) {
            String contentType = props.getProperty("attachmentContentType", "");
            byte[] datos = Base64.getDecoder().decode(props.getProperty("attachmentData", ""));
            attachment = new EmailAttachment(fileName, contentType, datos);
        }

        return new MensajePendiente(archivo, to, subject, body, attachment);
    }

    public void eliminar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ex) {
            logger.warn("No se pudo eliminar el archivo de spool {} tras el envío exitoso", archivo.getFileName(), ex);
        }
    }
}
