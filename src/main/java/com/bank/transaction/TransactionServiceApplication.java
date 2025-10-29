package com.bank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Clase principal de la aplicación Transaction Service.
 * Este microservicio maneja las operaciones de transacciones financieras.
 *
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TransactionServiceApplication {

  /**
   * Método principal para iniciar la aplicación Transaction Service.
   *
   * @param args argumentos de línea de comandos
   */
	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

}
