package com.bank.transaction.infrastructure.messaging;

import com.bank.transaction.application.event.BankWithdrawalRequestEvent;
import com.bank.transaction.application.event.BankWithdrawalResponseEvent;
import com.bank.transaction.application.event.TransactionPaymentRequestEvent;
import com.bank.transaction.application.event.TransactionConsumptionRequestEvent;
import com.bank.transaction.domain.ports.input.TransactionInputPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionInputPort transactionInputPort;
    private final ObjectMapper objectMapper;  // Spring Boot provee esto automáticamente
    private final TransactionEventProducer transactionEventProducer;

    @KafkaListener(topics = "transaction.payment.request", groupId = "transaction-service")
    public void consumeTransactionPaymentRequest(String message) {
        try {
            log.info("📨 Received raw message: {}", message);

            TransactionPaymentRequestEvent event = objectMapper.readValue(message, TransactionPaymentRequestEvent.class);
            log.info("🎯 Parsed transaction payment request - CreditId: {}, Amount: {}",
                    event.getCreditId(), event.getAmount());

            transactionInputPort.makeCreditPayment(
                    event.getCreditId(),
                    event.getAmount().doubleValue(),
                    event.getDescription()
            ).subscribe(transaction ->
                    log.info("✅ Credit payment processed - TransactionId: {}", transaction.getId())
            );
        } catch (Exception e) {
            log.error("❌ Error processing payment request: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "transaction.consumption.request", groupId = "transaction-service")
    public void consumeTransactionConsumptionRequest(String message) {
        try {
            log.info("📨 Received raw message: {}", message);

            TransactionConsumptionRequestEvent event = objectMapper.readValue(message, TransactionConsumptionRequestEvent.class);
            log.info("🎯 Received transaction consumption request - CreditId: {}, Amount: {}, Merchant: {}",
                    event.getCreditId(), event.getAmount(), event.getMerchant());

            transactionInputPort.makeCreditConsumption(
                    event.getCreditId(),
                    event.getAmount().doubleValue(),
                    event.getDescription(),
                    event.getMerchant()
            ).subscribe(transaction ->
                    log.info("✅ Credit consumption processed - TransactionId: {}", transaction.getId())
            );
        } catch (Exception e) {
            log.error("❌ Error processing consumption request: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bank-withdrawal-request", groupId = "transaction-service")
    public void consumeBankWithdrawalRequest(String message) {
        try {
            log.info("📨 Received bank withdrawal request raw message: {}", message);

            BankWithdrawalRequestEvent event = objectMapper.readValue(message, BankWithdrawalRequestEvent.class);
            log.info("🎯 Parsed bank withdrawal request - WithdrawalId: {}, RequestId: {}, Account: {}, Amount: {}",
                    event.getWithdrawalId(), event.getRequestId(), event.getAccountId(), event.getAmount());

            // Procesar el retiro bancario
            transactionInputPort.makeWithdrawal(
                    event.getAccountId(),
                    event.getAmount(),
                    event.getDescription()
            ).subscribe(transaction -> {
                log.info("✅ Bank withdrawal processed - TransactionId: {}, WithdrawalId: {}",
                        transaction.getId(), event.getWithdrawalId());

                // Enviar respuesta exitosa
                BankWithdrawalResponseEvent response = BankWithdrawalResponseEvent.builder()
                        .withdrawalId(event.getWithdrawalId())
                        .requestId(event.getRequestId())
                        .transactionId(transaction.getId())
                        .status("COMPLETADO")
                        .previousBalance(transaction.getPreviousBalance())
                        .newBalance(transaction.getNewBalance())
                        .message("Retiro bancario exitoso")
                        .timestamp(System.currentTimeMillis())
                        .build();

                transactionEventProducer.sendBankWithdrawalResponse(response);

            }, error -> {
                log.error("❌ Error processing bank withdrawal - WithdrawalId: {}, Error: {}",
                        event.getWithdrawalId(), error.getMessage());

                // Enviar respuesta de error
                BankWithdrawalResponseEvent response = BankWithdrawalResponseEvent.builder()
                        .withdrawalId(event.getWithdrawalId())
                        .requestId(event.getRequestId())
                        .transactionId(null)
                        .status("RECHAZADO")
                        .previousBalance(0.0)
                        .newBalance(0.0)
                        .message("Error en retiro bancario: " + error.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build();

                transactionEventProducer.sendBankWithdrawalResponse(response);
            });

        } catch (Exception e) {
            log.error("❌ Error processing bank withdrawal request: {}", e.getMessage(), e);
        }
    }
}