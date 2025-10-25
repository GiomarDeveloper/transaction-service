package com.bank.transaction.domain.service.impl;

import com.bank.transaction.domain.model.AccountInfo;
import com.bank.transaction.domain.model.ProductInfo;
import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.domain.ports.input.TransactionInputPort;
import com.bank.transaction.domain.ports.output.TransactionRepositoryPort;
import com.bank.transaction.domain.ports.output.AccountServicePort;
import com.bank.transaction.domain.ports.output.CreditServicePort;
import com.bank.transaction.domain.exception.*;
import com.bank.transaction.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionInputPort {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountServicePort accountServicePort;
    private final CreditServicePort creditServicePort;

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
                        return Mono.error(new TransactionException("Account is not active. Status: " + accountInfo.getStatus()));
                    }

                    // Validar límites de transacción mensual si aplica
                    if (shouldValidateTransactionLimit(accountInfo)) {
                        if (accountInfo.getCurrentMonthTransactions() >= accountInfo.getMonthlyTransactionLimit()) {
                            return Mono.error(new TransactionException(
                                    "Monthly transaction limit exceeded. Limit: " + accountInfo.getMonthlyTransactionLimit() +
                                            ", Current: " + accountInfo.getCurrentMonthTransactions()
                            ));
                        }
                    }

                    return processAccountTransaction(accountId, amount, description, "DEPOSITO", null, null);
                })
                .onErrorResume(ex -> createFailedTransaction(
                        accountId, amount, description, "DEPOSITO", "CUENTA", ex.getMessage()));
    }

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
                        return Mono.error(new TransactionException("Account is not active. Status: " + accountInfo.getStatus()));
                    }

                    // Validar fondos suficientes para retiro
                    if (accountInfo.getBalance() < amount) {
                        return Mono.error(new InsufficientFundsException(
                                "Insufficient funds. Available: " + accountInfo.getBalance() + ", Requested: " + amount
                        ));
                    }

                    // Validar límites de transacción mensual si aplica
                    if (shouldValidateTransactionLimit(accountInfo)) {
                        if (accountInfo.getCurrentMonthTransactions() >= accountInfo.getMonthlyTransactionLimit()) {
                            return Mono.error(new TransactionException(
                                    "Monthly transaction limit exceeded. Limit: " + accountInfo.getMonthlyTransactionLimit() +
                                            ", Current: " + accountInfo.getCurrentMonthTransactions()
                            ));
                        }
                    }

                    return processAccountTransaction(accountId, amount, description, "RETIRO", null,null);
                })
                .onErrorResume(ex -> createFailedTransaction(
                        accountId, amount, description, "RETIRO", "CUENTA", ex.getMessage()));
    }

    @Override
    public Mono<Transaction> makeCreditPayment(String creditId, Double amount, String description) {
        return validateRequest(creditId, amount, description)
                .then(creditServicePort.getCreditInfo(creditId))
                .flatMap(creditInfo -> {
                    if (!creditInfo.getExists()) {
                        return Mono.error(new TransactionException("Credit not found: " + creditId));
                    }

                    if (!"ACTIVO".equals(creditInfo.getStatus())) {
                        return Mono.error(new TransactionException("Credit is not active. Status: " + creditInfo.getStatus()));
                    }

                    // Para PAGO: permitir tanto PRESTAMO_PERSONAL como TARJETA_CREDITO
                    // No necesitamos validar tipo específico para pagos

                    return processCreditTransaction(creditId, amount, description, "PAGO_CREDITO");
                })
                .onErrorResume(ex -> createFailedTransaction(
                        creditId, amount, description, "PAGO_CREDITO", "CREDITO", ex.getMessage()));
    }

    @Override
    public Mono<Transaction> makeCreditConsumption(String creditId, Double amount, String description, String merchant) {
        return validateRequest(creditId, amount, description)
                .then(creditServicePort.getCreditInfo(creditId))
                .flatMap(creditInfo -> {
                    // Validar existencia
                    if (!creditInfo.getExists()) {
                        return Mono.error(new TransactionException("Credit not found: " + creditId));
                    }

                    // Validar que esté activo
                    if (!"ACTIVO".equals(creditInfo.getStatus())) {
                        return Mono.error(new TransactionException("Credit is not active. Status: " + creditInfo.getStatus()));
                    }

                    // Validar tipo de crédito - SOLO tarjetas para consumo
                    if (!"TARJETA_CREDITO".equals(creditInfo.getCreditType())) {
                        return Mono.error(new TransactionException(
                                "Credit consumption only allowed for credit cards. Credit type: " + creditInfo.getCreditType()
                        ));
                    }

                    // Validar límite disponible
                    if (creditInfo.getAvailableCredit() == null || creditInfo.getAvailableCredit() < amount) {
                        return Mono.error(new InsufficientFundsException(
                                "Credit limit exceeded. Available: " + creditInfo.getAvailableCredit() + ", Requested: " + amount
                        ));
                    }

                    return processCreditTransaction(creditId, -amount, description + " - " + merchant, "CONSUMO_TARJETA");
                })
                .onErrorResume(ex -> createFailedTransaction(
                        creditId, amount, description, "CONSUMO_TARJETA", "CREDITO", ex.getMessage()));
    }

    @Override
    public Flux<Transaction> getAllTransactions(String customerId, String productType, String productId) {
        log.info("Getting transactions - customer: {}, productType: {}, productId: {}", customerId, productType, productId);

        if (customerId != null && productType != null) {
            return transactionRepositoryPort.findByCustomerIdAndProductType(customerId, productType);
        } else if (customerId != null) {
            return transactionRepositoryPort.findByCustomerId(customerId);
        } else if (productId != null) {
            return transactionRepositoryPort.findByProductId(productId);
        }
        return transactionRepositoryPort.findAll();
    }

    @Override
    public Flux<Transaction> getTransactionsByAccount(String accountId) {
        return transactionRepositoryPort.findByProductId(accountId);
    }

    @Override
    public Flux<Transaction> getTransactionsByCredit(String creditId) {
        return transactionRepositoryPort.findByProductId(creditId);
    }

    @Override
    public Flux<Transaction> getTransactionsByCustomer(String customerId) {
        return transactionRepositoryPort.findByCustomerId(customerId);
    }

    @Override
    public Flux<Transaction> makeTransfer(TransferRequest transferRequest) {
        return validateTransferRequest(transferRequest)
                .then(Mono.zip(
                        accountServicePort.getAccountInfo(transferRequest.getFromAccountId()),
                        accountServicePort.getAccountInfo(transferRequest.getToAccountId())
                ))
                .flatMapMany(accounts -> processTransferValidation(transferRequest, accounts.getT1(), accounts.getT2()))
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

    @Override
    public Flux<Transaction> getProductTransactionsForCurrentMonth(String productId, String productType) {
        log.info("Getting transactions for product: {} of type: {} in current month", productId, productType);

        return transactionRepositoryPort.findByProductIdAndProductTypeAndCurrentMonth(productId, productType)
                .onErrorResume(ex -> {
                    log.error("Error getting transactions for product {} (type: {}): {}",
                            productId, productType, ex.getMessage());
                    return Flux.empty();
                });
    }


    @Override
    public Flux<CommissionReport> getCommissionsReport(LocalDate startDate, LocalDate endDate, String productType) {
        log.info("Generating commissions report from {} to {}, productType: {}", startDate, endDate, productType);

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

    private CommissionReport createCommissionReport(String productId, String productType,
                                                    List<Transaction> transactions, double totalCommissions,
                                                    LocalDate startDate, LocalDate endDate, ProductInfo productInfo) {
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

    private CommissionDetail toCommissionDetail(Transaction transaction) {
        CommissionDetail detail = new CommissionDetail();
        detail.setTransactionId(transaction.getId());

        if (transaction.getTransactionDate() != null) {
            ZoneOffset serverOffset = ZoneId.systemDefault().getRules().getOffset(transaction.getTransactionDate());
            detail.setTransactionDate(transaction.getTransactionDate().atOffset(serverOffset));
        }

        detail.setTransactionType(transaction.getTransactionType().getValue());
        detail.setAmount(transaction.getAmount());
        detail.setCommission(transaction.getCommissionApplied());
        detail.setDescription(transaction.getDescription());
        return detail;
    }


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

    private Mono<Transaction> processAccountTransaction(String accountId, Double amount, String description, String transactionType,
                                                        String transactionSubType, String relatedAccountId) {
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

    private Mono<Transaction> processCreditTransaction(String creditId, Double amount, String description, String transactionType) {
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

    private Mono<Transaction> createFailedTransaction(String productId, Double amount, String description,
                                                      String transactionType, String productType, String errorMessage) {
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

    private TransactionTypeEnum convertToTransactionTypeEnum(String transactionType) {
        return TransactionTypeEnum.fromValue(transactionType);
    }

    private ProductTypeEnum convertToProductTypeEnum(String productType) {
        return ProductTypeEnum.fromValue(productType);
    }

    private Double getPreviousBalance(Map<String, Object> response, TransactionTypeEnum transactionType, Double amount) {
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

    private Double getNewBalance(Map<String, Object> response) {
        return response.get("outstandingBalance") != null ?
                Double.valueOf(response.get("outstandingBalance").toString()) : 0.0;
    }

    private boolean shouldValidateTransactionLimit(AccountInfo accountInfo) {
        // Solo validar límites para cuentas de ahorro
        return "AHORRO".equals(accountInfo.getAccountType()) &&
                accountInfo.getMonthlyTransactionLimit() != null &&
                accountInfo.getCurrentMonthTransactions() != null;
    }

    private boolean isProcessingError(Throwable ex) {
        return ex instanceof ExternalServiceException || ex instanceof RuntimeException && !(ex instanceof TransactionException);
    }

    private Mono<TransferRequest> validateTransferRequest(TransferRequest request) {
        return Mono.fromCallable(() -> {
            if (request.getFromAccountId().equals(request.getToAccountId())) {
                throw new TransactionException("Cannot transfer to the same account");
            }
            return request;
        });
    }

    private Flux<Transaction> processTransferValidation(TransferRequest request, AccountInfo fromAccount, AccountInfo toAccount) {
        // Validar cuenta origen
        if (!fromAccount.getExists()) {
            return Flux.error(new TransactionException("Source account not found: " + request.getFromAccountId()));
        }
        if (!"ACTIVO".equals(fromAccount.getStatus())) {
            return Flux.error(new TransactionException("Source account is not active. Status: " + fromAccount.getStatus()));
        }

        // Validar cuenta destino
        if (!toAccount.getExists()) {
            return Flux.error(new TransactionException("Destination account not found: " + request.getToAccountId()));
        }
        if (!"ACTIVO".equals(toAccount.getStatus())) {
            return Flux.error(new TransactionException("Destination account is not active. Status: " + toAccount.getStatus()));
        }

        // Validar fondos suficientes en cuenta origen
        if (fromAccount.getBalance() < request.getAmount()) {
            return Flux.error(new InsufficientFundsException(
                    "Insufficient funds in source account. Available: " + fromAccount.getBalance() + ", Requested: " + request.getAmount()
            ));
        }

        // Validar límites de transacción mensual para cuenta origen
        if (shouldValidateTransactionLimit(fromAccount)) {
            if (fromAccount.getCurrentMonthTransactions() >= fromAccount.getMonthlyTransactionLimit()) {
                return Flux.error(new TransactionException(
                        "Monthly transaction limit exceeded for source account. Limit: " + fromAccount.getMonthlyTransactionLimit() +
                                ", Current: " + fromAccount.getCurrentMonthTransactions()
                ));
            }
        }

        // Si pasa todas las validaciones, procesar la transferencia
        return processTransfer(request, fromAccount, toAccount);
    }

    private Flux<Transaction> processTransfer(TransferRequest request, AccountInfo fromAccount, AccountInfo toAccount) {
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
