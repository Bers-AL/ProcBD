package com.example.storedproc.core;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DatabaseAdapter {

    boolean isReactive();

    <T> T execute(String procedureName, Map<String, Object> parameters, Class<T> resultType);

    <T> CompletableFuture<T> executeAsync(String procedureName, Map<String, Object> parameters, Class<T> resultType);

    <T> Publisher<T> executeReactive(String procedureName, Map<String, Object> parameters, Class<T> resultType);
}
