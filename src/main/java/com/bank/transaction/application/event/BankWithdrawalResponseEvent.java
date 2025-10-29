package com.bank.transaction.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankWithdrawalResponseEvent {
    private String withdrawalId;
    private String requestId;
    private String transactionId;
    private String status; // COMPLETADO, RECHAZADO
    private Double previousBalance;
    private Double newBalance;
    private String message;
    private Long timestamp;
}