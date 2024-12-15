package com.example.storedproc.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.reactivestreams.Publisher;


import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Configuration
public class DefaultDatabaseAdapterConfiguration {

    @Bean
    @ConditionalOnMissingBean(DatabaseAdapter.class)
    public DatabaseAdapter defaultDatabaseAdapter() {
        return new NoOpDatabaseAdapter();
    }

    static class NoOpDatabaseAdapter implements DatabaseAdapter {
        @Override
        public boolean isReactive() {
            return false;
        }

        @Override
        public <T> T execute(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
            throw new UnsupportedOperationException("DatabaseAdapter не настроен.");
        }

        @Override
        public <T> CompletableFuture<T> executeAsync(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
            throw new UnsupportedOperationException("DatabaseAdapter не настроен.");
        }

        @Override
        public <T> Publisher<T> executeReactive(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
            throw new UnsupportedOperationException("DatabaseAdapter не настроен.");
        }
    }
}

