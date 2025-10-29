package com.bank.transaction.domain.service.impl;

import com.bank.transaction.domain.exception.ExternalServiceException;
import com.bank.transaction.domain.exception.InsufficientFundsException;
import com.bank.transaction.domain.exception.TransactionException;
import com.bank.transaction.domain.model.AccountInfo;
import com.bank.transaction.domain.model.AssociatedAccountInfo;
import com.bank.transaction.domain.model.CreditInfo;
import com.bank.transaction.domain.model.ProductInfo;
import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.domain.model.TransactionResult;
import com.bank.transaction.domain.ports.input.TransactionInputPort;
import com.bank.transaction.domain.ports.output.AccountServicePort;
import com.bank.transaction.domain.ports.output.CreditServicePort;
import com.bank.transaction.domain.ports.output.TransactionRepositoryPort;
import com.bank.transaction.model.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementación del servicio de transacciones.
 * Maneja todas las operaciones relacionadas con transacciones financieras.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionInputPort {

  private final TransactionRepositoryPort transactionRepositoryPort;
  private final AccountServicePort accountServicePort;
  private final CreditServicePort creditServicePort;

  /**
   * Realiza un depósito en una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a depositar
   * @param description la descripción del depósito
   * @return Mono con la transacción realizada
   */
  @Override
  public Mono<Transaction> makeDeposit(String accountId, Double amount, String description) {
    return validateRequest(accountId, amount, description)
      .then(accountServicePort.getAccountInfo(accountId))
      .flatMap(accountInfo -> {
        // Validar existencia
        if (!accountInfo.getExists()) {
          return Mono.error(new TransactionException("Account not found: " + accountId));
        }

        // Validar que esté activa
        if (!"ACTIVO".equals(accountInfo.getStatus())) {
          return Mono.error(
            new TransactionException("Account is not active. Status: " + accountInfo.getStatus()));
        }

        // Validar límites de transacción mensual si aplica
        if (shouldValidateTransactionLimit(accountInfo)) {
          if (accountInfo.getCurrentMonthTransactions() >=
            accountInfo.getMonthlyTransactionLimit()) {
            return Mono.error(new TransactionException(
              "Monthly transaction limit exceeded. Limit: " +
                accountInfo.getMonthlyTransactionLimit() +
                ", Current: " + accountInfo.getCurrentMonthTransactions()
            ));
          }
        }

        return processAccountTransaction(accountId, amount, description, "DEPOSITO", null, null);
      })
      .onErrorResume(ex -> createFailedTransaction(
        accountId, amount, description, "DEPOSITO", "CUENTA", ex.getMessage()));
  }

  /**
   * Realiza un retiro de una cuenta.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto a retirar
   * @param description la descripción del retiro
   * @return Mono con la transacción realizada
   */
  @Override
  public Mono<Transaction> makeWithdrawal(String accountId, Double amount, String description) {
    return validateRequest(accountId, amount, description)
      .then(accountServicePort.getAccountInfo(accountId))
      .flatMap(accountInfo -> {
        // Validar existencia
        if (!accountInfo.getExists()) {
          return Mono.error(new TransactionException("Account not found: " + accountId));
        }

        // Validar que esté activa
        if (!"ACTIVO".equals(accountInfo.getStatus())) {
          return Mono.error(
            new TransactionException("Account is not active. Status: " + accountInfo.getStatus()));
        }

        // Validar fondos suficientes para retiro
        if (accountInfo.getBalance() < amount) {
          return Mono.error(new InsufficientFundsException(
            "Insufficient funds. Available: " + accountInfo.getBalance() + ", Requested: " + amount
          ));
        }

        // Validar límites de transacción mensual si aplica
        if (shouldValidateTransactionLimit(accountInfo)) {
          if (accountInfo.getCurrentMonthTransactions() >=
            accountInfo.getMonthlyTransactionLimit()) {
            return Mono.error(new TransactionException(
              "Monthly transaction limit exceeded. Limit: " +
                accountInfo.getMonthlyTransactionLimit() +
                ", Current: " + accountInfo.getCurrentMonthTransactions()
            ));
          }
        }

        return processAccountTransaction(accountId, amount, description, "RETIRO", null, null);
      })
      .onErrorResume(ex -> createFailedTransaction(
        accountId, amount, description, "RETIRO", "CUENTA", ex.getMessage()));
  }

  /**
   * Realiza un pago a un crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto a pagar
   * @param description la descripción del pago
   * @return Mono con la transacción realizada
   */
  @Override
  public Mono<Transaction> makeCreditPayment(String creditId, Double amount, String description) {
    return validateRequest(creditId, amount, description)
      .then(creditServicePort.getCreditInfo(creditId))
      .flatMap(creditInfo -> {
        if (!creditInfo.getExists()) {
          return Mono.error(new TransactionException("Credit not found: " + creditId));
        }

        if (!"ACTIVO".equals(creditInfo.getStatus())) {
          return Mono.error(
            new TransactionException("Credit is not active. Status: " + creditInfo.getStatus()));
        }

        if ("TARJETA_DEBITO".equals(creditInfo.getCreditType())) {
          return processDebitCardPayment(creditInfo, amount, description);
        }

        // Para PAGO: permitir tanto PRESTAMO_PERSONAL como TARJETA_CREDITO
        // No necesitamos validar tipo específico para pagos

        return processCreditTransaction(creditId, amount, description, "PAGO_CREDITO");
      })
      .onErrorResume(ex -> createFailedTransaction(
        creditId, amount, description, "PAGO_CREDITO", "CREDITO", ex.getMessage()));
  }

  /**
   * Realiza un consumo con tarjeta de crédito.
   *
   * @param creditId el ID del crédito
   * @param amount el monto del consumo
   * @param description la descripción del consumo
   * @param merchant el comercio donde se realiza el consumo
   * @return Mono con la transacción realizada
   */
  @Override
  public Mono<Transaction> makeCreditConsumption(String creditId, Double amount, String description,
                                                 String merchant) {
    return validateRequest(creditId, amount, description)
      .then(creditServicePort.getCreditInfo(creditId))
      .flatMap(creditInfo -> {
        // Validar existencia
        if (!creditInfo.getExists()) {
          return Mono.error(new TransactionException("Credit not found: " + creditId));
        }

        // Validar que esté activo
        if (!"ACTIVO".equals(creditInfo.getStatus())) {
          return Mono.error(
            new TransactionException("Credit is not active. Status: " + creditInfo.getStatus()));
        }

        if ("TARJETA_DEBITO".equals(creditInfo.getCreditType())) {
          return processDebitCardDeposit(creditInfo, amount, description, merchant);
        }

        // Validar tipo de crédito - SOLO tarjetas para consumo
        if (!"TARJETA_CREDITO".equals(creditInfo.getCreditType())) {
          return Mono.error(new TransactionException(
            "Credit consumption only allowed for credit cards. Credit type: " +
              creditInfo.getCreditType()
          ));
        }

        // Validar límite disponible
        if (creditInfo.getAvailableCredit() == null || creditInfo.getAvailableCredit() < amount) {
          return Mono.error(new InsufficientFundsException(
            "Credit limit exceeded. Available: " + creditInfo.getAvailableCredit() +
              ", Requested: " + amount
          ));
        }

        return processCreditTransaction(creditId, -amount, description + " - " + merchant,
          "CONSUMO_TARJETA");
      })
      .onErrorResume(ex -> createFailedTransaction(
        creditId, amount, description, "CONSUMO_TARJETA", "CREDITO", ex.getMessage()));
  }

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
  @Override
  public Flux<Transaction> getAllTransactions(String customerId, String productType,
                                              String productId, String startDate, String endDate) {
    log.info(
      "Getting transactions - customer: {}, productType: {}, productId: {}, startDate: {}, endDate: {}",
      customerId, productType, productId, startDate, endDate);

    // Si hay fechas, priorizar búsqueda por rango de fechas
    if (startDate != null && endDate != null) {
      Instant startInstant = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant endInstant =
        LocalDate.parse(endDate).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

      if (customerId != null && productType != null) {
        return transactionRepositoryPort.findByCustomerIdAndProductTypeAndTransactionDateBetween(
          customerId, productType, startInstant, endInstant);
      } else if (customerId != null) {
        return transactionRepositoryPort.findByCustomerIdAndTransactionDateBetween(customerId,
          startInstant, endInstant);
      } else if (productType != null) {
        return transactionRepositoryPort.findByProductTypeAndTransactionDateBetween(productType,
          startInstant, endInstant);
      } else if (productId != null) {
        return transactionRepositoryPort.findByProductIdAndTransactionDateBetween(productId,
          startInstant, endInstant);
      } else {
        return transactionRepositoryPort.findByTransactionDateBetween(startInstant, endInstant);
      }
    }

    if (customerId != null && productType != null) {
      return transactionRepositoryPort.findByCustomerIdAndProductType(customerId, productType);
    } else if (customerId != null) {
      return transactionRepositoryPort.findByCustomerId(customerId);
    } else if (productId != null) {
      return transactionRepositoryPort.findByProductId(productId);
    }
    return transactionRepositoryPort.findAll();
  }

  /**
   * Obtiene transacciones por cuenta específica.
   *
   * @param accountId el ID de la cuenta
   * @return Flux con las transacciones de la cuenta
   */
  @Override
  public Flux<Transaction> getTransactionsByAccount(String accountId) {
    return transactionRepositoryPort.findByProductId(accountId);
  }

  /**
   * Obtiene transacciones por crédito específico.
   *
   * @param creditId el ID del crédito
   * @return Flux con las transacciones del crédito
   */
  @Override
  public Flux<Transaction> getTransactionsByCredit(String creditId) {
    return transactionRepositoryPort.findByProductId(creditId);
  }

  /**
   * Obtiene transacciones por cliente específico.
   *
   * @param customerId el ID del cliente
   * @return Flux con las transacciones del cliente
   */
  @Override
  public Flux<Transaction> getTransactionsByCustomer(String customerId) {
    return transactionRepositoryPort.findByCustomerId(customerId);
  }

  /**
   * Realiza una transferencia entre cuentas.
   *
   * @param transferRequest la solicitud de transferencia
   * @return Flux con las transacciones de transferencia (origen y destino)
   */
  @Override
  public Flux<Transaction> makeTransfer(TransferRequest transferRequest) {
    return validateTransferRequest(transferRequest)
      .then(Mono.zip(
        accountServicePort.getAccountInfo(transferRequest.getFromAccountId()),
        accountServicePort.getAccountInfo(transferRequest.getToAccountId())
      ))
      .flatMapMany(
        accounts -> processTransferValidation(transferRequest, accounts.getT1(), accounts.getT2()))
      .doOnError(ex -> {
        if (isProcessingError(ex)) {
          log.error("Processing error during transfer - From: {}, To: {}, Amount: {}, Error: {}",
            transferRequest.getFromAccountId(),
            transferRequest.getToAccountId(),
            transferRequest.getAmount(),
            ex.getMessage());
        } else {
          log.warn("Validation error in transfer - From: {}, To: {}, Amount: {}, Error: {}",
            transferRequest.getFromAccountId(),
            transferRequest.getToAccountId(),
            transferRequest.getAmount(),
            ex.getMessage());
        }
      });
  }

  /**
   * Obtiene transacciones del mes actual para un producto específico.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Flux con las transacciones del mes actual
   */
  @Override
  public Flux<Transaction> getProductTransactionsForCurrentMonth(String productId,
                                                                 String productType) {
    log.info("Getting transactions for product: {} of type: {} in current month", productId,
      productType);

    return transactionRepositoryPort.findByProductIdAndProductTypeAndCurrentMonth(productId,
        productType)
      .onErrorResume(ex -> {
        log.error("Error getting transactions for product {} (type: {}): {}",
          productId, productType, ex.getMessage());
        return Flux.empty();
      });
  }

  /**
   * Genera un reporte de comisiones por período.
   *
   * @param startDate la fecha de inicio del reporte
   * @param endDate la fecha de fin del reporte
   * @param productType el tipo de producto (opcional)
   * @return Flux con el reporte de comisiones
   */
  @Override
  public Flux<CommissionReport> getCommissionsReport(LocalDate startDate, LocalDate endDate,
                                                     String productType) {
    log.info("Generating commissions report from {} to {}, productType: {}", startDate, endDate,
      productType);

    Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Double minCommission = 0.01;

    Flux<Transaction> transactions;
    if (productType != null) {
      transactions = transactionRepositoryPort
        .findByTransactionDateBetweenAndProductTypeAndCommissionAppliedGreaterThan(
          startInstant, endInstant, productType, minCommission);
    } else {
      transactions = transactionRepositoryPort
        .findByTransactionDateBetweenAndCommissionAppliedGreaterThan(
          startInstant, endInstant, minCommission);
    }

    // Agrega logging para debug
    return transactions
      .doOnNext(tx -> log.info("Transaction found: id={}, commission={}, productId={}",
        tx.getId(), tx.getCommissionApplied(), tx.getProductId()))
      .groupBy(Transaction::getProductId)
      .flatMap(group -> generateProductCommissionReport(group.key(), group, startDate, endDate))
      .onErrorResume(ex -> {
        log.error("Error generating commissions report: {}", ex.getMessage());
        return Flux.empty();
      });
  }

  /**
   * Realiza un pago de crédito desde terceros.
   *
   * @param request la solicitud de pago de terceros
   * @return Mono con la respuesta de la transacción
   */
  @Override
  public Mono<TransactionResponse> makeThirdPartyCreditPayment(
    ThirdPartyCreditPaymentRequest request) {
    log.info("Processing third party credit payment - credit: {}, payer: {}, amount: {}",
      request.getCreditId(), request.getPayerCustomerId(), request.getAmount());

    // Crear request para credit-service
    ThirdPartyCreditPaymentRequest paymentRequest = new ThirdPartyCreditPaymentRequest();
    paymentRequest.setAmount(request.getAmount());
    paymentRequest.setPayerCustomerId(request.getPayerCustomerId());
    paymentRequest.setPaymentDate(
      request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
    paymentRequest.setDescription(request.getDescription() != null ?
      request.getDescription() :
      "Third party payment from customer: " + request.getPayerCustomerId());

    // Procesar pago en credit-service y registrar transacción
    return creditServicePort.makeThirdPartyPayment(request.getCreditId(), paymentRequest)
      .flatMap(creditResponse -> {
        // Registrar la transacción localmente
        Transaction transaction = createThirdPartyPaymentTransaction(request, creditResponse);
        return transactionRepositoryPort.save(transaction);
      })
      .map(this::toTransactionResponse) // Convertir manualmente a TransactionResponse
      .doOnSuccess(
        transaction -> log.info("Third party credit payment completed: {}", transaction.getId()))
      .doOnError(
        error -> log.error("Error processing third party credit payment: {}", error.getMessage()));
  }

  /**
   * Obtiene los últimos 10 movimientos de un producto.
   *
   * @param productId el ID del producto
   * @param productType el tipo de producto
   * @return Mono con los últimos movimientos
   */
  @Override
  public Mono<LastMovementsResponse> getLast10MovementsReport(String productId,
                                                              String productType) {
    log.info("Getting last 10 movements report for product: {}, type: {}", productId, productType);

    // Validar que el productType sea válido
    if (!"CUENTA".equals(productType) && !"CREDITO".equals(productType)) {
      return Mono.error(new IllegalArgumentException("Invalid product type: " + productType));
    }

    return transactionRepositoryPort.findTop10ByProductIdAndProductTypeOrderByTransactionDateDesc(
        productId, productType)
      .collectList()
      .flatMap(transactions -> {
        if (transactions.isEmpty()) {
          return Mono.error(new TransactionException(
            "No movements found for product: " + productId + " of type: " + productType));
        }

        // Obtener información del producto para el reporte
        return getProductInfo(productId, productType)
          .map(productInfo -> {
            // Crear LastMovementsResponse usando setters (sin builder)
            LastMovementsResponse response = new LastMovementsResponse();
            response.setProductId(productId);
            response.setProductType(ProductTypeEnum.valueOf(productType));
            response.setProductNumber(productInfo.getNumber());
            response.setCustomerId(productInfo.getCustomerId());
            response.setTotalMovements(transactions.size());

            // Convertir transactions a MovementDetail
            List<MovementDetail> movementDetails = transactions.stream()
              .map(this::toMovementDetail)
              .collect(Collectors.toList());
            response.setMovements(movementDetails);

            return response;
          });
      })
      .doOnSuccess(
        report -> log.info("Last 10 movements report generated successfully for product: {}",
          productId))
      .doOnError(
        error -> log.error("Error generating last 10 movements report: {}", error.getMessage()));
  }

  /**
   * Convierte una transacción a detalle de movimiento.
   *
   * @param transaction la transacción a convertir
   * @return MovementDetail con los datos de la transacción
   */
  private MovementDetail toMovementDetail(Transaction transaction) {
    // Crear MovementDetail usando setters (sin builder)
    MovementDetail detail = new MovementDetail();
    detail.setTransactionId(transaction.getId());
    detail.setTransactionType(transaction.getTransactionType());
    detail.setAmount(transaction.getAmount());
    detail.setDescription(transaction.getDescription());
    if (transaction.getTransactionDate() != null) {
      ZoneOffset serverOffset =
        ZoneId.systemDefault().getRules().getOffset(transaction.getTransactionDate());
      OffsetDateTime offsetDateTime = transaction.getTransactionDate().atOffset(serverOffset);
      detail.setTransactionDate(offsetDateTime);
    }
    detail.setStatus(transaction.getStatus());
    detail.setPreviousBalance(transaction.getPreviousBalance());
    detail.setNewBalance(transaction.getNewBalance());

    return detail;
  }

  /**
   * Procesa un pago con tarjeta de débito.
   *
   * @param creditInfo información del crédito (tarjeta de débito)
   * @param amount monto del pago
   * @param description descripción del pago
   * @return Mono con la transacción realizada
   */
  private Mono<Transaction> processDebitCardPayment(CreditInfo creditInfo, Double amount,
                                                    String description) {
    log.info("Processing debit card payment - Card: {}, Amount: {}",
      creditInfo.getCreditNumber(), amount);

    if (!creditInfo.getExists()) {
      return Mono.error(new TransactionException(
        "Debit card is not exists. Card status: " + creditInfo.getCardStatus()));
    }

    return validateDebitCardLimits(creditInfo, amount)
      .then(processPaymentInSequence(creditInfo, amount, description))
      .flatMap(transactionResult -> {
        return transactionRepositoryPort.findById(transactionResult.getTransactionId())
          .doOnSuccess(
            tx -> log.info("Debit card payment completed - Transaction: {}, Card: {}, Account: {}",
              tx.getId(), creditInfo.getCreditNumber(), transactionResult.getAccountId()));
      });
  }

  /**
   * Valida los límites de la tarjeta de débito.
   *
   * @param creditInfo información del crédito
   * @param amount monto a validar
   * @return Mono vacío si la validación es exitosa
   */
  private Mono<Void> validateDebitCardLimits(CreditInfo creditInfo, Double amount) {
    if (creditInfo.getDailyPurchaseLimit() != null && amount > creditInfo.getDailyPurchaseLimit()) {
      return Mono.error(new TransactionException(
        "Amount exceeds daily purchase limit. Limit: " + creditInfo.getDailyPurchaseLimit() +
          ", Attempted: " + amount
      ));
    }

    return Mono.empty();
  }

  /**
   * Procesa el pago en secuencia de cuentas asociadas.
   *
   * @param creditInfo información del crédito
   * @param amount monto del pago
   * @param description descripción del pago
   * @return Mono con el resultado de la transacción
   */
  private Mono<TransactionResult> processPaymentInSequence(CreditInfo creditInfo, Double amount,
                                                           String description) {
    List<String> accountSequence = buildAccountSequence(creditInfo);

    log.info("Processing payment in sequence for debit card {} - Accounts: {}",
      creditInfo.getCreditNumber(), accountSequence.size());

    return tryPaymentInAccounts(accountSequence, amount, 0, description,
      creditInfo.getCreditNumber())
      .switchIfEmpty(Mono.error(new TransactionException(
        "Insufficient balance in all associated accounts for debit card: " +
          creditInfo.getCreditNumber()
      )));
  }

  /**
   * Construye la secuencia de cuentas para pago con tarjeta de débito.
   *
   * @param creditInfo información del crédito
   * @return lista de IDs de cuentas en orden de prioridad
   */
  private List<String> buildAccountSequence(CreditInfo creditInfo) {
    List<String> sequence = new ArrayList<>();

    // 1. Cuenta principal
    if (creditInfo.getMainAccountId() != null) {
      sequence.add(creditInfo.getMainAccountId());
    }

    // 2. Cuentas asociadas ordenadas por sequenceOrder
    if (creditInfo.getAssociatedAccounts() != null &&
      !creditInfo.getAssociatedAccounts().isEmpty()) {
      creditInfo.getAssociatedAccounts().stream()
        .sorted(Comparator.comparing(AssociatedAccountInfo::getSequenceOrder))
        .map(AssociatedAccountInfo::getAccountId)
        .forEach(sequence::add);
    }

    log.debug("Account sequence for debit card {}: {}", creditInfo.getCreditNumber(), sequence);
    return sequence;
  }

  /**
   * Intenta procesar el pago en las cuentas en secuencia.
   *
   * @param accountSequence secuencia de cuentas
   * @param amount monto del pago
   * @param currentIndex índice actual en la secuencia
   * @param description descripción del pago
   * @param cardNumber número de tarjeta
   * @return Mono con el resultado de la transacción
   */
  private Mono<TransactionResult> tryPaymentInAccounts(List<String> accountSequence, Double amount,
                                                       int currentIndex, String description,
                                                       String cardNumber) {
    if (currentIndex >= accountSequence.size()) {
      log.warn("No more accounts to try for debit card {}", cardNumber);
      return Mono.empty();
    }

    String currentAccountId = accountSequence.get(currentIndex);

    log.debug("Trying account {} ({} of {}) for debit card {}",
      currentAccountId, currentIndex + 1, accountSequence.size(), cardNumber);

    return accountServicePort.getAccountInfo(currentAccountId)
      .flatMap(accountInfo -> {
        // Validaciones...
        if (!accountInfo.getExists() || !"ACTIVO".equals(accountInfo.getStatus())) {
          return tryPaymentInAccounts(accountSequence, amount, currentIndex + 1, description,
            cardNumber);
        }

        // Verificar saldo suficiente
        if (accountInfo.getBalance() >= amount) {
          log.info("Sufficient balance in account {} (${}), processing payment",
            currentAccountId, accountInfo.getBalance());

          // SOLO actualizar el balance sin crear transacción de RETIRO
          return accountServicePort.updateAccountBalance(currentAccountId, amount, "RETIRO")
            .flatMap(response -> {
              // Crear directamente la transacción de PAGO_DEBITO
              Transaction transaction = buildDebitCardPaymentTransaction(
                response, amount, description, cardNumber, currentAccountId);
              return transactionRepositoryPort.save(transaction)
                .map(savedTx -> TransactionResult.builder()
                  .success(true)
                  .accountId(currentAccountId)
                  .transactionId(savedTx.getId())
                  .message("Payment processed successfully from account: " + currentAccountId)
                  .build());
            });
        } else {
          return tryPaymentInAccounts(accountSequence, amount, currentIndex + 1, description,
            cardNumber);
        }
      })
      .onErrorResume(ex -> {
        log.error("Error processing account {}: {}, trying next account",
          currentAccountId, ex.getMessage());
        return tryPaymentInAccounts(accountSequence, amount, currentIndex + 1, description,
          cardNumber);
      });
  }

  /**
   * Construye la transacción de pago con tarjeta de débito.
   *
   * @param response respuesta del servicio de cuentas
   * @param amount monto del pago
   * @param description descripción del pago
   * @param cardNumber número de tarjeta
   * @param accountId ID de la cuenta
   * @return transacción construida
   */
  private Transaction buildDebitCardPaymentTransaction(Map<String, Object> response, Double amount,
                                                       String description, String cardNumber,
                                                       String accountId) {
    return Transaction.builder()
      .transactionType(TransactionTypeEnum.PAGO_DEBITO)
      .productType(ProductTypeEnum.CUENTA)
      .productId(accountId) // La cuenta de donde se debitó
      .customerId(response.get("customerId").toString())
      .amount(amount)
      .description(description + " - Debit Card: " + cardNumber)
      .previousBalance(Double.valueOf(response.get("previousBalance").toString()))
      .newBalance(Double.valueOf(response.get("newBalance").toString()))
      .transactionDate(LocalDateTime.now())
      .status(TransactionStatusEnum.COMPLETADO)
      .transactionSubType("PAGO_DEBITO")
      .relatedAccountId(cardNumber) // La tarjeta de débito relacionada
      .commissionApplied(0.0)
      .build();
  }

  /**
   * Convierte una transacción a TransactionResponse.
   *
   * @param transaction la transacción a convertir
   * @return TransactionResponse convertido
   */
  private TransactionResponse toTransactionResponse(Transaction transaction) {
    TransactionResponse response = new TransactionResponse();
    response.setId(transaction.getId());
    response.setTransactionType(transaction.getTransactionType());
    response.setProductType(transaction.getProductType());
    response.setProductId(transaction.getProductId());
    response.setCustomerId(transaction.getCustomerId());
    response.setAmount(transaction.getAmount());
    response.setDescription(transaction.getDescription());
    response.setPreviousBalance(transaction.getPreviousBalance());
    response.setNewBalance(transaction.getNewBalance());

    // Convertir LocalDateTime a OffsetDateTime
    if (transaction.getTransactionDate() != null) {
      ZoneOffset serverOffset =
        ZoneId.systemDefault().getRules().getOffset(transaction.getTransactionDate());
      response.setTransactionDate(transaction.getTransactionDate().atOffset(serverOffset));
    }

    response.setStatus(transaction.getStatus());

    return response;
  }

  /**
   * Crea una transacción de pago de terceros.
   *
   * @param request solicitud de pago
   * @param creditResponse respuesta del servicio de créditos
   * @return transacción creada
   */
  private Transaction createThirdPartyPaymentTransaction(ThirdPartyCreditPaymentRequest request,
                                                         Map<String, Object> creditResponse) {
    Transaction transaction = new Transaction();
    transaction.setTransactionType(TransactionTypeEnum.PAGO_CREDITO);
    transaction.setProductType(ProductTypeEnum.CREDITO);
    transaction.setProductId(request.getCreditId());
    transaction.setCustomerId(request.getPayerCustomerId());
    transaction.setAmount(request.getAmount());
    transaction.setDescription(request.getDescription() != null ?
      request.getDescription() : "Third party payment for credit: " + request.getCreditId());
    transaction.setTransactionDate(LocalDateTime.now());
    transaction.setStatus(TransactionStatusEnum.COMPLETADO);
    transaction.setCommissionApplied(0.0);

    // Extraer información del crédito actualizado
    if (creditResponse != null) {
      Object outstandingBalance = creditResponse.get("outstandingBalance");
      if (outstandingBalance != null) {
        transaction.setNewBalance(Double.valueOf(outstandingBalance.toString()));
      }

      // También puedes establecer el previousBalance si está disponible
      Object previousBalance = creditResponse.get("previousBalance");
      if (previousBalance != null) {
        transaction.setPreviousBalance(Double.valueOf(previousBalance.toString()));
      }
    }

    return transaction;
  }

  /**
   * Genera un reporte de comisiones por producto.
   *
   * @param productId ID del producto
   * @param transactions transacciones del producto
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @return Mono con el reporte de comisiones
   */
  private Mono<CommissionReport> generateProductCommissionReport(String productId,
                                                                 Flux<Transaction> transactions,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
    return transactions.collectList()
      .flatMap(transactionList -> {
        if (transactionList.isEmpty()) {
          return Mono.empty();
        }

        // Obtener la primera transacción para saber el tipo de producto
        Transaction firstTransaction = transactionList.get(0);
        String productType = firstTransaction.getProductType().getValue();

        // Calcular totales
        double totalCommissions = transactionList.stream()
          .mapToDouble(Transaction::getCommissionApplied)
          .sum();

        // Obtener información del producto
        return getProductInfo(productId, productType)
          .map(productInfo -> createCommissionReport(productId, productType, transactionList,
            totalCommissions, startDate, endDate, productInfo));
      });
  }

  /**
   * Obtiene información del producto.
   *
   * @param productId ID del producto
   * @param productType tipo de producto
   * @return Mono con la información del producto
   */
  private Mono<ProductInfo> getProductInfo(String productId, String productType) {
    if ("CUENTA".equals(productType)) {
      return accountServicePort.getAccountInfo(productId)
        .map(account -> new ProductInfo(account.getAccountNumber(), account.getCustomerId()))
        .onErrorReturn(new ProductInfo("UNKNOWN", "UNKNOWN"));
    } else {
      return creditServicePort.getCreditInfo(productId)
        .map(credit -> new ProductInfo(credit.getCreditNumber(), credit.getCustomerId()))
        .onErrorReturn(new ProductInfo("UNKNOWN", "UNKNOWN"));
    }
  }

  /**
   * Crea un reporte de comisiones.
   *
   * @param productId ID del producto
   * @param productType tipo de producto
   * @param transactions lista de transacciones
   * @param totalCommissions total de comisiones
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param productInfo información del producto
   * @return reporte de comisiones
   */
  private CommissionReport createCommissionReport(String productId, String productType,
                                                  List<Transaction> transactions,
                                                  double totalCommissions,
                                                  LocalDate startDate, LocalDate endDate,
                                                  ProductInfo productInfo) {
    List<CommissionDetail> details = transactions.stream()
      .map(this::toCommissionDetail)
      .collect(Collectors.toList());

    CommissionReport report = new CommissionReport();
    report.setProductId(productId);
    report.setProductType(productType);
    report.setProductNumber(productInfo.getNumber());
    report.setCustomerId(productInfo.getCustomerId());
    report.setTotalCommissions(totalCommissions);
    report.setTransactionsCount(transactions.size());
    report.setPeriod(startDate.toString() + " to " + endDate.toString());
    report.setDetails(details);

    return report;
  }

  /**
   * Convierte una transacción a detalle de comisión.
   *
   * @param transaction transacción a convertir
   * @return detalle de comisión
   */
  private CommissionDetail toCommissionDetail(Transaction transaction) {
    CommissionDetail detail = new CommissionDetail();
    detail.setTransactionId(transaction.getId());

    if (transaction.getTransactionDate() != null) {
      ZoneOffset serverOffset =
        ZoneId.systemDefault().getRules().getOffset(transaction.getTransactionDate());
      detail.setTransactionDate(transaction.getTransactionDate().atOffset(serverOffset));
    }

    detail.setTransactionType(transaction.getTransactionType().getValue());
    detail.setAmount(transaction.getAmount());
    detail.setCommission(transaction.getCommissionApplied());
    detail.setDescription(transaction.getDescription());
    return detail;
  }

  /**
   * Procesa un depósito con tarjeta de débito.
   *
   * @param creditInfo información del crédito
   * @param amount monto del depósito
   * @param description descripción del depósito
   * @param merchant comercio
   * @return Mono con la transacción realizada
   */
  private Mono<Transaction> processDebitCardDeposit(CreditInfo creditInfo, Double amount,
                                                    String description, String merchant) {
    log.info("Processing debit card deposit - Card: {}, Amount: {}, Merchant: {}",
      creditInfo.getCreditNumber(), amount, merchant);

    if (!creditInfo.getExists()) {
      return Mono.error(new TransactionException("Debit card does not exist"));
    }

    // Validar que tenga cuenta principal
    if (creditInfo.getMainAccountId() == null) {
      return Mono.error(new TransactionException("Debit card has no main account associated"));
    }

    String fullDescription =
      description + " - " + merchant + " - Debit Card: " + creditInfo.getCreditNumber();

    return processAccountTransaction(
      creditInfo.getMainAccountId(),
      amount,
      fullDescription,
      "DEPOSITO",
      "CONSUMO_DEBITO",
      creditInfo.getCreditNumber()
    )
      .doOnSuccess(transaction -> log.info(
        "Debit card deposit completed - Transaction: {}, Card: {}, Account: {}",
        transaction.getId(), creditInfo.getCreditNumber(), creditInfo.getMainAccountId()));
  }

  /**
   * Valida los parámetros básicos de una solicitud de transacción.
   *
   * @param productId el ID del producto
   * @param amount el monto de la transacción
   * @param description la descripción de la transacción
   * @return Mono vacío si la validación es exitosa
   */
  private Mono<Void> validateRequest(String productId, Double amount, String description) {
    return Mono.fromRunnable(() -> {
      if (productId == null || productId.trim().isEmpty()) {
        throw new IllegalArgumentException("Product ID is required");
      }
      if (amount == null || amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
      }
      if (description == null || description.trim().isEmpty()) {
        throw new IllegalArgumentException("Description is required");
      }
    });
  }

  /**
   * Procesa una transacción de cuenta actualizando el balance y creando el registro.
   *
   * @param accountId el ID de la cuenta
   * @param amount el monto de la transacción
   * @param description la descripción de la transacción
   * @param transactionType el tipo de transacción
   * @param transactionSubType el subtipo de transacción
   * @param relatedAccountId el ID de cuenta relacionada (opcional)
   * @return Mono con la transacción procesada
   */
  private Mono<Transaction> processAccountTransaction(String accountId, Double amount,
                                                      String description, String transactionType,
                                                      String transactionSubType,
                                                      String relatedAccountId) {
    return accountServicePort.updateAccountBalance(accountId, Math.abs(amount), transactionType)
      .flatMap(response -> {
        Transaction transaction = buildAccountTransaction(
          response,
          convertToTransactionTypeEnum(transactionType),
          ProductTypeEnum.CUENTA,
          Math.abs(amount),
          description,
          transactionSubType,
          relatedAccountId
        );
        return transactionRepositoryPort.save(transaction);
      });
  }

  /**
   * Procesa una transacción de crédito actualizando el balance y creando el registro.
   *
   * @param creditId el ID del crédito
   * @param amount el monto de la transacción
   * @param description la descripción de la transacción
   * @param transactionType el tipo de transacción
   * @return Mono con la transacción procesada
   */
  private Mono<Transaction> processCreditTransaction(String creditId, Double amount,
                                                     String description, String transactionType) {
    return creditServicePort.updateCreditBalance(creditId, Math.abs(amount), transactionType)
      .flatMap(response -> {
        Transaction transaction = buildCreditTransaction(
          response,
          convertToTransactionTypeEnum(transactionType),
          ProductTypeEnum.CREDITO,
          Math.abs(amount),
          description
        );
        return transactionRepositoryPort.save(transaction);
      });
  }

  /**
   * Construye una transacción de cuenta a partir de la respuesta del servicio.
   *
   * @param response respuesta del servicio de cuentas
   * @param transactionType tipo de transacción
   * @param productType tipo de producto
   * @param amount monto de la transacción
   * @param description descripción de la transacción
   * @param transactionSubType subtipo de transacción
   * @param relatedAccountId ID de cuenta relacionada
   * @return transacción construida
   */
  private Transaction buildAccountTransaction(Map<String, Object> response,
                                              TransactionTypeEnum transactionType,
                                              ProductTypeEnum productType,
                                              Double amount, String description,
                                              String transactionSubType, String relatedAccountId) {
    return Transaction.builder()
      .transactionType(transactionType)
      .productType(productType)
      .productId(response.get("productId").toString())
      .customerId(response.get("customerId").toString())
      .amount(amount)
      .description(description)
      .previousBalance(Double.valueOf(response.get("previousBalance").toString()))
      .newBalance(Double.valueOf(response.get("newBalance").toString()))
      .transactionDate(LocalDateTime.now())
      .status(TransactionStatusEnum.COMPLETADO)
      .transactionSubType(transactionSubType)
      .relatedAccountId(relatedAccountId)
      .commissionApplied(Double.valueOf(response.get("commissionApplied").toString()))
      .build();
  }

  /**
   * Construye una transacción de crédito a partir de la respuesta del servicio.
   *
   * @param response respuesta del servicio de créditos
   * @param transactionType tipo de transacción
   * @param productType tipo de producto
   * @param amount monto de la transacción
   * @param description descripción de la transacción
   * @return transacción construida
   */
  private Transaction buildCreditTransaction(Map<String, Object> response,
                                             TransactionTypeEnum transactionType,
                                             ProductTypeEnum productType,
                                             Double amount, String description) {
    return Transaction.builder()
      .transactionType(transactionType)
      .productType(productType)
      .productId(response.get("id").toString())
      .customerId(response.get("customerId").toString())
      .amount(amount)
      .description(description)
      .previousBalance(getPreviousBalance(response, transactionType, amount))
      .newBalance(getNewBalance(response))
      .transactionDate(LocalDateTime.now())
      .status(TransactionStatusEnum.COMPLETADO)
      .build();
  }

  /**
   * Crea una transacción fallida para registrar el error.
   *
   * @param productId el ID del producto
   * @param amount el monto de la transacción
   * @param description la descripción de la transacción
   * @param transactionType el tipo de transacción
   * @param productType el tipo de producto
   * @param errorMessage el mensaje de error
   * @return Mono con la transacción fallida
   */
  private Mono<Transaction> createFailedTransaction(String productId, Double amount,
                                                    String description,
                                                    String transactionType, String productType,
                                                    String errorMessage) {
    Transaction failed = Transaction.builder()
      .transactionType(convertToTransactionTypeEnum(transactionType))
      .productType(convertToProductTypeEnum(productType))
      .productId(productId)
      .amount(amount)
      .description(description + " - FAILED: " + errorMessage)
      .transactionDate(LocalDateTime.now())
      .status(TransactionStatusEnum.RECHAZADO)
      .build();
    return transactionRepositoryPort.save(failed);
  }

  /**
   * Convierte un string a TransactionTypeEnum.
   *
   * @param transactionType el tipo de transacción como string
   * @return TransactionTypeEnum correspondiente
   */
  private TransactionTypeEnum convertToTransactionTypeEnum(String transactionType) {
    return TransactionTypeEnum.fromValue(transactionType);
  }

  /**
   * Convierte un string a ProductTypeEnum.
   *
   * @param productType el tipo de producto como string
   * @return ProductTypeEnum correspondiente
   */
  private ProductTypeEnum convertToProductTypeEnum(String productType) {
    return ProductTypeEnum.fromValue(productType);
  }

  /**
   * Calcula el balance anterior para transacciones de crédito.
   *
   * @param response respuesta del servicio de créditos
   * @param transactionType tipo de transacción
   * @param amount monto de la transacción
   * @return balance anterior calculado
   */
  private Double getPreviousBalance(Map<String, Object> response,
                                    TransactionTypeEnum transactionType, Double amount) {
    Double currentOutstanding = response.get("outstandingBalance") != null ?
      Double.valueOf(response.get("outstandingBalance").toString()) : 0.0;

    if ("PAGO_CREDITO".equals(transactionType.getValue())) {
      // Para pago: antes tenía más deuda
      return currentOutstanding + amount;
    } else if ("CONSUMO_TARJETA".equals(transactionType.getValue())) {
      // Para consumo: antes tenía menos deuda
      return currentOutstanding - amount;
    }
    return currentOutstanding;
  }

  /**
   * Obtiene el nuevo balance de la respuesta del servicio.
   *
   * @param response respuesta del servicio
   * @return nuevo balance
   */
  private Double getNewBalance(Map<String, Object> response) {
    return response.get("outstandingBalance") != null ?
      Double.valueOf(response.get("outstandingBalance").toString()) : 0.0;
  }


  /**
   * Determina si se debe validar el límite de transacciones mensuales.
   *
   * @param accountInfo información de la cuenta
   * @return true si se debe validar el límite, false en caso contrario
   */
  private boolean shouldValidateTransactionLimit(AccountInfo accountInfo) {
    // Solo validar límites para cuentas de ahorro
    return "AHORRO".equals(accountInfo.getAccountType()) &&
      accountInfo.getMonthlyTransactionLimit() != null &&
      accountInfo.getCurrentMonthTransactions() != null;
  }

  /**
   * Determina si un error es de procesamiento (no de validación).
   *
   * @param ex la excepción a evaluar
   * @return true si es error de procesamiento, false si es de validación
   */
  private boolean isProcessingError(Throwable ex) {
    return ex instanceof ExternalServiceException ||
      ex instanceof RuntimeException && !(ex instanceof TransactionException);
  }

  /**
   * Valida una solicitud de transferencia.
   *
   * @param request la solicitud de transferencia
   * @return Mono con la solicitud validada
   */
  private Mono<TransferRequest> validateTransferRequest(TransferRequest request) {
    return Mono.fromCallable(() -> {
      if (request.getFromAccountId().equals(request.getToAccountId())) {
        throw new TransactionException("Cannot transfer to the same account");
      }
      return request;
    });
  }

  /**
   * Procesa la validación de una transferencia.
   *
   * @param request la solicitud de transferencia
   * @param fromAccount información de la cuenta origen
   * @param toAccount información de la cuenta destino
   * @return Flux con las transacciones de transferencia
   */
  private Flux<Transaction> processTransferValidation(TransferRequest request,
                                                      AccountInfo fromAccount,
                                                      AccountInfo toAccount) {
    // Validar cuenta origen
    if (!fromAccount.getExists()) {
      return Flux.error(
        new TransactionException("Source account not found: " + request.getFromAccountId()));
    }
    if (!"ACTIVO".equals(fromAccount.getStatus())) {
      return Flux.error(new TransactionException(
        "Source account is not active. Status: " + fromAccount.getStatus()));
    }

    // Validar cuenta destino
    if (!toAccount.getExists()) {
      return Flux.error(
        new TransactionException("Destination account not found: " + request.getToAccountId()));
    }
    if (!"ACTIVO".equals(toAccount.getStatus())) {
      return Flux.error(new TransactionException(
        "Destination account is not active. Status: " + toAccount.getStatus()));
    }

    // Validar fondos suficientes en cuenta origen
    if (fromAccount.getBalance() < request.getAmount()) {
      return Flux.error(new InsufficientFundsException(
        "Insufficient funds in source account. Available: " + fromAccount.getBalance() +
          ", Requested: " + request.getAmount()
      ));
    }

    // Validar límites de transacción mensual para cuenta origen
    if (shouldValidateTransactionLimit(fromAccount)) {
      if (fromAccount.getCurrentMonthTransactions() >= fromAccount.getMonthlyTransactionLimit()) {
        return Flux.error(new TransactionException(
          "Monthly transaction limit exceeded for source account. Limit: " +
            fromAccount.getMonthlyTransactionLimit() +
            ", Current: " + fromAccount.getCurrentMonthTransactions()
        ));
      }
    }

    // Si pasa todas las validaciones, procesar la transferencia
    return processTransfer(request, fromAccount, toAccount);
  }

  /**
   * Procesa una transferencia entre cuentas.
   *
   * @param request la solicitud de transferencia
   * @param fromAccount información de la cuenta origen
   * @param toAccount información de la cuenta destino
   * @return Flux con las transacciones de débito y crédito
   */
  private Flux<Transaction> processTransfer(TransferRequest request, AccountInfo fromAccount,
                                            AccountInfo toAccount) {
    String description = "Transferencia - " + request.getDescription();

    Mono<Transaction> debitTransaction = processAccountTransaction(
      request.getFromAccountId(),
      request.getAmount(),
      description,
      "RETIRO",
      "TRANSFERENCIA",
      request.getToAccountId()
    );

    Mono<Transaction> creditTransaction = processAccountTransaction(
      request.getToAccountId(),
      request.getAmount(),
      description,
      "DEPOSITO",
      "TRANSFERENCIA",
      request.getFromAccountId()
    );

    return Flux.merge(debitTransaction, creditTransaction);
  }

}
