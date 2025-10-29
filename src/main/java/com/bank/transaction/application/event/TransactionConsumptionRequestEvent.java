package com.bank.transaction.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionConsumptionRequestEvent {
    private String consumptionId;
    private String creditId;
    private BigDecimal amount;
    private String description;
    private String merchant;
    private LocalDateTime transactionDate;
    private String source;
}