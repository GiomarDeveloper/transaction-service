package com.bank.transaction.infrastructure.messaging;

import com.bank.transaction.application.event.BankWithdrawalResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBankWithdrawalResponse(BankWithdrawalResponseEvent response) {
        try {
            CompletableFuture<?> future = kafkaTemplate.send("bank-withdrawal-response", response.getWithdrawalId(), response);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Bank withdrawal response sent - WithdrawalId: {}, Status: {}",
                            response.getWithdrawalId(), response.getStatus());
                } else {
                    log.error("❌ Failed to send bank withdrawal response - WithdrawalId: {}: {}",
                            response.getWithdrawalId(), ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("❌ Error sending bank withdrawal response: {}", e.getMessage());
        }
    }
}