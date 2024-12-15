package com.example.storedproc.core;

import java.util.concurrent.ConcurrentHashMap;

public class StoredProcedureRegistry {

    private final ConcurrentHashMap<String, StoredProcedureCaller<?>> procedureMap = new ConcurrentHashMap<>();

    public <T> void registerProcedure(String procedureName, StoredProcedureCaller<T> caller) {
        procedureMap.put(procedureName, caller);
    }

    public void removeProcedure(String procedureName) {
        procedureMap.remove(procedureName);
    }

    @SuppressWarnings("unchecked")
    public <T> StoredProcedureCaller<T> getProcedureCaller(String procedureName) {
        StoredProcedureCaller<?> caller = procedureMap.get(procedureName);
        if (caller == null) {
            throw new IllegalArgumentException("Процедура не найдена: " + procedureName);
        }
        return (StoredProcedureCaller<T>) caller;
    }

    public boolean isEmpty() {
        return procedureMap.isEmpty();
    }
}
