package com.bank.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo que representa la información de una cuenta bancaria.
 * Contiene los datos necesarios para realizar operaciones de transacción.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo {
  private Boolean exists;
  private String accountNumber;
  private String accountType;
  private String customerId;
  private Double balance;
  private Integer monthlyTransactionLimit;
  private Integer currentMonthTransactions;
  private String status;
}