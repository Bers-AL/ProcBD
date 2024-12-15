package com.example.storedproc.exceptions;

public class ConfigurationException extends StoredProcedureException {
    public ConfigurationException(String message) {
        super(message, null);
    }
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
