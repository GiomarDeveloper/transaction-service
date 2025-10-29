package com.bank.transaction.infrastructure.repository.mongodb.mapper;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para convertir entre entidades de dominio y entidades de persistencia.
 * Utiliza MapStruct para el mapeo automático entre Transaction y TransactionEntity.
 */
@Mapper(componentModel = "spring")
public interface TransactionEntityMapper {

  /**
   * Convierte Transaction a TransactionEntity.
   * Establece automáticamente la fecha de creación.
   *
   * @param transaction la transacción de dominio
   * @return la entidad de persistencia
   */
  @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
  TransactionEntity toEntity(Transaction transaction);

  /**
   * Convierte TransactionEntity a Transaction.
   *
   * @param entity la entidad de persistencia
   * @return la transacción de dominio
   */
  Transaction toDomain(TransactionEntity entity);
}