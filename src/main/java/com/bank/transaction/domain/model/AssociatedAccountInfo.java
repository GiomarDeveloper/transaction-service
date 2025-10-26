package com.bank.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedAccountInfo {
    private String accountId;
    private Integer sequenceOrder;
    private String associatedAt;
    private String status;
}