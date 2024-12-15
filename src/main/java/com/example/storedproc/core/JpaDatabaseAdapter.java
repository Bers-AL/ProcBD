package com.example.storedproc.core;

import com.example.storedproc.exceptions.ProcedureExecutionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JpaDatabaseAdapter implements DatabaseAdapter {

    private final EntityManager entityManager;

    public JpaDatabaseAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean isReactive() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
        try {
            Query query = entityManager.createNativeQuery(procedureName);
            parameters.forEach(query::setParameter);

            if (resultType == Void.TYPE) {
                query.executeUpdate();
                return null;
            }

            Object result = query.getSingleResult();
            if (result == null) {
                return null;
            }

            return (T) result;
        } catch (Exception e) {
            throw new ProcedureExecutionException(procedureName,
                    "Ошибка выполнения JPA запроса", e);
        }
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(String procedureName,
                                                 Map<String, Object> parameters, Class<T> resultType) {
        return CompletableFuture.supplyAsync(() ->
                execute(procedureName, parameters, resultType));
    }

    @Override
    public <T> Publisher<T> executeReactive(String procedureName,
                                            Map<String, Object> parameters, Class<T> resultType) {
        throw new UnsupportedOperationException(
                "Реактивный режим недоступен для JPA. Используйте execute или executeAsync");
    }
}