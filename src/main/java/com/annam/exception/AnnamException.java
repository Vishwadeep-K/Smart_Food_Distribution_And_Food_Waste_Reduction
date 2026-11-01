package com.annam.exception;

public class AnnamException extends RuntimeException {
    public AnnamException(String message) {
        super(message);
    }

    public AnnamException(String message, Throwable cause) {
        super(message, cause);
    }
}
