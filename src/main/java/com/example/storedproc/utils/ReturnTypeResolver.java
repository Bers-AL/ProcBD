package com.example.storedproc.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReturnTypeResolver {

    /**
     * Определяет SQL-тип возвращаемого значения на основе типа метода.
     *
     * @param method Метод, аннотированный @StoredProcedure
     * @return SQL-тип возвращаемого значения (TABLE, SINGLE ROW, VOID)
     */
    public static String determineReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        // 1. Реактивные типы
        if (Mono.class.isAssignableFrom(returnType) || Flux.class.isAssignableFrom(returnType)) {
            return resolveReactiveReturnType(method);
        }

        // 2. Асинхронные типы
        if (CompletableFuture.class.isAssignableFrom(returnType)) {
            return resolveGenericType(method, CompletableFuture.class, "SINGLE ROW", "TABLE");
        }

        // 3. Коллекции и массивы
        if (Iterable.class.isAssignableFrom(returnType) || returnType.isArray()) {
            return "TABLE"; // Коллекции и массивы интерпретируются как множество строк
        }

        // 4. Optional
        if (Optional.class.isAssignableFrom(returnType)) {
            return resolveGenericType(method, Optional.class, "SINGLE ROW", "TABLE");
        }

        // 5. Примитивные и одиночные типы
        if (void.class.isAssignableFrom(returnType)) {
            return "VOID"; // Процедура ничего не возвращает
        }

        // По умолчанию — одиночное значение
        return "SINGLE ROW";
    }

    /**
     * Определяет тип возвращаемого значения для реактивных методов (Mono/Flux).
     */
    private static String resolveReactiveReturnType(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            Class<?> innerType = getRawType(parameterizedType.getActualTypeArguments()[0]);

            // Если внутренний тип — коллекция или массив
            if (Iterable.class.isAssignableFrom(innerType) || innerType.isArray()) {
                return "TABLE";
            }
        }
        return "SINGLE ROW"; // Если возвращается одиночный элемент
    }

    /**
     * Определяет SQL-тип для обобщённых типов, например Optional<T>, CompletableFuture<T>.
     */
    private static String resolveGenericType(Method method, Class<?> wrapperType, String singleRowResult, String tableResult) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            Class<?> innerType = getRawType(parameterizedType.getActualTypeArguments()[0]);

            // Если внутренний тип — коллекция или массив
            if (Iterable.class.isAssignableFrom(innerType) || innerType.isArray()) {
                return tableResult;
            }
        }
        return singleRowResult; // Если возвращается одиночное значение
    }

    /**
     * Извлекает тип T из ParameterizedType.
     */
    private static Class<?> getRawType(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        throw new IllegalArgumentException("Неизвестный тип: " + type);
    }
}

