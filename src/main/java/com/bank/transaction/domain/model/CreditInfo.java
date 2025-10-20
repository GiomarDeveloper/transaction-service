package com.bank.transaction.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditInfo {
    private Boolean exists;
    private String creditType;
    private Double availableCredit;
    private Double creditLimit;
    private Double outstandingBalance;
    private String status;
}