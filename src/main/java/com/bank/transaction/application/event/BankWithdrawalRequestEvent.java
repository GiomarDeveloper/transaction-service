package com.bank.transaction.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankWithdrawalRequestEvent {
    private String withdrawalId;
    private String requestId; // ID de la transacción Bootcoin
    private String accountId;
    private Double amount;
    private String description;
    private String currency;
    private Long timestamp;
}