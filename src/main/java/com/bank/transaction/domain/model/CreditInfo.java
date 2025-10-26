package com.bank.transaction.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditInfo {
    private Boolean exists;
    private String creditType;
    private String creditNumber;
    private Double availableCredit;
    private Double creditLimit;
    private Double outstandingBalance;
    private String status;
    private String customerId;

    private String mainAccountId;
    private List<AssociatedAccountInfo> associatedAccounts;
    private Double dailyWithdrawalLimit;
    private Double dailyPurchaseLimit;
    private String expirationDate;
    private String cardBrand;
    private String cardStatus;
}