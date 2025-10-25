package com.bank.transaction.infrastructure.repository.adapter;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.domain.ports.output.TransactionRepositoryPort;
import com.bank.transaction.infrastructure.repository.mongodb.TransactionMongoRepository;
import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import com.bank.transaction.infrastructure.repository.mongodb.mapper.TransactionEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionMongoRepository mongoRepository;
    private final TransactionEntityMapper mapper;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        TransactionEntity entity = mapper.toEntity(transaction);
        return mongoRepository.save(entity).map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findAll() {
        return mongoRepository.findAll().map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByCustomerId(String customerId) {
        return mongoRepository.findByCustomerId(customerId).map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByProductId(String productId) {
        return mongoRepository.findByProductId(productId).map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByCustomerIdAndProductType(String customerId, String productType) {
        return mongoRepository.findByCustomerIdAndProductType(customerId, productType).map(mapper::toDomain);
    }

    @Override
    public Mono<Transaction> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByProductIdAndProductTypeAndCurrentMonth(String productId, String productType) {
        return mongoRepository.findByProductIdAndProductTypeAndCurrentMonth(productId, productType)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
            Instant startDate, Instant endDate, Double minCommission) {
        return mongoRepository.findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
                        startDate, endDate, minCommission)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
            Instant startDate, Instant endDate, String productType, Double minCommission) {
        return mongoRepository.findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
                        startDate, endDate, productType, minCommission)
                .map(mapper::toDomain);
    }
}