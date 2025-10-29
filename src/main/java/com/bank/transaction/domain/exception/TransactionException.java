package com.bank.transaction.domain.exception;

/**
 * Excepción base para todas las excepciones relacionadas con transacciones.
 * Proporciona una clase padre común para el manejo uniforme de errores.
 */
public class TransactionException extends RuntimeException {

  /**
   * Constructor con mensaje de error.
   *
   * @param message el mensaje descriptivo del error
   */
  public TransactionException(String message) {
    super(message);
  }
}