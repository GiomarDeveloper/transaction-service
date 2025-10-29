package com.bank.transaction.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de solicitud de consumo con tarjeta de crédito.
 * Se utiliza para procesar consumos/pagos con productos de crédito.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionConsumptionRequestEvent {
  private String consumptionId;
  private String creditId;
  private BigDecimal amount;
  private String description;
  private String merchant;
  private LocalDateTime transactionDate;
  private String source;
}