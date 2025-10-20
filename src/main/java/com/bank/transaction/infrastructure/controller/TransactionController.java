package com.bank.transaction.infrastructure.controller;

import com.bank.transaction.api.TransactionsApi;
import com.bank.transaction.domain.ports.input.TransactionInputPort;
import com.bank.transaction.mapper.TransactionApiMapper;
import com.bank.transaction.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionsApi {

    private final TransactionInputPort transactionInputPort;
    private final TransactionApiMapper transactionApiMapper;

    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getAll(String customerId, ProductTypeEnum productType, String productId, ServerWebExchange exchange) {
        log.info("GET /transactions - customerId: {}, productType: {}, productId: {}", customerId, productType, productId);

        Flux<TransactionResponse> transactions = transactionInputPort.getAllTransactions(customerId, (productType != null) ? productType.getValue() : null, productId)
                .map(transactionApiMapper::toResponse);

        return Mono.just(ResponseEntity.ok(transactions));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getByAccount(String accountId, ServerWebExchange exchange) {
        log.info("GET /transactions/account/{}", accountId);

        Flux<TransactionResponse> transactions = transactionInputPort.getTransactionsByAccount(accountId)
                .map(transactionApiMapper::toResponse);

        return Mono.just(ResponseEntity.ok(transactions));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getByCredit(String creditId, ServerWebExchange exchange) {
        log.info("GET /transactions/credit/{}", creditId);

        Flux<TransactionResponse> transactions = transactionInputPort.getTransactionsByCredit(creditId)
                .map(transactionApiMapper::toResponse);

        return Mono.just(ResponseEntity.ok(transactions));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getByCustomer(String customerId, ServerWebExchange exchange) {
        log.info("GET /transactions/customer/{}", customerId);

        Flux<TransactionResponse> transactions = transactionInputPort.getTransactionsByCustomer(customerId)
                .map(transactionApiMapper::toResponse);

        return Mono.just(ResponseEntity.ok(transactions));
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> makeDeposit(Mono<DepositRequest> depositRequest, ServerWebExchange exchange) {
        return depositRequest.flatMap(request -> {
            log.info("POST /transactions/deposit - accountId: {}, amount: {}", request.getAccountId(), request.getAmount());

            return transactionInputPort.makeDeposit(request.getAccountId(), request.getAmount(), request.getDescription())
                    .map(transactionApiMapper::toResponse)
                    .map(response -> ResponseEntity.status(201).body(response));
        });
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> makeWithdrawal(Mono<WithdrawalRequest> withdrawalRequest, ServerWebExchange exchange) {
        return withdrawalRequest.flatMap(request -> {
            log.info("POST /transactions/withdrawal - accountId: {}, amount: {}", request.getAccountId(), request.getAmount());

            return transactionInputPort.makeWithdrawal(request.getAccountId(), request.getAmount(), request.getDescription())
                    .map(transactionApiMapper::toResponse)
                    .map(response -> ResponseEntity.status(201).body(response));
        });
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> makeCreditConsumption(Mono<ConsumptionRequest> consumptionRequest, ServerWebExchange exchange) {
        return consumptionRequest.flatMap(request -> {
            log.info("POST /transactions/credit-consumption - creditId: {}, amount: {}, merchant: {}",
                    request.getCreditId(), request.getAmount(), request.getMerchant());

            return transactionInputPort.makeCreditConsumption(
                            request.getCreditId(),
                            request.getAmount(),
                            request.getDescription(),
                            request.getMerchant()
                    )
                    .map(transactionApiMapper::toResponse)
                    .map(response -> ResponseEntity.status(201).body(response));
        });
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> makeCreditPayment(Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest.flatMap(request -> {
            log.info("POST /transactions/credit-payment - creditId: {}, amount: {}", request.getCreditId(), request.getAmount());

            return transactionInputPort.makeCreditPayment(request.getCreditId(), request.getAmount(), request.getDescription())
                    .map(transactionApiMapper::toResponse)
                    .map(response -> ResponseEntity.status(201).body(response));
        });
    }
}
