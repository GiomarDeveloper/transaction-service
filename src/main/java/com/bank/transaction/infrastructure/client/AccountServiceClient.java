package com.bank.transaction.infrastructure.client;

import com.bank.transaction.domain.exception.ExternalServiceException;
import com.bank.transaction.domain.model.AccountInfo;
import com.bank.transaction.domain.ports.output.AccountServicePort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para comunicación con el servicio de Cuentas.
 * Implementa el puerto de salida AccountServicePort para interactuar con el microservicio de cuentas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient implements AccountServicePort {

  private final WebClient webClient;

  @Value("${external.services.account.url:http://localhost:8082/}")
  private String accountServiceUrl;

  /**
   * Actualiza el balance de una cuenta en el servicio de cuentas.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a actualizar
   * @param transactionType el tipo de transacción
   * @return Mono con la respuesta del servicio de cuentas
   */
  @Override
  public Mono<Map<String, Object>> updateAccountBalance(String accountId, Double amount,
                                                        String transactionType) {
    log.info("Updating account balance - accountId: {}, amount: {}, type: {}", accountId, amount,
      transactionType);

    Map<String, Object> requestBody = Map.of(
      "amount", amount,
      "transactionType", transactionType
    );

    return webClient.patch()
      .uri(accountServiceUrl + "/accounts/{accountId}/balanceUpdate", accountId)
      .bodyValue(requestBody)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        response.bodyToMono(String.class)
          .map(errorBody -> new ExternalServiceException(
            "Account service error: " + response.statusCode() + " - " + errorBody
          ))
      )
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
      })
      .onErrorResume(ex -> {
        log.error("Error calling account service: {}", ex.getMessage());
        return Mono.error(
          new ExternalServiceException("Unable to update account balance: " + ex.getMessage()));
      });
  }

  /**
   * Valida si una cuenta existe en el servicio de cuentas.
   *
   * @param accountId el ID de la cuenta a validar
   * @return Mono que emite true si la cuenta existe, false en caso contrario
   */
  @Override
  public Mono<Boolean> validateAccountExists(String accountId) {
    log.info("Validating account existence: {}", accountId);

    return webClient.get()
      .uri(accountServiceUrl + "/accounts/{accountId}", accountId)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        Mono.just(
          new ExternalServiceException("Account validation failed: " + response.statusCode()))
      )
      .bodyToMono(Object.class) // Puede ser cualquier clase, solo nos importa el status
      .map(response -> true) // Si obtiene respuesta, la cuenta existe
      .onErrorResume(ExternalServiceException.class, ex -> {
        log.warn("Account validation failed: {}", ex.getMessage());
        return Mono.just(false);
      })
      .onErrorResume(ex -> {
        log.error("Error validating account: {}", ex.getMessage());
        return Mono.just(false);
      })
      .defaultIfEmpty(false);
  }

  /**
   * Obtiene información completa de una cuenta desde el servicio de cuentas.
   *
   * @param accountId el ID de la cuenta
   * @return Mono con la información de la cuenta
   */
  @Override
  public Mono<AccountInfo> getAccountInfo(String accountId) {
    log.info("Getting account info for: {}", accountId);

    return webClient.get()
      .uri(accountServiceUrl + "/accounts/{accountId}", accountId)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        Mono.error(
          new ExternalServiceException("Failed to get account info: " + response.statusCode()))
      )
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
      })
      .map(responseMap -> AccountInfo.builder()
        .exists(true)
        .accountType(getStringValue(responseMap, "accountType"))
        .customerId(getStringValue(responseMap, "customerId"))
        .balance(getDoubleValue(responseMap, "balance"))
        .monthlyTransactionLimit(getIntegerValue(responseMap, "monthlyTransactionLimit"))
        .currentMonthTransactions(getIntegerValue(responseMap, "currentMonthTransactions"))
        .status(getStringValue(responseMap, "status"))
        .accountNumber(getStringValue(responseMap, "accountNumber"))
        .build())
      .onErrorResume(ex -> {
        log.error("Error getting account info: {}", ex.getMessage());
        return Mono.just(AccountInfo.builder()
          .exists(false)
          .build());
      });
  }

  /**
   * Obtiene un valor string de un mapa.
   *
   * @param map el mapa de valores
   * @param key la clave del valor
   * @return el valor como string o null si no existe
   */
  private String getStringValue(Map<String, Object> map, String key) {
    return map.containsKey(key) ? map.get(key).toString() : null;
  }

  /**
   * Obtiene un valor double de un mapa.
   *
   * @param map el mapa de valores
   * @param key la clave del valor
   * @return el valor como Double o null si no existe
   */
  private Double getDoubleValue(Map<String, Object> map, String key) {
    if (!map.containsKey(key) || map.get(key) == null) {
      return null;
    }
    Object value = map.get(key);
    return value instanceof Number ? ((Number) value).doubleValue() :
      Double.valueOf(value.toString());
  }

  /**
   * Obtiene un valor integer de un mapa.
   *
   * @param map el mapa de valores
   * @param key la clave del valor
   * @return el valor como Integer o null si no existe
   */
  private Integer getIntegerValue(Map<String, Object> map, String key) {
    if (!map.containsKey(key) || map.get(key) == null) {
      return null;
    }
    Object value = map.get(key);
    return value instanceof Number ? ((Number) value).intValue() :
      Integer.valueOf(value.toString());
  }
}