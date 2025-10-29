package com.bank.transaction.mapper;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.model.ProductTypeEnum;
import com.bank.transaction.model.TransactionResponse;
import com.bank.transaction.model.TransactionStatusEnum;
import com.bank.transaction.model.TransactionTypeEnum;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para convertir entre modelos de dominio y DTOs de API.
 * Utiliza MapStruct para el mapeo automático entre Transaction y TransactionResponse.
 */
@Mapper(componentModel = "spring")
public interface TransactionApiMapper {

  /**
   * Convierte una transacción de dominio a TransactionResponse para la API.
   *
   * @param transaction la transacción de dominio
   * @return TransactionResponse para la API
   */
  @Mapping(target = "transactionType", expression = "java(convertTransactionType(transaction.getTransactionType()))")
  @Mapping(target = "productType", expression = "java(convertProductType(transaction.getProductType()))")
  @Mapping(target = "status", expression = "java(convertStatus(transaction.getStatus()))")
  @Mapping(target = "transactionDate", expression = "java(convertToOffsetDateTime(transaction.getTransactionDate()))")
  TransactionResponse toResponse(Transaction transaction);

  /**
   * Convierte TransactionTypeEnum de dominio a TransactionTypeEnum de API.
   * Garantiza la compatibilidad entre las enumeraciones de diferentes capas.
   *
   * @param type el tipo de transacción de dominio
   * @return el tipo de transacción de API
   */
  default TransactionTypeEnum convertTransactionType(TransactionTypeEnum type) {
    return type != null ? TransactionTypeEnum.fromValue(type.getValue()) : null;
  }

  /**
   * Convierte ProductTypeEnum de dominio a ProductTypeEnum de API.
   * Garantiza la compatibilidad entre las enumeraciones de diferentes capas.
   *
   * @param type el tipo de producto de dominio
   * @return el tipo de producto de API
   */
  default ProductTypeEnum convertProductType(ProductTypeEnum type) {
    return type != null ? ProductTypeEnum.fromValue(type.getValue()) : null;
  }

  /**
   * Convierte TransactionStatusEnum de dominio a TransactionStatusEnum de API.
   * Garantiza la compatibilidad entre las enumeraciones de diferentes capas.
   *
   * @param status el estado de transacción de dominio
   * @return el estado de transacción de API
   */
  default TransactionStatusEnum convertStatus(TransactionStatusEnum status) {
    return status != null ? TransactionStatusEnum.fromValue(status.getValue()) : null;
  }

  /**
   * Convierte LocalDateTime a OffsetDateTime con zona horaria UTC.
   * Utilizado para estandarizar las fechas en las respuestas de la API.
   *
   * @param localDateTime la fecha y hora local
   * @return OffsetDateTime en UTC
   */
  default OffsetDateTime convertToOffsetDateTime(LocalDateTime localDateTime) {
    return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
  }
}