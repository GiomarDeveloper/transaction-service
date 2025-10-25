package com.bank.transaction.infrastructure.repository.mongodb;

import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Repository
public interface TransactionMongoRepository extends ReactiveMongoRepository<TransactionEntity, String> {
    Flux<TransactionEntity> findByCustomerId(String customerId);
    Flux<TransactionEntity> findByProductId(String productId);
    Flux<TransactionEntity> findByCustomerIdAndProductType(String customerId, String productType);
    Flux<TransactionEntity> findByTransactionType(String transactionType);
    Flux<TransactionEntity> findByStatus(String status);
    Flux<TransactionEntity> findByProductIdAndProductTypeAndTransactionDateBetween(
            String productId,
            String productType,
            Instant startDate,
            Instant endDate
    );
    default Flux<TransactionEntity> findByProductIdAndProductTypeAndCurrentMonth(String productId, String productType) {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = now.plusMonths(1).withDayOfMonth(1);

        Instant startDate = firstDayOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = firstDayOfNextMonth.atStartOfDay(ZoneOffset.UTC).toInstant();

        return findByProductIdAndProductTypeAndTransactionDateBetween(
                productId,
                productType,
                startDate,
                endDate
        );
    }
    Flux<TransactionEntity> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
            Instant startDate,
            Instant endDate,
            Double minCommission
    );

    Flux<TransactionEntity> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
            Instant startDate,
            Instant endDate,
            String productType,
            Double minCommission
    );
}