package com.example.storedproc.core;

import com.example.storedproc.exceptions.ProcedureExecutionException;
import com.example.storedproc.exceptions.StoredProcedureException;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReactiveDatabaseAdapter implements DatabaseAdapter {

    private final ConnectionFactory connectionFactory;

    public ReactiveDatabaseAdapter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean isReactive() {
        return true;
    }

    @Override
    public <T> T execute(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
        throw new UnsupportedOperationException("Синхронный режим недоступен для R2DBC.");
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(String procedureName, Map<String, Object> parameters, Class<T> resultType) {
        throw new UnsupportedOperationException("Асинхронный режим недоступен для R2DBC. Используйте executeReactive.");
    }

    @Override
    public <T> Publisher<T> executeReactive(String procedureName,
                                            Map<String, Object> parameters, Class<T> resultType) {
        return Flux.usingWhen(
                connectionFactory.create(),
                connection -> executeStatement(connection, procedureName, parameters, resultType),
                Connection::close
        ).onErrorMap(e -> {
            if (e instanceof R2dbcException) {
                return new ProcedureExecutionException(procedureName,
                        "Ошибка выполнения R2DBC", e);
            }
            return new StoredProcedureException(
                    "Неожиданная ошибка при выполнении " + procedureName, e);
        });
    }

    private <T> Flux<T> executeStatement(Connection connection,
                                         String procedureName,
                                         Map<String, Object> parameters,
                                         Class<T> resultType) {
        Statement statement = connection.createStatement("CALL " + procedureName);
        parameters.forEach((key, value) -> bindParameter(statement, key, value));

        if (resultType == Void.TYPE) {
            return Flux.from(statement.execute()).then(Mono.<T>empty()).flux();
        }

        return Flux.from(statement.execute())
                .flatMap(result -> result.map((row, metadata) ->
                        row.get(0, resultType)));
    }

    private void bindParameter(Statement statement, String name, Object value) {
        if (value == null) {
            statement.bindNull(name, Object.class);
        } else {
            statement.bind(name, value);
        }
    }
}
