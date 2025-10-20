package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.AccountInfo;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AccountServicePort {
    Mono<Map<String, Object>> updateAccountBalance(String accountId, Double amount, String transactionType);
    Mono<Boolean> validateAccountExists(String accountId);
    Mono<AccountInfo> getAccountInfo(String accountId);
}