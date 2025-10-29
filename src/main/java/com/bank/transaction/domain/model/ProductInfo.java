package com.bank.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Modelo básico que representa información mínima de un producto financiero.
 * Utilizado para identificar productos (cuentas o créditos) en transacciones.
 */
@Data
@AllArgsConstructor
public class ProductInfo {
  private String number;
  private String customerId;
}
