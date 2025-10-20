package com.bank.transaction.domain.exception;

public class ExternalServiceException extends TransactionException {
    public ExternalServiceException(String message) {
        super(message);
    }
}