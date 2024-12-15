package com.example.storedproc.exceptions;

public class StoredProcedureException extends RuntimeException {
    public StoredProcedureException(String message) {
        super(message);
    }

    public StoredProcedureException(String message, Throwable cause) {
        super(message, cause);
    }
}
