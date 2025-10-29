package com.bank.transaction.domain.ports.output;

import com.bank.transaction.domain.model.Transaction;
import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para el repositorio de transacciones.
 * Define las operaciones de persistencia para transacciones.
 */
public interface TransactionRepositoryPort {

  /**
   * Guarda una transacción.
   *
   * @param transaction la transacción a guardar
   * @return Mono con la transacción guardada
   */
  Mono<Transaction> save(Transaction transaction);

  /**
   * Obtiene todas las transacciones.
   *
   * @return Flux con todas las transacciones
   */
  Flux<Transaction> findAll();

  /**
   * Obtiene transacciones por ID de cliente.
   *
   * @param customerId el ID del cliente
   * @return Flux con las transacciones del cliente
   */
  Flux<Transaction> findByCustomerId(String customerId);

  /**
   * Obtiene transacciones por ID de producto.
   *
   * @param productId el ID del producto
   * @return Flux con las transacciones del producto
   */
  Flux<Transaction> findByProductId(String productId);

  /**
   * Obtiene transacciones por cliente y tipo de producto.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByCustomerIdAndProductType(String customerId, String productType);

  /**
   * Obtiene una transacción por ID.
   *
   * @param id el ID de la transacción
   * @return Mono con la transacción encontrada
   */
  Mono<Transaction> findById(String id);

  /**
   * Obtiene transacciones del mes actual por producto y tipo.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones del mes actual
   */
  Flux<Transaction> findByProductIdAndProductTypeAndCurrentMonth(String productId,
                                                                 String productType);

  /**
   * Obtiene transacciones con comisión aplicada en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
    Instant startDate,
    Instant endDate,
    Double minCommission
  );

  /**
   * Obtiene transacciones con comisión aplicada por tipo de producto en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param productType el tipo de producto
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
    Instant startDate,
    Instant endDate,
    String productType,
    Double minCommission
  );

  /**
   * Obtiene transacciones en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones en el rango
   */
  Flux<Transaction> findByTransactionDateBetween(Instant startDate, Instant endDate);

  /**
   * Obtiene transacciones por cliente en un rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByCustomerIdAndTransactionDateBetween(String customerId, Instant startDate,
                                                              Instant endDate);

  /**
   * Obtiene transacciones por tipo de producto en un rango de fechas.
   *
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByProductTypeAndTransactionDateBetween(String productType,
                                                               Instant startDate, Instant endDate);

  /**
   * Obtiene transacciones por producto en un rango de fechas.
   *
   * @param productId el ID del producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByProductIdAndTransactionDateBetween(String productId, Instant startDate,
                                                             Instant endDate);

  /**
   * Obtiene transacciones por cliente, tipo de producto y rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> findByCustomerIdAndProductTypeAndTransactionDateBetween(
    String customerId, String productType, Instant startDate, Instant endDate);

  /**
   * Obtiene las últimas 10 transacciones de un producto ordenadas por fecha descendente.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las últimas 10 transacciones
   */
  Flux<Transaction> findTop10ByProductIdAndProductTypeOrderByTransactionDateDesc(
    String productId, String productType);
}