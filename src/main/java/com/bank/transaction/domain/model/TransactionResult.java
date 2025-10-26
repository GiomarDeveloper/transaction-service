package com.bank.transaction.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResult {
    private Boolean success;
    private String accountId;
    private String transactionId;
    private String message;
}