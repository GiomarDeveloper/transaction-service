package com.bank.transaction.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Modelo que representa el resultado de una operación de transacción.
 * Contiene información sobre el éxito o fracaso de la transacción procesada.
 */
@Data
@Builder
public class TransactionResult {
  private Boolean success;
  private String accountId;
  private String transactionId;
  private String message;
}