package com.bank.transaction.domain.exception;

/**
 * Excepción lanzada cuando ocurre un error al comunicarse con servicios externos.
 * Por ejemplo: fallos en llamadas a servicios de cuentas, créditos, etc.
 */
public class ExternalServiceException extends TransactionException {

  /**
   * Constructor con mensaje de error.
   *
   * @param message el mensaje descriptivo del error
   */
  public ExternalServiceException(String message) {
    super(message);
  }
}