package com.bank.transaction.mapper;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.model.TransactionResponse;
import com.bank.transaction.model.TransactionTypeEnum;
import com.bank.transaction.model.ProductTypeEnum;
import com.bank.transaction.model.TransactionStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface TransactionApiMapper {

    @Mapping(target = "transactionType", expression = "java(convertTransactionType(transaction.getTransactionType()))")
    @Mapping(target = "productType", expression = "java(convertProductType(transaction.getProductType()))")
    @Mapping(target = "status", expression = "java(convertStatus(transaction.getStatus()))")
    @Mapping(target = "transactionDate", expression = "java(convertToOffsetDateTime(transaction.getTransactionDate()))")
    TransactionResponse toResponse(Transaction transaction);

    default TransactionTypeEnum convertTransactionType(TransactionTypeEnum type) {
        return type != null ? TransactionTypeEnum.fromValue(type.getValue()) : null;
    }

    default ProductTypeEnum convertProductType(ProductTypeEnum type) {
        return type != null ? ProductTypeEnum.fromValue(type.getValue()) : null;
    }

    default TransactionStatusEnum convertStatus(TransactionStatusEnum status) {
        return status != null ? TransactionStatusEnum.fromValue(status.getValue()) : null;
    }

    default OffsetDateTime convertToOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }
}