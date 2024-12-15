package com.example.storedproc.core;

import java.util.concurrent.CompletableFuture;

public interface ReactiveQueryExecutor {
    CompletableFuture<Void> executeUpdate(String sql);
    CompletableFuture<Boolean> checkExists(String sql);
}
