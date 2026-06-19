package com.grupo06sa.sistema_inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SistemaInventarioApplication {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		SpringApplication.run(SistemaInventarioApplication.class, args);
	}

}
