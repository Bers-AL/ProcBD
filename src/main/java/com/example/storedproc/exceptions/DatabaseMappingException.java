package com.example.storedproc.exceptions;

public class DatabaseMappingException extends RuntimeException {
    public DatabaseMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}