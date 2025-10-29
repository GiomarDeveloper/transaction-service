package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.CreditInfo;
import com.bank.transaction.model.ThirdPartyCreditPaymentRequest;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para servicios de créditos.
 * Define las operaciones necesarias para interactuar con el servicio de créditos.
 */
public interface CreditServicePort {

  /**
   * Actualiza el balance de un crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a actualizar
   * @param transactionType el tipo de transacción
   * @return Mono con el resultado de la actualización
   */
  Mono<Map<String, Object>> updateCreditBalance(String creditId, Double amount,
                                                String transactionType);

  /**
   * Valida si un crédito existe.
   *
   * @param creditId el ID del crédito a validar
   * @return Mono que emite true si el crédito existe, false en caso contrario
   */
  Mono<Boolean> validateCreditExists(String creditId);

  /**
   * Valida el límite disponible de un crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a validar
   * @return Mono que emite true si hay límite disponible, false en caso contrario
   */
  Mono<Boolean> validateCreditLimit(String creditId, Double amount);

  /**
   * Obtiene información de un crédito.
   *
   * @param creditId el ID del crédito
   * @return Mono con la información del crédito
   */
  Mono<CreditInfo> getCreditInfo(String creditId);

  /**
   * Realiza un pago de terceros a un crédito.
   *
   * @param creditId el ID del crédito
   * @param request la solicitud de pago de terceros
   * @return Mono con el resultado del pago
   */
  Mono<Map<String, Object>> makeThirdPartyPayment(String creditId,
                                                  ThirdPartyCreditPaymentRequest request);
}