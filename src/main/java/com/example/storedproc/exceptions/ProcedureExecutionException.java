package com.example.storedproc.exceptions;

public class ProcedureExecutionException extends StoredProcedureException {
    public ProcedureExecutionException(String procedureName, String message) {
        super("Ошибка выполнения процедуры " + procedureName + ": " + message);
    }

    public ProcedureExecutionException(String procedureName, String message, Throwable cause) {
        super("Ошибка выполнения процедуры " + procedureName + ": " + message, cause);
    }
}
