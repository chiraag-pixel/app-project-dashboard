package com.skillgap.exception;

/** Thrown when a business rule prevents an operation (shown to the user as a message). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
