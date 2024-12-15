package com.example.storedproc.core;

import com.example.storedproc.annotations.StoredProceduresRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StoredProcedureSynchronizer {
    private final DatabaseAdapter adapter;
    private final StoredProcedureRegistry registry;
    private final ProcedureSQLGenerator sqlGenerator;

    public StoredProcedureSynchronizer(DatabaseAdapter adapter, StoredProcedureRegistry registry, ProcedureSQLGenerator sqlGenerator) {
        this.adapter = adapter;
        this.registry = registry;
        this.sqlGenerator = sqlGenerator;
    }

    public void synchronizeProcedures(Class<?> repositoryInterface, Class<?> entityClass) {
        if (repositoryInterface == null || entityClass == null) {
            throw new NullPointerException("Repository interface and entity class must not be null");
        }

        if (!repositoryInterface.isAnnotationPresent(StoredProceduresRepository.class)) {
            throw new IllegalArgumentException(
                    "Interface " + repositoryInterface.getName() + " must be annotated with @StoredProceduresRepository"
            );
        }

        ensureTableExists();
        Map<String, String> existingProcedures = getExistingProcedures();

        // Синхронизация процедур
        for (Method method : repositoryInterface.getMethods()) {
            String procedureName = method.getName();
            String sql = sqlGenerator.generateProcedureSQL(method, entityClass);

            Class<?> resultType = determineResultType(method);
            Map<String, Class<?>> parameterTypes = determineParameterTypes(method);

            // Создаем новую или обновляем существующую процедуру
            if (existingProcedures.containsKey(procedureName)) {
                dropProcedure(procedureName);
            }
            createProcedure(procedureName, sql);

            // Регистрируем caller для процедуры
            StoredProcedureCaller<?> caller = new StoredProcedureCaller<>(
                    procedureName,
                    method,
                    resultType,
                    adapter
            );
            registry.registerProcedure(procedureName, caller);
        }

        // Удаляем устаревшие процедуры
        cleanupObsoleteProcedures(repositoryInterface, existingProcedures);
    }

    private void ensureTableExists() {
        String checkTableSQL = """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_name = 'stored_procedures'
        """;

        Long tableExists = adapter.execute(checkTableSQL, Map.of(), Long.class);

        if (tableExists == null || tableExists == 0) {
            String createTableSQL = """
                CREATE TABLE stored_procedures (
                    id SERIAL PRIMARY KEY,
                    procedure_name VARCHAR(255) NOT NULL UNIQUE
                );
            """;
            adapter.execute(createTableSQL, Map.of(), Void.class);
        }
    }

    private Map<String, String> getExistingProcedures() {
        Map<String, String> result = new HashMap<>();

        List<String> procedures = adapter.execute(
                "SELECT procedure_name FROM stored_procedures",
                Map.of(),
                List.class
        );

        if (procedures != null) {
            for (String procedure : procedures) {
                result.put(procedure, procedure);
            }
        }

        return result;
    }

    private void createProcedure(String procedureName, String sql) {
        adapter.execute(sql, Map.of(), Void.class);
        adapter.execute(
                "INSERT INTO stored_procedures (procedure_name) VALUES (:name)",
                Map.of("name", procedureName),
                Void.class
        );
    }

    private void dropProcedure(String procedureName) {
        adapter.execute(
                "DROP PROCEDURE IF EXISTS " + procedureName,
                Map.of(),
                Void.class
        );
        adapter.execute(
                "DELETE FROM stored_procedures WHERE procedure_name = :name",
                Map.of("name", procedureName),
                Void.class
        );
    }

    private Class<?> determineResultType(Method method) {
        if (method.getReturnType() == Mono.class) {
            return (Class<?>) ((ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments()[0];
        }
        if (method.getReturnType() == Flux.class) {
            return (Class<?>) ((ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments()[0];
        }
        return method.getReturnType();
    }

    private Map<String, Class<?>> determineParameterTypes(Method method) {
        Map<String, Class<?>> parameterTypes = new HashMap<>();
        Parameter[] parameters = method.getParameters();

        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            Class<?> paramType = parameter.getType();

            if (paramType == Mono.class || paramType == Flux.class) {
                paramType = (Class<?>) ((ParameterizedType) parameter.getParameterizedType())
                        .getActualTypeArguments()[0];
            }

            parameterTypes.put(paramName, paramType);
        }

        return parameterTypes;
    }

    private void cleanupObsoleteProcedures(Class<?> repositoryInterface, Map<String, String> existingProcedures) {
        Set<String> currentMethods = Arrays.stream(repositoryInterface.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        existingProcedures.keySet().stream()
                .filter(proc -> !currentMethods.contains(proc))
                .forEach(proc -> {
                    dropProcedure(proc);
                    registry.removeProcedure(proc);
                });
    }
}
