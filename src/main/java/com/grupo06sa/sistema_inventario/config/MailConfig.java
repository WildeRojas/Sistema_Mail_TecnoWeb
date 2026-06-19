package com.grupo06sa.sistema_inventario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${mail.server}")
    private String mailServer;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.user:}")
    private String mailUser;

    @Value("${mail.password:}")
    private String mailPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailServer);
        mailSender.setPort(smtpPort);
        // No configuramos username ni password para evitar que Spring JavaMailSender intente
        // autenticarse, ya que el servidor SMTP en puerto 25 de Tecnoweb no requiere auth.

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
