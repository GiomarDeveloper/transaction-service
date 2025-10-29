package com.bank.transaction.domain.exception;

/**
 * Excepción lanzada cuando una cuenta no tiene fondos suficientes para una transacción.
 * Se utiliza en operaciones de retiro, transferencia y pago.
 */
public class InsufficientFundsException extends TransactionException {

  /**
   * Constructor con mensaje de error.
   *
   * @param message el mensaje descriptivo del error
   */
  public InsufficientFundsException(String message) {
    super(message);
  }
}