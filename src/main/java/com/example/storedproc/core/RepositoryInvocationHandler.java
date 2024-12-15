package com.example.storedproc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RepositoryInvocationHandler implements InvocationHandler {

    private final Class<?> repositoryInterface;
    private final StoredProcedureRegistry registry;

    public RepositoryInvocationHandler(Class<?> repositoryInterface, StoredProcedureRegistry registry) {
        this.repositoryInterface = repositoryInterface;
        this.registry = registry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        StoredProcedureCaller<?> caller = registry.getProcedureCaller(method.getName());
        return caller.call(args);
    }
}

