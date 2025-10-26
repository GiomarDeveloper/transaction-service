package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface TransactionRepositoryPort {
    Mono<Transaction> save(Transaction transaction);
    Flux<Transaction> findAll();
    Flux<Transaction> findByCustomerId(String customerId);
    Flux<Transaction> findByProductId(String productId);
    Flux<Transaction> findByCustomerIdAndProductType(String customerId, String productType);
    Mono<Transaction> findById(String id);
    Flux<Transaction> findByProductIdAndProductTypeAndCurrentMonth(String productId, String productType);
    Flux<Transaction> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
            Instant startDate,
            Instant endDate,
            Double minCommission
    );
    Flux<Transaction> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
            Instant startDate,
            Instant endDate,
            String productType,
            Double minCommission
    );
    Flux<Transaction> findByTransactionDateBetween(Instant startDate, Instant endDate);
    Flux<Transaction> findByCustomerIdAndTransactionDateBetween(String customerId, Instant startDate, Instant endDate);
    Flux<Transaction> findByProductTypeAndTransactionDateBetween(String productType, Instant startDate, Instant endDate);
    Flux<Transaction> findByProductIdAndTransactionDateBetween(String productId, Instant startDate, Instant endDate);
    Flux<Transaction> findByCustomerIdAndProductTypeAndTransactionDateBetween(
            String customerId, String productType, Instant startDate, Instant endDate);
    Flux<Transaction> findTop10ByProductIdAndProductTypeOrderByTransactionDateDesc(
            String productId, String productType);
}