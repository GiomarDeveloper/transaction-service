package com.bank.transaction.infrastructure.repository.adapter;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.domain.ports.output.TransactionRepositoryPort;
import com.bank.transaction.infrastructure.repository.mongodb.TransactionMongoRepository;
import com.bank.transaction.infrastructure.repository.mongodb.entity.TransactionEntity;
import com.bank.transaction.infrastructure.repository.mongodb.mapper.TransactionEntityMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador del repositorio de transacciones.
 * Implementa el puerto de salida TransactionRepositoryPort usando MongoDB.
 */
@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

  private final TransactionMongoRepository mongoRepository;
  private final TransactionEntityMapper mapper;

  /**
   * Guarda una transacción en la base de datos.
   *
   * @param transaction la transacción a guardar
   * @return Mono con la transacción guardada
   */
  @Override
  public Mono<Transaction> save(Transaction transaction) {
    TransactionEntity entity = mapper.toEntity(transaction);
    return mongoRepository.save(entity).map(mapper::toDomain);
  }

  /**
   * Obtiene todas las transacciones.
   *
   * @return Flux con todas las transacciones
   */
  @Override
  public Flux<Transaction> findAll() {
    return mongoRepository.findAll().map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por ID de cliente.
   *
   * @param customerId el ID del cliente
   * @return Flux con las transacciones del cliente
   */
  @Override
  public Flux<Transaction> findByCustomerId(String customerId) {
    return mongoRepository.findByCustomerId(customerId).map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por ID de producto.
   *
   * @param productId el ID del producto
   * @return Flux con las transacciones del producto
   */
  @Override
  public Flux<Transaction> findByProductId(String productId) {
    return mongoRepository.findByProductId(productId).map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por cliente y tipo de producto.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByCustomerIdAndProductType(String customerId, String productType) {
    return mongoRepository.findByCustomerIdAndProductType(customerId, productType)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene una transacción por ID.
   *
   * @param id el ID de la transacción
   * @return Mono con la transacción encontrada
   */
  @Override
  public Mono<Transaction> findById(String id) {
    return mongoRepository.findById(id).map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones del mes actual por producto y tipo.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones del mes actual
   */
  @Override
  public Flux<Transaction> findByProductIdAndProductTypeAndCurrentMonth(String productId,
                                                                        String productType) {
    return mongoRepository.findByProductIdAndProductTypeAndCurrentMonth(productId, productType)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones con comisión aplicada en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
    Instant startDate, Instant endDate, Double minCommission) {
    return mongoRepository.findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
        startDate, endDate, minCommission)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones con comisión aplicada por tipo de producto en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @param productType el tipo de producto
   * @param minCommission la comisión mínima aplicada
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
    Instant startDate, Instant endDate, String productType, Double minCommission) {
    return mongoRepository.findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
        startDate, endDate, productType, minCommission)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones en un rango de fechas.
   *
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones en el rango
   */
  @Override
  public Flux<Transaction> findByTransactionDateBetween(Instant startDate, Instant endDate) {
    return mongoRepository.findByTransactionDateBetween(startDate, endDate).map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por cliente en un rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByCustomerIdAndTransactionDateBetween(String customerId,
                                                                     Instant startDate,
                                                                     Instant endDate) {
    return mongoRepository.findByCustomerIdAndTransactionDateBetween(customerId, startDate, endDate)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por tipo de producto en un rango de fechas.
   *
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByProductTypeAndTransactionDateBetween(String productType,
                                                                      Instant startDate,
                                                                      Instant endDate) {
    return mongoRepository.findByProductTypeAndTransactionDateBetween(productType, startDate,
      endDate).map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por producto en un rango de fechas.
   *
   * @param productId el ID del producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByProductIdAndTransactionDateBetween(String productId,
                                                                    Instant startDate,
                                                                    Instant endDate) {
    return mongoRepository.findByProductIdAndTransactionDateBetween(productId, startDate, endDate)
      .map(mapper::toDomain);
  }

  /**
   * Obtiene transacciones por cliente, tipo de producto y rango de fechas.
   *
   * @param customerId el ID del cliente
   * @param productType el tipo de producto
   * @param startDate la fecha de inicio
   * @param endDate la fecha de fin
   * @return Flux con las transacciones filtradas
   */
  @Override
  public Flux<Transaction> findByCustomerIdAndProductTypeAndTransactionDateBetween(
    String customerId, String productType, Instant startDate, Instant endDate) {
    return mongoRepository.findByCustomerIdAndProductTypeAndTransactionDateBetween(
      customerId, productType, startDate, endDate).map(mapper::toDomain);
  }

  /**
   * Obtiene las últimas 10 transacciones de un producto ordenadas por fecha descendente.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las últimas 10 transacciones
   */
  @Override
  public Flux<Transaction> findTop10ByProductIdAndProductTypeOrderByTransactionDateDesc(
    String productId, String productType) {

    return mongoRepository.findByProductIdAndProductTypeOrderByTransactionDateDesc(productId,
        productType)
      .take(10)  // Limitar a 10 resultados
      .map(mapper::toDomain);
  }
}