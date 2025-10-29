package com.bank.transaction.infrastructure.repository.mongodb.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad de transacción para persistencia en MongoDB.
 * Representa el esquema de datos de transacciones en la base de datos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class TransactionEntity {
  @Id
  private String id;

  private String transactionType;

  private String productType;

  private String productId;

  private String customerId;

  private Double amount;

  private String description;

  private Double previousBalance;

  private Double newBalance;

  private LocalDateTime transactionDate;

  private String status;

  private LocalDateTime createdAt;

  private String transactionSubType;

  private String relatedAccountId;

  private Double commissionApplied;
}
