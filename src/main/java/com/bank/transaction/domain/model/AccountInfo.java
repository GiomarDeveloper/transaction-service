package com.bank.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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