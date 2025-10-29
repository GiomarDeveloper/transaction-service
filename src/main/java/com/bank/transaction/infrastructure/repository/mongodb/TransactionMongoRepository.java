package com.bank.transaction.infrastructure.repository.mongodb;

import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


/**
 * Repositorio reactivo para operaciones de base de datos de transacciones.
 * Extiende ReactiveMongoRepository para operaciones CRUD reactivas con MongoDB.
 */
@Repository
public interface TransactionMongoRepository
  extends ReactiveMongoRepository<TransactionEntity, String> {

  /**
   * Busca transacciones por ID de cliente.
   *
   * @param customerId el ID del cliente
   * @return Flux con las transacciones del cliente
   */
  Flux<TransactionEntity> findByCustomerId(String customerId);

  /**
   * Busca transacciones por ID de producto.
   *
   * @param productId el ID del producto
   * @return Flux con las transacciones del producto
   */
  Flux<TransactionEntity> findByProductId(String productId);

  /**
   * Busca transacciones por cliente y tipo de producto.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @return Flux con las transacciones filtradas
   */
  Flux<TransactionEntity> findByCustomerIdAndProductType(String customerId, String productType);

  /**
   * Busca transacciones por tipo de transacción.
   *
   * @param transactionType el tipo de transacción
   * @return Flux con las transacciones del tipo especificado
   */
  Flux<TransactionEntity> findByTransactionType(String transactionType);

  /**
   * Busca transacciones por estado.
   *
   * @param status el estado de la transacción
   * @return Flux con las transacciones del estado especificado
   */
  Flux<TransactionEntity> findByStatus(String status);

  /**
   * Busca transacciones por producto, tipo y rango de fechas.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  Flux<TransactionEntity> findByProductIdAndProductTypeAndTransactionDateBetween(
    String productId,
    String productType,
    Instant startDate,
    Instant endDate
  );

  /**
   * Busca transacciones del mes actual por producto y tipo.
   * Método por defecto que calcula automáticamente las fechas del mes actual.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones del mes actual
   */
  default Flux<TransactionEntity> findByProductIdAndProductTypeAndCurrentMonth(String productId,
                                                                               String productType) {
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

  /**
   * Busca transacciones con comisión aplicada en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones que tienen comisión mayor al mínimo
   */
  Flux<TransactionEntity> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
    Instant startDate,
    Instant endDate,
    Double minCommission
  );

  /**
   * Busca transacciones con comisión aplicada por tipo de producto en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param productType el tipo de producto
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones filtradas
   */
  Flux<TransactionEntity> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
    Instant startDate,
    Instant endDate,
    String productType,
    Double minCommission
  );

  /**
   * Busca transacciones en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones en el rango especificado
   */
  Flux<TransactionEntity> findByTransactionDateBetween(Instant startDate, Instant endDate);

  /**
   * Busca transacciones por cliente en un rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones del cliente en el rango de fechas
   */
  Flux<TransactionEntity> findByCustomerIdAndTransactionDateBetween(String customerId,
                                                                    Instant startDate,
                                                                    Instant endDate);

  /**
   * Busca transacciones por tipo de producto en un rango de fechas.
   *
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones del tipo de producto en el rango de fechas
   */
  Flux<TransactionEntity> findByProductTypeAndTransactionDateBetween(String productType,
                                                                     Instant startDate,
                                                                     Instant endDate);

  /**
   * Busca transacciones por producto en un rango de fechas.
   *
   * @param productId el ID del producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones del producto en el rango de fechas
   */
  Flux<TransactionEntity> findByProductIdAndTransactionDateBetween(String productId,
                                                                   Instant startDate,
                                                                   Instant endDate);

  /**
   * Busca transacciones por cliente, tipo de producto y rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones que cumplen todos los criterios
   */
  Flux<TransactionEntity> findByCustomerIdAndProductTypeAndTransactionDateBetween(
    String customerId, String productType, Instant startDate, Instant endDate);

  /**
   * Busca transacciones por producto y tipo, ordenadas por fecha descendente.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones ordenadas por fecha descendente
   */
  Flux<TransactionEntity> findByProductIdAndProductTypeOrderByTransactionDateDesc(
    String productId, String productType);
}