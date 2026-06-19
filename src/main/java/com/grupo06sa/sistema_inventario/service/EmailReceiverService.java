package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.processor.CommandProcessor;
import com.grupo06sa.sistema_inventario.util.CommandParser;
import com.grupo06sa.sistema_inventario.util.CommandResult;
import com.grupo06sa.sistema_inventario.util.CommandRequest;
import com.grupo06sa.sistema_inventario.util.MimeDecoderUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("^[A-Z_]+\\[.*\\]$");
    private static final Pattern FROM_PATTERN = Pattern.compile("<([^>]+)>");

    private final EmailSenderService emailSenderService;
    private final CommandProcessor commandProcessor;

    @Value("${mail.server}")
    private String mailServer;

    @Value("${mail.pop3.port}")
    private int pop3Port;

    @Value("${mail.user}")
    private String mailUser;

    @Value("${mail.password}")
    private String mailPassword;

    public EmailReceiverService(EmailSenderService emailSenderService, CommandProcessor commandProcessor) {
        this.emailSenderService = emailSenderService;
        this.commandProcessor = commandProcessor;
    }

    @Scheduled(fixedDelay = 10000)
    public void checkInbox() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mailServer, pop3Port), 10000);
            socket.setSoTimeout(15000);

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
                    logger.warn("POP3 server rejected connection: {}", greeting);
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

                for (int i = 1; i <= messageCount; i++) {
                    boolean retrieved = false;
                    try {
                        List<String> lines = retrieveMessage(reader, writer, i);
                        if (lines == null) {
                            continue;
                        }
                        retrieved = true;
                        processMessage(lines);
                    } catch (Exception ex) {
                        logger.warn("Failed to process POP3 message {}", i, ex);
                    } finally {
                        if (retrieved) {
                            sendOkCommand(reader, writer, "DELE " + i);
                        }
                    }
                }

                sendCommand(writer, "QUIT");
            }
        } catch (IOException ex) {
            logger.error("Failed to poll POP3 inbox", ex);
        }
    }

    private int readMessageCount(BufferedReader reader, PrintWriter writer) throws IOException {
        sendCommand(writer, "STAT");
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            logger.warn("POP3 STAT failed: {}", response);
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

    private List<String> retrieveMessage(BufferedReader reader, PrintWriter writer, int index) throws IOException {
        sendCommand(writer, "RETR " + index);
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            logger.warn("POP3 RETR {} failed: {}", index, response);
            return null;
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

    private void processMessage(List<String> lines) {
        ParsedHeaders headers = parseHeaders(lines);
        String decodedSubject = MimeDecoderUtil.decode(headers.subject());
        if (!isValidSubject(decodedSubject)) {
            logger.info("Skipping message with invalid subject: {}", decodedSubject);
            return;
        }

        CommandResult response = null;
        try {
            CommandRequest request = CommandParser.parse(decodedSubject);
            response = commandProcessor.process(request, headers.from());
        } catch (Exception ex) {
            logger.warn("Failed to process command: {}", decodedSubject, ex);
            response = CommandResult.text("Error");
        }

        if (headers.from() != null && !headers.from().isBlank()) {
            try {
                if (response != null && response.getAttachment() != null) {
                    emailSenderService.sendEmailWithAttachment(
                        headers.from(),
                        "Respuesta",
                        response.getBody(),
                        response.getAttachment()
                    );
                } else if (response != null) {
                    emailSenderService.sendEmail(headers.from(), "Respuesta", response.getBody());
                }
            } catch (Exception ex) {
                logger.error("Failed to send response to {}", headers.from(), ex);
            }
        } else {
            logger.warn("Skipping reply because sender is empty");
        }
    }

    private ParsedHeaders parseHeaders(List<String> lines) {
        StringBuilder subject = new StringBuilder();
        StringBuilder from = new StringBuilder();
        String currentHeader = null;

        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }

            if (line.startsWith(" ") || line.startsWith("\t")) {
                if ("subject".equals(currentHeader)) {
                    subject.append(" ").append(line.trim());
                } else if ("from".equals(currentHeader)) {
                    from.append(" ").append(line.trim());
                }
                continue;
            }

            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }

            String headerName = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(idx + 1).trim();
            currentHeader = headerName;
            if ("subject".equals(headerName)) {
                subject.setLength(0);
                subject.append(value);
            } else if ("from".equals(headerName)) {
                from.setLength(0);
                from.append(value);
            }
        }

        String parsedFrom = extractFrom(from.toString());
        return new ParsedHeaders(parsedFrom, subject.toString());
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

    private boolean isValidSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return false;
        }

        return SUBJECT_PATTERN.matcher(subject.trim()).matches();
    }

    private void sendCommand(PrintWriter writer, String command) {
        writer.print(command + "\r\n");
        writer.flush();
    }

    private boolean sendOkCommand(BufferedReader reader, PrintWriter writer, String command) throws IOException {
        sendCommand(writer, command);
        String response = reader.readLine();
        if (response == null || !response.startsWith("+OK")) {
            logger.warn("POP3 command failed ({}): {}", command, response);
            return false;
        }

        return true;
    }

    private record ParsedHeaders(String from, String subject) {
    }
}
