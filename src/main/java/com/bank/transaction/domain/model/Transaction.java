package com.bank.transaction.domain.model;

import com.bank.transaction.model.ProductTypeEnum;
import com.bank.transaction.model.TransactionStatusEnum;
import com.bank.transaction.model.TransactionTypeEnum;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Modelo principal que representa una transacción financiera.
 * Contiene todos los datos necesarios para registrar y procesar transacciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
  private String id;
  private TransactionTypeEnum transactionType;
  private ProductTypeEnum productType;
  private String productId;
  private String customerId;
  private Double amount;
  private String description;
  private Double previousBalance;
  private Double newBalance;
  private LocalDateTime transactionDate;
  private TransactionStatusEnum status;
  private String transactionSubType;
  private String relatedAccountId;
  private Double commissionApplied;

  public void validate() {
    if (amount == null || amount <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    if (description == null || description.trim().isEmpty()) {
      throw new IllegalArgumentException("Description is required");
    }
  }
}