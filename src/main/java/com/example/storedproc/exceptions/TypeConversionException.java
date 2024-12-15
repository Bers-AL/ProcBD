package com.example.storedproc.exceptions;

public class TypeConversionException extends RuntimeException {
    public TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}