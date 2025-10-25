package com.bank.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductInfo {
    private String number;
    private String customerId;
}
