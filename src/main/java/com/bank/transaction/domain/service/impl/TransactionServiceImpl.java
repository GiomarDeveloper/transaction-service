package com.bank.transaction.domain.service.impl;

import com.bank.transaction.domain.model.AccountInfo;
import com.bank.transaction.domain.model.Transaction;
import com.bank.transaction.domain.ports.input.TransactionInputPort;
import com.bank.transaction.domain.ports.output.TransactionRepositoryPort;
import com.bank.transaction.domain.ports.output.AccountServicePort;
import com.bank.transaction.domain.ports.output.CreditServicePort;
import com.bank.transaction.domain.exception.*;
import com.bank.transaction.model.ProductTypeEnum;
import com.bank.transaction.model.TransactionStatusEnum;
import com.bank.transaction.model.TransactionTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.Map;

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

                    return processAccountTransaction(accountId, amount, description, "DEPOSITO");
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

                    return processAccountTransaction(accountId, -amount, description, "RETIRO");
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

    private Mono<Transaction> processAccountTransaction(String accountId, Double amount, String description, String transactionType) {
        return accountServicePort.updateAccountBalance(accountId, Math.abs(amount), transactionType)
                .flatMap(response -> {
                    Transaction transaction = buildAccountTransaction(
                            response,
                            convertToTransactionTypeEnum(transactionType),
                            ProductTypeEnum.CUENTA,
                            Math.abs(amount),
                            description
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
                                                Double amount, String description) {
        return Transaction.builder()
                .transactionType(transactionType)
                .productType(productType)
                .productId(response.get("productId").toString())
                .customerId(response.get("customerId").toString())
                .amount(amount)
                .description(description)
                // ✅ Usar directamente los valores del response
                .previousBalance(Double.valueOf(response.get("previousBalance").toString()))
                .newBalance(Double.valueOf(response.get("newBalance").toString()))
                .transactionDate(LocalDateTime.now())
                .status(TransactionStatusEnum.COMPLETADO)
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
                // Para créditos, usar outstandingBalance como balance
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
}
