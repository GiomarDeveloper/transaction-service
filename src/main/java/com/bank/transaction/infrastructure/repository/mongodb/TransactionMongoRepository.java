package com.bank.transaction.infrastructure.repository.mongodb;

import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransactionMongoRepository extends ReactiveMongoRepository<TransactionEntity, String> {
    Flux<TransactionEntity> findByCustomerId(String customerId);
    Flux<TransactionEntity> findByProductId(String productId);
    Flux<TransactionEntity> findByCustomerIdAndProductType(String customerId, String productType);
    Flux<TransactionEntity> findByTransactionType(String transactionType);
    Flux<TransactionEntity> findByStatus(String status);
}