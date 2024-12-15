package com.example.storedproc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Динамический прокси для вызова процедур.
 * Поддерживает синхронный и реактивный стеки.
 */
public class StoredProcedureInvocationHandler implements InvocationHandler {

    private final StoredProcedureRegistry registry;
    private final DatabaseAdapter databaseAdapter;

    public StoredProcedureInvocationHandler(StoredProcedureRegistry registry, DatabaseAdapter databaseAdapter) {
        this.registry = registry;
        this.databaseAdapter = databaseAdapter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        StoredProcedureCaller<?> caller = registry.getProcedureCaller(method.getName());
        return caller.call(args);
    }
}
