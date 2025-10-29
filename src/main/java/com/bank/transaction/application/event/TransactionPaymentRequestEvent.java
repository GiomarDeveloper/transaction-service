package com.bank.transaction.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de solicitud de pago de crédito.
 * Se utiliza para procesar pagos a productos de crédito.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPaymentRequestEvent {
  private String paymentId;
  private String creditId;
  private BigDecimal amount;
  private String description;
  private String customerId;
  private LocalDateTime paymentDate;
  private String source;
}