package com.bank.transaction.infrastructure.client;

import com.bank.transaction.domain.exception.ExternalServiceException;
import com.bank.transaction.domain.model.AssociatedAccountInfo;
import com.bank.transaction.domain.model.CreditInfo;
import com.bank.transaction.domain.ports.output.CreditServicePort;
import com.bank.transaction.model.ConsumptionRequest;
import com.bank.transaction.model.PaymentRequest;
import com.bank.transaction.model.ThirdPartyCreditPaymentRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para comunicación con el servicio de Créditos.
 * Implementa el puerto de salida CreditServicePort para interactuar con el microservicio de créditos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditServiceClient implements CreditServicePort {

  private final WebClient webClient;

  @Value("${external.services.credit.url:http://localhost:8083/}")
  private String creditServiceUrl;

  /**
   * Actualiza el balance de un crédito en el servicio de créditos.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a actualizar
   * @param transactionType el tipo de transacción
   * @return Mono con la respuesta del servicio de créditos
   */
  @Override
  public Mono<Map<String, Object>> updateCreditBalance(String creditId, Double amount,
                                                       String transactionType) {
    log.info("Updating credit balance - creditId: {}, amount: {}, type: {}", creditId, amount,
      transactionType);

    // Determinar qué endpoint usar basado en el tipo de transacción
    if ("PAGO_CREDITO".equals(transactionType)) {
      // Usar makePayment para pagos
      PaymentRequest paymentRequest = new PaymentRequest();
      paymentRequest.setAmount(Math.abs(amount));
      paymentRequest.setDescription("Payment from transaction service");
      paymentRequest.setPaymentDate(LocalDate.now());

      return webClient.post()
        .uri(creditServiceUrl + "/credits/{creditId}/payment", creditId)
        .bodyValue(paymentRequest)
        .retrieve()
        .onStatus(status -> status.isError(), response ->
          response.bodyToMono(String.class)
            .map(errorBody -> new ExternalServiceException(
              "Credit service error: " + response.statusCode() + " - " + errorBody
            ))
        )
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .onErrorResume(ex -> {
          log.error("Error calling credit service: {}", ex.getMessage());
          return Mono.error(
            new ExternalServiceException("Unable to process credit payment: " + ex.getMessage()));
        });

    } else if ("CONSUMO_TARJETA".equals(transactionType)) {
      // Usar chargeConsumption para consumos
      ConsumptionRequest consumptionRequest = new ConsumptionRequest();
      consumptionRequest.setAmount(Math.abs(amount));
      consumptionRequest.setDescription("Consumption from transaction service");
      consumptionRequest.setMerchant("Transaction System");
      consumptionRequest.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));

      return webClient.post()
        .uri(creditServiceUrl + "/credits/{creditId}/consumption", creditId)
        .bodyValue(consumptionRequest)
        .retrieve()
        .onStatus(status -> status.isError(), response ->
          response.bodyToMono(String.class)
            .map(errorBody -> new ExternalServiceException(
              "Credit service error: " + response.statusCode() + " - " + errorBody
            ))
        )
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .onErrorResume(ex -> {
          log.error("Error calling credit service: {}", ex.getMessage());
          return Mono.error(new ExternalServiceException(
            "Unable to process credit consumption: " + ex.getMessage()));
        });
    } else {
      return Mono.error(
        new ExternalServiceException("Unsupported transaction type: " + transactionType));
    }
  }

  /**
   * Valida si un crédito existe en el servicio de créditos.
   *
   * @param creditId el ID del crédito a validar
   * @return Mono que emite true si el crédito existe, false en caso contrario
   */
  @Override
  public Mono<Boolean> validateCreditExists(String creditId) {
    log.info("Validating credit existence: {}", creditId);

    return webClient.get()
      .uri(creditServiceUrl + "/credits/{creditId}", creditId)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        Mono.just(
          new ExternalServiceException("Credit validation failed: " + response.statusCode()))
      )
      .bodyToMono(Object.class) // Puede ser cualquier clase, solo nos importa el status
      .map(response -> true) // Si llega respuesta, existe
      .onErrorResume(ExternalServiceException.class, ex -> {
        log.warn("Credit validation failed: {}", ex.getMessage());
        return Mono.just(false);
      })
      .onErrorResume(ex -> {
        log.error("Error validating credit: {}", ex.getMessage());
        return Mono.just(false);
      })
      .defaultIfEmpty(false);
  }

  /**
   * Valida el límite disponible de un crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a validar
   * @return Mono que emite true si hay límite disponible, false en caso contrario
   */
  @Override
  public Mono<Boolean> validateCreditLimit(String creditId, Double amount) {
    log.info("Validating credit limit - creditId: {}, amount: {}", creditId, amount);

    return webClient.get()
      .uri(creditServiceUrl + "/credits/{creditId}/validate-limit?amount={amount}", creditId,
        amount)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        Mono.just(
          new ExternalServiceException("Credit limit validation failed: " + response.statusCode()))
      )
      .bodyToMono(Boolean.class)
      .defaultIfEmpty(false)
      .onErrorResume(ExternalServiceException.class, ex -> {
        log.warn("Credit limit validation failed: {}", ex.getMessage());
        return Mono.just(false);
      })
      .onErrorResume(ex -> {
        log.error("Error validating credit limit: {}", ex.getMessage());
        return Mono.just(false);
      });
  }

  /**
   * Obtiene información completa de un crédito desde el servicio de créditos.
   *
   * @param creditId el ID del crédito
   * @return Mono con la información del crédito
   */
  @Override
  public Mono<CreditInfo> getCreditInfo(String creditId) {
    log.info("Getting complete credit info for: {}", creditId);

    return webClient.get()
      .uri(creditServiceUrl + "/credits/{creditId}", creditId)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        Mono.error(
          new ExternalServiceException("Failed to get credit info: " + response.statusCode()))
      )
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
      })
      .map(responseMap -> {
        // Mapear manualmente desde el Map
        CreditInfo.CreditInfoBuilder builder = CreditInfo.builder()
          .exists(true)
          .creditType(getStringValue(responseMap, "creditType"))
          .availableCredit(getDoubleValue(responseMap, "availableCredit"))
          .creditLimit(getDoubleValue(responseMap, "creditLimit"))
          .outstandingBalance(getDoubleValue(responseMap, "outstandingBalance"))
          .status(getStringValue(responseMap, "status"))
          .creditNumber(getStringValue(responseMap, "creditNumber"))
          .customerId(getStringValue(responseMap, "customerId"));

        String creditType = getStringValue(responseMap, "creditType");
        if ("TARJETA_DEBITO".equals(creditType)) {
          // Campos específicos para tarjetas de débito
          builder
            .mainAccountId(getStringValue(responseMap, "mainAccountId"))
            .dailyWithdrawalLimit(getDoubleValue(responseMap, "dailyWithdrawalLimit"))
            .dailyPurchaseLimit(getDoubleValue(responseMap, "dailyPurchaseLimit"))
            .expirationDate(getStringValue(responseMap, "expirationDate"))
            .cardBrand(getStringValue(responseMap, "cardBrand"))
            .cardStatus(getStringValue(responseMap, "cardStatus"))
            .associatedAccounts(mapAssociatedAccounts(responseMap));
        }

        CreditInfo creditInfo = builder.build();


        return builder.build();
      })
      .onErrorResume(ex -> {
        log.error("Error getting credit info: {}", ex.getMessage());
        return Mono.just(CreditInfo.builder()
          .exists(false)
          .build());
      });
  }

  /**
   * Mapea las cuentas asociadas desde la respuesta del servicio.
   *
   * @param responseMap el mapa de respuesta
   * @return lista de cuentas asociadas
   */
  private List<AssociatedAccountInfo> mapAssociatedAccounts(Map<String, Object> responseMap) {
    Object associatedAccounts = responseMap.get("associatedAccounts");
    if (associatedAccounts instanceof List<?>) {
      List<Map<String, Object>> accountsList = (List<Map<String, Object>>) associatedAccounts;
      return accountsList.stream()
        .map(accountMap -> AssociatedAccountInfo.builder()
          .accountId(getStringValue(accountMap, "accountId"))
          .sequenceOrder(getIntegerValue(accountMap, "sequenceOrder"))
          .associatedAt(getStringValue(accountMap, "associatedAt"))
          .status(getStringValue(accountMap, "status"))
          .build())
        .collect(Collectors.toList());
    }
    return null;
  }

  /**
   * Realiza un pago de terceros a un crédito.
   *
   * @param creditId el ID del crédito
   * @param request la solicitud de pago de terceros
   * @return Mono con la respuesta del servicio de créditos
   */
  @Override
  public Mono<Map<String, Object>> makeThirdPartyPayment(String creditId,
                                                         ThirdPartyCreditPaymentRequest request) {
    log.info("Making third party payment - credit: {}, payer: {}, amount: {}",
      creditId, request.getPayerCustomerId(), request.getAmount());

    return webClient.post()
      .uri(creditServiceUrl + "/credits/{creditId}/third-party-payment", creditId)
      .bodyValue(request)
      .retrieve()
      .onStatus(status -> status.isError(), response ->
        response.bodyToMono(String.class)
          .flatMap(errorBody -> Mono.error(new ExternalServiceException(
            "Credit service error for third party payment: " + response.statusCode() + " - " +
              errorBody
          )))
      )
      .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
      })
      .doOnSuccess(response -> log.info("Third party payment successful for credit: {}", creditId))
      .doOnError(error -> log.error("Error making third party payment: {}", error.getMessage()));
  }

  /**
   * Obtiene un valor integer de un mapa.
   *
   * @param map el mapa de valores
   * @param key la clave del valor
   * @return el valor como Integer o null si no existe
   */
  private Integer getIntegerValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
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
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return Double.valueOf(value.toString());
  }
}