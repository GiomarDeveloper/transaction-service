package com.bank.transaction.infrastructure.client;

import com.bank.transaction.domain.model.CreditInfo;
import com.bank.transaction.domain.ports.output.CreditServicePort;
import com.bank.transaction.domain.exception.ExternalServiceException;
import com.bank.transaction.model.ConsumptionRequest;
import com.bank.transaction.model.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditServiceClient implements CreditServicePort {

    private final WebClient webClient;

    @Value("${external.services.credit.url:http://localhost:8083/}")
    private String creditServiceUrl;

    @Override
    public Mono<Map<String, Object>> updateCreditBalance(String creditId, Double amount, String transactionType) {
        log.info("Updating credit balance - creditId: {}, amount: {}, type: {}", creditId, amount, transactionType);

        // Determinar qué endpoint usar basado en el tipo de transacción
        if ("PAGO_CREDITO".equals(transactionType)) {
            // Usar makePayment para pagos
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setAmount(Math.abs(amount));
            paymentRequest.setDescription("Payment from transaction service");
            paymentRequest.setPaymentDate(LocalDate.now());

            return webClient.post()
                    .uri(creditServiceUrl + "/credits/{creditId}/payment", creditId)
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .map(errorBody -> new ExternalServiceException(
                                            "Credit service error: " + response.statusCode() + " - " + errorBody
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(ex -> {
                        log.error("Error calling credit service: {}", ex.getMessage());
                        return Mono.error(new ExternalServiceException("Unable to process credit payment: " + ex.getMessage()));
                    });

        } else if ("CONSUMO_TARJETA".equals(transactionType)) {
            // Usar chargeConsumption para consumos
            ConsumptionRequest consumptionRequest = new ConsumptionRequest();
            consumptionRequest.setAmount(Math.abs(amount));
            consumptionRequest.setDescription("Consumption from transaction service");
            consumptionRequest.setMerchant("Transaction System");
            consumptionRequest.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));

            return webClient.post()
                    .uri(creditServiceUrl + "/credits/{creditId}/consumption", creditId)
                    .bodyValue(consumptionRequest)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .map(errorBody -> new ExternalServiceException(
                                            "Credit service error: " + response.statusCode() + " - " + errorBody
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorResume(ex -> {
                        log.error("Error calling credit service: {}", ex.getMessage());
                        return Mono.error(new ExternalServiceException("Unable to process credit consumption: " + ex.getMessage()));
                    });
        } else {
            return Mono.error(new ExternalServiceException("Unsupported transaction type: " + transactionType));
        }
    }

    @Override
    public Mono<Boolean> validateCreditExists(String creditId) {
        log.info("Validating credit existence: {}", creditId);

        return webClient.get()
                .uri(creditServiceUrl + "/credits/{creditId}", creditId)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        Mono.just(new ExternalServiceException("Credit validation failed: " + response.statusCode()))
                )
                .bodyToMono(Object.class) // Puede ser cualquier clase, solo nos importa el status
                .map(response -> true) // Si llega respuesta, existe
                .onErrorResume(ExternalServiceException.class, ex -> {
                    log.warn("Credit validation failed: {}", ex.getMessage());
                    return Mono.just(false);
                })
                .onErrorResume(ex -> {
                    log.error("Error validating credit: {}", ex.getMessage());
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> validateCreditLimit(String creditId, Double amount) {
        log.info("Validating credit limit - creditId: {}, amount: {}", creditId, amount);

        return webClient.get()
                .uri(creditServiceUrl + "/credits/{creditId}/validate-limit?amount={amount}", creditId, amount)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        Mono.just(new ExternalServiceException("Credit limit validation failed: " + response.statusCode()))
                )
                .bodyToMono(Boolean.class)
                .defaultIfEmpty(false)
                .onErrorResume(ExternalServiceException.class, ex -> {
                    log.warn("Credit limit validation failed: {}", ex.getMessage());
                    return Mono.just(false);
                })
                .onErrorResume(ex -> {
                    log.error("Error validating credit limit: {}", ex.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<CreditInfo> getCreditInfo(String creditId) {
        log.info("Getting complete credit info for: {}", creditId);

        return webClient.get()
                .uri(creditServiceUrl + "/credits/{creditId}", creditId)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        Mono.error(new ExternalServiceException("Failed to get credit info: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(responseMap -> {
                    // Mapear manualmente desde el Map
                    return CreditInfo.builder()
                            .exists(true)
                            .creditType(getStringValue(responseMap, "creditType"))
                            .availableCredit(getDoubleValue(responseMap, "availableCredit"))
                            .creditLimit(getDoubleValue(responseMap, "creditLimit"))
                            .outstandingBalance(getDoubleValue(responseMap, "outstandingBalance"))
                            .status(getStringValue(responseMap, "status"))
                            .creditNumber(getStringValue(responseMap, "creditNumber"))
                            .customerId(getStringValue(responseMap, "customerId"))
                            .build();
                })
                .onErrorResume(ex -> {
                    log.error("Error getting credit info: {}", ex.getMessage());
                    return Mono.just(CreditInfo.builder()
                            .exists(false)
                            .build());
                });
    }

    private String getStringValue(Map<String, Object> map, String key) {
        return map.containsKey(key) ? map.get(key).toString() : null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(value.toString());
    }
}