package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.AccountInfo;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para servicios de cuentas.
 * Define las operaciones necesarias para interactuar con el servicio de cuentas.
 */
public interface AccountServicePort {

  /**
   * Actualiza el balance de una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a actualizar (positivo para depósito, negativo para retiro)
   * @param transactionType el tipo de transacción
   * @return Mono con el resultado de la actualización
   */
  Mono<Map<String, Object>> updateAccountBalance(String accountId, Double amount,
                                                 String transactionType);

  /**
   * Valida si una cuenta existe.
   *
   * @param accountId el ID de la cuenta a validar
   * @return Mono que emite true si la cuenta existe, false en caso contrario
   */
  Mono<Boolean> validateAccountExists(String accountId);

  /**
   * Obtiene información de una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @return Mono con la información de la cuenta
   */
  Mono<AccountInfo> getAccountInfo(String accountId);
}