package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.CreditInfo;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface CreditServicePort {
    Mono<Map<String, Object>> updateCreditBalance(String creditId, Double amount, String transactionType);
    Mono<Boolean> validateCreditExists(String creditId);
    Mono<Boolean> validateCreditLimit(String creditId, Double amount);
    Mono<CreditInfo> getCreditInfo(String creditId);
}