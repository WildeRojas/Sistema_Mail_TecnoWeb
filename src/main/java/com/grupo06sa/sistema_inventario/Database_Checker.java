package com.grupo06sa.sistema_inventario;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Database_Checker implements CommandLineRunner {

    private final DataSource dataSource;

    // Spring Boot inyectará automáticamente la configuración de tu application.properties aquí
    public Database_Checker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("\n=================================================");
            System.out.println(" ¡CONECTADO CON ÉXITO A LA BASE DE DATOS! ");
            System.out.println(" Servidor: " + connection.getMetaData().getURL());
            System.out.println(" Base de datos activa: " + connection.getCatalog());
            System.out.println("=================================================\n");
        } catch (Exception e) {
            System.err.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println(" ERROR: No se pudo conectar al servidor ");
            System.err.println(" Detalle: " + e.getMessage());
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
        }
    }
}