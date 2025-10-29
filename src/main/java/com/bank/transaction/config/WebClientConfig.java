package com.bank.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración para WebClient.
 * Provee un bean de WebClient para comunicación con otros microservicios.
 */
@Configuration
public class WebClientConfig {

  /**
   * Crea y configura un bean de WebClient.
   * WebClient es el cliente reactivo para hacer peticiones HTTP.
   *
   * @return instancia configurada de WebClient
   */
  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}