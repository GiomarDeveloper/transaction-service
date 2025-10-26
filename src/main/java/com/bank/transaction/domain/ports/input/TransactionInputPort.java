package com.bank.transaction.domain.ports.input;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TransactionInputPort {
    Mono<Transaction> makeDeposit(String accountId, Double amount, String description);
    Mono<Transaction> makeWithdrawal(String accountId, Double amount, String description);
    Mono<Transaction> makeCreditPayment(String creditId, Double amount, String description);
    Mono<Transaction> makeCreditConsumption(String creditId, Double amount, String description, String merchant);
    Flux<Transaction> getAllTransactions(String customerId, String productType, String productId, String startDate, String endDate);
    Flux<Transaction> getTransactionsByAccount(String accountId);
    Flux<Transaction> getTransactionsByCredit(String creditId);
    Flux<Transaction> getTransactionsByCustomer(String customerId);
    Flux<Transaction> makeTransfer(TransferRequest transferRequest);
    Flux<Transaction> getProductTransactionsForCurrentMonth(String productId, String productType);
    Flux<CommissionReport> getCommissionsReport(LocalDate startDate, LocalDate endDate, String productType);
    Mono<TransactionResponse> makeThirdPartyCreditPayment(ThirdPartyCreditPaymentRequest request);
    Mono<LastMovementsResponse> getLast10MovementsReport(String productId, String productType);
}