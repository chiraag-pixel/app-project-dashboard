package com.skillgap.exception;

/** Thrown when an entity does not exist or does not belong to the current user. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
