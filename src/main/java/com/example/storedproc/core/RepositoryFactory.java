package com.example.storedproc.core;

import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

@Component
public class RepositoryFactory {

    private final StoredProcedureRegistry registry;

    public RepositoryFactory(StoredProcedureRegistry registry) {
        this.registry = registry;
    }

    public <T> T createRepository(Class<T> repositoryInterface) {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                new RepositoryInvocationHandler(repositoryInterface, registry)
        );
    }
}

