package com.bank.transaction.domain.model;

import com.bank.transaction.model.ProductTypeEnum;
import com.bank.transaction.model.TransactionStatusEnum;
import com.bank.transaction.model.TransactionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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

    public void validate() {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
    }
}