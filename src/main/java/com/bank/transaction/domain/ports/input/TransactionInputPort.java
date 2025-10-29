package com.bank.transaction.domain.ports.input;

import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.model.*;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de entrada para operaciones de transacciones.
 * Define los casos de uso principales del servicio de transacciones.
 */
public interface TransactionInputPort {

  /**
   * Realiza un depósito en una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a depositar
   * @param description la descripción del depósito
   * @return Mono con la transacción realizada
   */
  Mono<Transaction> makeDeposit(String accountId, Double amount, String description);

  /**
   * Realiza un retiro de una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a retirar
   * @param description la descripción del retiro
   * @return Mono con la transacción realizada
   */
  Mono<Transaction> makeWithdrawal(String accountId, Double amount, String description);

  /**
   * Realiza un pago a un crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a pagar
   * @param description la descripción del pago
   * @return Mono con la transacción realizada
   */
  Mono<Transaction> makeCreditPayment(String creditId, Double amount, String description);

  /**
   * Realiza un consumo con tarjeta de crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto del consumo
   * @param description la descripción del consumo
   * @param merchant el comercio donde se realiza el consumo
   * @return Mono con la transacción realizada
   */
  Mono<Transaction> makeCreditConsumption(String creditId, Double amount, String description,
                                          String merchant);

  /**
   * Obtiene transacciones con filtros opcionales.
   *
   * @param customerId el ID del cliente (opcional)
   * @param productType el tipo de producto (opcional)
   * @param productId el ID del producto (opcional)
   * @param startDate la fecha de inicio (opcional)
   * @param endDate la fecha de fin (opcional)
   * @return Flux con las transacciones filtradas
   */
  Flux<Transaction> getAllTransactions(String customerId, String productType, String productId,
                                       String startDate, String endDate);

  /**
   * Obtiene transacciones por cuenta específica.
   *
   * @param accountId el ID de la cuenta
   * @return Flux con las transacciones de la cuenta
   */
  Flux<Transaction> getTransactionsByAccount(String accountId);

  /**
   * Obtiene transacciones por crédito específico.
   *
   * @param creditId el ID del crédito
   * @return Flux con las transacciones del crédito
   */
  Flux<Transaction> getTransactionsByCredit(String creditId);

  /**
   * Obtiene transacciones por cliente específico.
   *
   * @param customerId el ID del cliente
   * @return Flux con las transacciones del cliente
   */
  Flux<Transaction> getTransactionsByCustomer(String customerId);

  /**
   * Realiza una transferencia entre cuentas.
   *
   * @param transferRequest la solicitud de transferencia
   * @return Flux con las transacciones de transferencia (origen y destino)
   */
  Flux<Transaction> makeTransfer(TransferRequest transferRequest);

  /**
   * Obtiene transacciones del mes actual para un producto específico.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones del mes actual
   */
  Flux<Transaction> getProductTransactionsForCurrentMonth(String productId, String productType);

  /**
   * Genera un reporte de comisiones por período.
   *
   * @param startDate la fecha de inicio del reporte
   * @param endDate la fecha de fin del reporte
   * @param productType el tipo de producto (opcional)
   * @return Flux con el reporte de comisiones
   */
  Flux<CommissionReport> getCommissionsReport(LocalDate startDate, LocalDate endDate,
                                              String productType);

  /**
   * Realiza un pago de crédito desde terceros.
   *
   * @param request la solicitud de pago de terceros
   * @return Mono con la respuesta de la transacción
   */
  Mono<TransactionResponse> makeThirdPartyCreditPayment(ThirdPartyCreditPaymentRequest request);

  /**
   * Obtiene los últimos 10 movimientos de un producto.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Mono con los últimos movimientos
   */
  Mono<LastMovementsResponse> getLast10MovementsReport(String productId, String productType);
}