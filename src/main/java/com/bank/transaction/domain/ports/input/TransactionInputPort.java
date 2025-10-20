package com.bank.transaction.domain.ports.input;

import com.bank.transaction.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionInputPort {
    Mono<Transaction> makeDeposit(String accountId, Double amount, String description);
    Mono<Transaction> makeWithdrawal(String accountId, Double amount, String description);
    Mono<Transaction> makeCreditPayment(String creditId, Double amount, String description);
    Mono<Transaction> makeCreditConsumption(String creditId, Double amount, String description, String merchant);
    Flux<Transaction> getAllTransactions(String customerId, String productType, String productId);
    Flux<Transaction> getTransactionsByAccount(String accountId);
    Flux<Transaction> getTransactionsByCredit(String creditId);
    Flux<Transaction> getTransactionsByCustomer(String customerId);
}