package com.grupo06sa.sistema_inventario.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.grupo06sa.sistema_inventario.util.EmailAttachment;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    @Value("${mail.server}")
    private String mailServer;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.user}")
    private String mailUser;

    @Value("${mail.password}")
    private String mailPassword;

    public void sendEmail(String to, String subject, String body) {
        sendEmailInternal(to, subject, body, null);
    }

    public void sendEmailWithAttachment(
        String to,
        String subject,
        String body,
        EmailAttachment attachment
    ) {
        if (attachment == null) {
            sendEmailInternal(to, subject, body, null);
            return;
        }

        sendEmailInternal(to, subject, body, attachment);
    }

    private void sendEmailInternal(String to, String subject, String body, EmailAttachment attachment) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient is empty");
        }

        String fromAddress = buildFromAddress();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mailServer, smtpPort), 10000);
            socket.setSoTimeout(15000);
            logger.info(
                "SMTP conectado: local={} -> {}:{}",
                socket.getLocalAddress().getHostAddress(),
                mailServer,
                smtpPort
            );

            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                )
            ) {
                expectCode(reader, 220);
                if (!sendEhlo(reader, writer)) {
                    throw new IOException("SMTP server rejected EHLO/HELO");
                }

                //authenticate(reader, writer);
                sendCommand(writer, "MAIL FROM:<" + fromAddress + ">");
                expectCode(reader, 250);
                sendCommand(writer, "RCPT TO:<" + to + ">");
                expectCode(reader, 250, 251);
                sendCommand(writer, "DATA");
                expectCode(reader, 354);

                String normalizedBody = normalizeBody(body);
                writer.print("From: " + fromAddress + "\r\n");
                writer.print("To: " + to + "\r\n");
                writer.print("Subject: " + safe(subject) + "\r\n");

                if (attachment == null) {
                    writer.print("Content-Type: text/html; charset=UTF-8\r\n");
                    writer.print("\r\n");
                    writer.print(normalizedBody);
                    writer.print("\r\n.\r\n");
                } else {
                    String boundary = "----=_Part_" + UUID.randomUUID();
                    writer.print("MIME-Version: 1.0\r\n");
                    writer.print("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n");
                    writer.print("\r\n");
                    writer.print("--" + boundary + "\r\n");
                    writer.print("Content-Type: text/html; charset=UTF-8\r\n");
                    writer.print("Content-Transfer-Encoding: 8bit\r\n\r\n");
                    writer.print(normalizedBody);
                    writer.print("\r\n");

                    String fileName = safe(attachment.getFileName());
                    String contentType = safe(attachment.getContentType());
                    if (contentType.isBlank()) {
                        contentType = "application/octet-stream";
                    }

                    writer.print("--" + boundary + "\r\n");
                    writer.print("Content-Type: " + contentType + "; name=\"" + fileName + "\"\r\n");
                    if (contentType.startsWith("image/")) {
                        writer.print("Content-Disposition: inline; filename=\"" + fileName + "\"\r\n");
                        writer.print("Content-ID: <" + fileName + ">\r\n");
                    } else {
                        writer.print("Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n");
                    }
                    writer.print("Content-Transfer-Encoding: base64\r\n\r\n");
                    writer.print(base64Mime(attachment.getData()));
                    writer.print("\r\n--" + boundary + "--\r\n");
                    writer.print(".\r\n");
                }
                writer.flush();

                expectCode(reader, 250);
                sendCommand(writer, "QUIT");
                expectCode(reader, 221);
            }
        } catch (IOException ex) {
            logger.error("Failed to send SMTP message to {}", to, ex);
            throw new IllegalStateException("SMTP send failed", ex);
        }
    }

    private boolean sendEhlo(BufferedReader reader, PrintWriter writer) throws IOException {
        sendCommand(writer, "EHLO mail.tecnoweb.org.bo");
        int code = readResponse(reader);
        if (code == 250) {
            return true;
        }

        sendCommand(writer, "HELO localhost");
        return readResponse(reader) == 250;
    }

    private void sendCommand(PrintWriter writer, String command) {
        logCommand(command);
        writer.print(command + "\r\n");
        writer.flush();
    }

    private int readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("SMTP connection closed by server");
        }

        logger.info("SERVIDOR RESPONDE: {}", line);

        int code = parseCode(line);
        if (line.length() > 3 && line.charAt(3) == '-') {
            String prefix = line.substring(0, 3) + " ";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    break;
                }
            }
        }

        return code;
    }

    private void expectCode(BufferedReader reader, int... expected) throws IOException {
        int code = readResponse(reader);
        for (int allowed : expected) {
            if (code == allowed) {
                return;
            }
        }

        throw new IOException("Unexpected SMTP response code: " + code);
    }

    private int parseCode(String line) {
        if (line.length() < 3) {
            return -1;
        }

        try {
            return Integer.parseInt(line.substring(0, 3));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String normalizeBody(String body) {
        if (body == null) {
            return "";
        }

        String normalized = body.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(".")) {
                builder.append('.');
            }
            builder.append(line);
            if (i < lines.length - 1) {
                builder.append("\r\n");
            }
        }

        return builder.toString();
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Mime(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        return Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(data);
    }

    private String buildFromAddress() {
        if (mailUser.contains("@")) {
            return mailUser;
        }

        String domain = mailServer;
        if (domain.startsWith("mail.")) {
            domain = domain.substring(5);
        }

        return mailUser + "@" + domain;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void logCommand(String command) {
        String masked = command;
        if (command.startsWith("AUTH")
            || command.equals(base64(mailUser))
            || command.equals(base64(mailPassword))) {
            masked = "[oculto]";
        }

        logger.info("CLIENTE ENVIA: {}", masked);
    }
}
