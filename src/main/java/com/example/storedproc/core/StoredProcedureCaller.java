package com.example.storedproc.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class StoredProcedureCaller<T> {
    private final String procedureName;
    private final Method method;
    private final Class<T> resultType;
    private final DatabaseAdapter adapter;

    public StoredProcedureCaller(String procedureName,
                                 Method method,
                                 Class<T> resultType,
                                 DatabaseAdapter adapter) {
        this.procedureName = procedureName;
        this.method = method;
        this.resultType = resultType;
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    public T call(Object... args) {
        Map<String, Object> parameters = mapParameters(args);
        return adapter.execute(procedureName, parameters, resultType);
    }

    private Map<String, Object> mapParameters(Object[] args) {
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < method.getParameters().length; i++) {
            parameters.put("p" + i, args[i]);
        }
        return parameters;
    }
}
