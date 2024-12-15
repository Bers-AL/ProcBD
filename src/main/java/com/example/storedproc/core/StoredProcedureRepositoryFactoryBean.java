package com.example.storedproc.core;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class StoredProcedureRepositoryFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;
    private final StoredProcedureRegistry registry;

    @Autowired(required = false)
    private JpaDatabaseAdapter jpaDatabaseAdapter;

    @Autowired(required = false)
    private ReactiveDatabaseAdapter reactiveDatabaseAdapter;

    public StoredProcedureRepositoryFactoryBean(Class<T> repositoryInterface, StoredProcedureRegistry registry) {
        this.repositoryInterface = repositoryInterface;
        this.registry = registry;
    }

    @Override
    public T getObject() {
        boolean isReactive = isReactiveRepository(repositoryInterface);

        if (isReactive) {
            if (reactiveDatabaseAdapter == null) {
                throw new IllegalStateException("ReactiveDatabaseAdapter не найден для реактивного репозитория");
            }
            return createProxy(reactiveDatabaseAdapter);
        } else {
            if (jpaDatabaseAdapter == null) {
                throw new IllegalStateException("JpaDatabaseAdapter не найден для JPA репозитория");
            }
            return createProxy(jpaDatabaseAdapter);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return this.repositoryInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private boolean isReactiveRepository(Class<?> repositoryInterface) {
        return Arrays.stream(repositoryInterface.getMethods())
                .anyMatch(method -> Publisher.class.isAssignableFrom(method.getReturnType()));
    }

    private T createProxy(DatabaseAdapter adapter) {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class[]{repositoryInterface},
                new StoredProcedureInvocationHandler(registry, adapter)
        );
    }
}
