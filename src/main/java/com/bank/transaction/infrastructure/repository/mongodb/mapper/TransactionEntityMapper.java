package com.bank.transaction.infrastructure.repository.mongodb.mapper;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionEntityMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    TransactionEntity toEntity(Transaction transaction);

    Transaction toDomain(TransactionEntity entity);
}