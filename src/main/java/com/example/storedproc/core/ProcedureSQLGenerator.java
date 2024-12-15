package com.example.storedproc.core;

import jakarta.persistence.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Генератор SQL для создания хранимых процедур на основе методов репозитория.
 */
public class ProcedureSQLGenerator {

    private static final Map<Class<?>, String> TYPE_MAPPING = Map.ofEntries(
            Map.entry(String.class, "VARCHAR"),
            Map.entry(Long.class, "BIGINT"),
            Map.entry(long.class, "BIGINT"),
            Map.entry(Integer.class, "INTEGER"),
            Map.entry(int.class, "INTEGER"),
            Map.entry(Short.class, "SMALLINT"),
            Map.entry(short.class, "SMALLINT"),
            Map.entry(Byte.class, "BYTE"),
            Map.entry(byte.class, "BYTE"),
            Map.entry(Character.class, "CHAR"),
            Map.entry(char.class, "CHAR"),
            Map.entry(Boolean.class, "BOOLEAN"),
            Map.entry(boolean.class, "BOOLEAN"),
            Map.entry(Double.class, "DOUBLE"),
            Map.entry(double.class, "DOUBLE"),
            Map.entry(Float.class, "FLOAT"),
            Map.entry(float.class, "FLOAT"),
            Map.entry(java.util.UUID.class, "UUID"),
            Map.entry(java.sql.Date.class, "DATE"),
            Map.entry(java.sql.Timestamp.class, "TIMESTAMP"),
            Map.entry(java.sql.Time.class, "TIME"),
            Map.entry(java.util.List.class, "ARRAY")
    );


    /**
     * Генерирует SQL для создания хранимой процедуры на основе метода.
     *
     * @param method        Метод репозитория.
     * @param entityClass   Класс сущности, связанный с процедурой.
     * @return Сгенерированный SQL.
     */
    public String generateProcedureSQL(Method method, Class<?> entityClass) {
        String procedureName = method.getName();
        String parameters = generateParameterDefinitions(method, entityClass);
        String body = generateProcedureBody(method, entityClass);

        return String.format(
                "CREATE OR REPLACE PROCEDURE %s (%s) LANGUAGE plpgsql AS $$ BEGIN %s END; $$",
                procedureName,
                parameters,
                body
        );
    }

    /**
     * Генерирует параметры для процедуры на основе сигнатуры метода и аннотаций сущности.
     */
    private String generateParameterDefinitions(Method method, Class<?> entityClass) {
        return java.util.Arrays.stream(method.getParameters())
                .map(param -> String.format("IN %s %s", param.getName(), resolveSqlType(param, entityClass)))
                .collect(Collectors.joining(", "));
    }

    /**
     * Разрешает SQL-тип на основе параметра метода и аннотаций сущности.
     */
    private String resolveSqlType(Parameter param, Class<?> entityClass) {
        if (entityClass != null) {
            Optional<String> columnType = resolveColumnType(param.getName(), entityClass);
            if (columnType.isPresent()) {
                return columnType.get();
            }
        }
        return mapJavaTypeToSQLType(param.getType());
    }

    private Optional<String> resolveColumnType(String paramName, Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getName().equals(paramName))
                .map(field -> {
                    StringBuilder columnDefinition = new StringBuilder();

                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);

                        if (!column.columnDefinition().isEmpty()) {
                            columnDefinition.append(column.columnDefinition());
                        } else {
                            // Используем тип из маппинга или TEXT по умолчанию
                            columnDefinition.append(TYPE_MAPPING.getOrDefault(field.getType(), "TEXT"));

                            // Добавляем длину, если это строка и длина указана
                            if (field.getType() == String.class && column.length() > 0) {
                                columnDefinition.append("(").append(column.length()).append(")");
                            }

                            // Обрабатываем атрибуты nullable и unique
                            if (!column.nullable()) {
                                columnDefinition.append(" NOT NULL");
                            }
                            if (column.unique()) {
                                columnDefinition.append(" UNIQUE");
                            }
                        }
                    } else if (field.isAnnotationPresent(Enumerated.class)) {
                        // Обработка для перечислений (Enum)
                        columnDefinition.append("ENUM");
                    } else {
                        // Используем маппинг типов по умолчанию
                        columnDefinition.append(TYPE_MAPPING.getOrDefault(field.getType(), "TEXT"));
                    }

                    return columnDefinition.toString();
                })
                .findFirst();
    }


    /**
     * Генерирует тело процедуры на основе метода.
     */
    private String generateProcedureBody(Method method, Class<?> entityClass) {
        String tableName = resolveTableName(entityClass);
        String query;

        if (method.getName().startsWith("find")) {
            query = generateSelectQuery(method, tableName);
        } else if (method.getName().startsWith("count")) {
            query = generateCountQuery(method, tableName);
        } else if (method.getName().startsWith("delete")) {
            query = generateDeleteQuery(method, tableName);
        } else if (method.getName().startsWith("update")) {
            query = generateUpdateQuery(method, tableName);
        } else if (method.getName().startsWith("save")) {
            query = generateInsertQuery(method, tableName);
        } else if (method.getName().startsWith("exists")) {
            query = generateExistsQuery(method, tableName);
        } else if (method.getName().startsWith("aggregate")) {
            query = generateAggregateQuery(method, tableName);
        } else if (method.getName().startsWith("bulkUpdate")) {
            query = generateBulkUpdateQuery(method, tableName);
        } else if (method.getName().startsWith("bulkDelete")) {
            query = generateBulkDeleteQuery(method, tableName);
        } else if (method.getName().startsWith("search")) {
            query = generateSearchQuery(method, tableName);
        } else {
            throw new UnsupportedOperationException("Invalid method name pattern: " + method.getName());
        }

        return query + ";";
    }


    private String generateExistsQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        return String.format("SELECT CASE WHEN EXISTS (SELECT 1 FROM %s %s) THEN 1 ELSE 0 END", tableName, conditions);
    }

    private String generateAggregateQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        String aggregateFunction = extractAggregateFunctionFromMethodName(method.getName());
        return String.format("SELECT %s(column_name) FROM %s %s", aggregateFunction, tableName, conditions);
    }

    private String extractAggregateFunctionFromMethodName(String methodName) {
        if (methodName.contains("Sum")) return "SUM";
        if (methodName.contains("Avg")) return "AVG";
        if (methodName.contains("Min")) return "MIN";
        if (methodName.contains("Max")) return "MAX";
        if (methodName.contains("Custom")) {
            // Можно добавить логику для кастомных функций через маппинг или аннотацию
            throw new UnsupportedOperationException("Custom aggregate functions require explicit mapping.");
        }
        throw new UnsupportedOperationException("Unknown aggregate function in method name: " + methodName);
    }

    private String generateBulkUpdateQuery(Method method, String tableName) {
        String setClause = generateSetClause(method);
        String whereClause = generateWhereClause(method);
        return String.format("UPDATE %s SET %s %s", tableName, setClause, whereClause);
    }

    private String generateBulkDeleteQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        return String.format("DELETE FROM %s %s", tableName, conditions);
    }

    private String generateSearchQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        String orderBy = generateOrderByClause(method);
        String limitOffset = generatePagingClause(method);
        return String.format("SELECT * FROM %s %s %s %s", tableName, conditions, orderBy, limitOffset).trim();
    }

    private String generateSetClause(Method method) {
        // Параметры метода используются для генерации пар "колонка = значение"
        return Arrays.stream(method.getParameters())
                .map(param -> String.format("%s = %s", param.getName(), param.getName()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Генерирует SELECT-запрос.
     */
    private String generateSelectQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        String orderBy = generateOrderByClause(method);
        String limitOffset = generatePagingClause(method);
        return String.format("SELECT * FROM %s %s %s %s", tableName, conditions, orderBy, limitOffset).trim();
    }

    /**
     * Генерирует COUNT-запрос.
     */
    private String generateCountQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        return String.format("SELECT COUNT(*) FROM %s %s", tableName, conditions);
    }

    /**
     * Генерирует DELETE-запрос.
     */
    private String generateDeleteQuery(Method method, String tableName) {
        String conditions = generateWhereClause(method);
        return String.format("DELETE FROM %s %s", tableName, conditions);
    }

    /**
     * Генерирует INSERT-запрос.
     */
    private String generateInsertQuery(Method method, String tableName) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 1 && parameters[0].getType().isAnnotationPresent(Entity.class)) {
            // Если параметр - сущность, разбиваем её на поля
            Class<?> entityClass = parameters[0].getType();

            // Генерация списка колонок и значений
            String columns = Arrays.stream(entityClass.getDeclaredFields())
                    .filter(field -> !field.isAnnotationPresent(Transient.class)) // Пропускаем @Transient
                    .map(field -> field.isAnnotationPresent(Column.class)
                            ? field.getAnnotation(Column.class).name()
                            : field.getName())
                    .collect(Collectors.joining(", "));

            String values = Arrays.stream(entityClass.getDeclaredFields())
                    .filter(field -> !field.isAnnotationPresent(Transient.class))
                    .map(field -> "" + field.getName())
                    .collect(Collectors.joining(", "));

            return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
        }

        throw new IllegalArgumentException("Метод для INSERT должен иметь один параметр — сущность.");
    }


    /**
     * Генерирует UPDATE-запрос.
     */
    private String generateUpdateQuery(Method method, String tableName) {
        String methodName = method.getName();
        String setPart = methodName.substring(0, methodName.indexOf("By")); // Получаем часть до "By"
        String wherePart = methodName.substring(methodName.indexOf("By") + 2); // Получаем часть после "By"

        // Параметры для SET
        String setClause = Arrays.stream(method.getParameters())
                .filter(param -> setPart.toLowerCase().contains(param.getName().toLowerCase()))
                .map(param -> String.format("%s = %s", param.getName(), param.getName()))
                .collect(Collectors.joining(", "));

        // Параметры для WHERE
        String whereClause = Arrays.stream(method.getParameters())
                .filter(param -> wherePart.toLowerCase().contains(param.getName().toLowerCase()))
                .map(param -> String.format("%s = %s", param.getName(), param.getName()))
                .collect(Collectors.joining(" AND "));

        return String.format("UPDATE %s SET %s WHERE %s", tableName, setClause, whereClause);
    }


    /**
     * Генерирует WHERE-условия на основе параметров метода.
     */
    private String generateWhereClause(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) return "";

        return "WHERE " + Arrays.stream(parameters)
                .map(param -> {
                    String operator = resolveOperator(param.getName());
                    if (operator.equals("BETWEEN")) {
                        return String.format("%s BETWEEN :%sStart AND :%sEnd",
                                param.getName().replace("Between", ""),
                                param.getName(),
                                param.getName()
                        );
                    }
                    return String.format("%s %s %s", param.getName(), operator, param.getName());
                })
                .collect(Collectors.joining(" AND "));
    }

    /**
     * Генерирует ORDER BY для запросов с сортировкой.
     */
    private String generateOrderByClause(Method method) {
        String methodName = method.getName();

        if (!methodName.contains("OrderBy")) {
            return ""; // Если сортировки нет, возвращаем пустую строку
        }

        String orderByPart = methodName.substring(methodName.indexOf("OrderBy") + 7); // Убираем "OrderBy"
        String[] orderSegments = orderByPart.split("(?<=Asc|Desc)(?=[A-Z])"); // Разделяем по Asc/Desc с учётом регистров

        return "ORDER BY " + Arrays.stream(orderSegments)
                .map(segment -> {
                    String direction = segment.endsWith("Desc") ? "DESC" : "ASC";
                    String fieldName = segment.replaceAll("(Asc|Desc)$", ""); // Убираем Asc/Desc
                    if (fieldName.isEmpty()) {
                        throw new IllegalArgumentException("Некорректный формат имени метода: " + methodName);
                    }
                    return fieldName.toLowerCase() + " " + direction; // Генерируем SQL
                })
                .collect(Collectors.joining(", "));
    }


    /**
     * Генерирует LIMIT и OFFSET для постраничной навигации.
     */
    private String generatePagingClause(Method method) {
        Parameter[] parameters = method.getParameters();
        boolean hasPage = java.util.Arrays.stream(parameters).anyMatch(p -> p.getName().equalsIgnoreCase("page"));
        boolean hasSize = java.util.Arrays.stream(parameters).anyMatch(p -> p.getName().equalsIgnoreCase("size"));

        if (hasPage && hasSize) {
            return "LIMIT :size OFFSET (:page - 1) * :size";
        }
        return "";
    }

    /**
     * Разрешает оператор для условия на основе соглашений об именах.
     */
    private String resolveOperator(String paramName) {
        if (paramName.endsWith("Like")) {
            return "LIKE";
        } else if (paramName.endsWith("ILike")) {
            return "ILIKE";
        } else if (paramName.endsWith("Between")) {
            return "BETWEEN";
        } else if (paramName.endsWith("GreaterThan")) {
            return ">";
        } else if (paramName.endsWith("LessThan")) {
            return "<";
        } else if (paramName.endsWith("GreaterThanOrEqual")) {
            return ">=";
        } else if (paramName.endsWith("LessThanOrEqual")) {
            return "<=";
        } else if (paramName.endsWith("In")) {
            return "IN";
        } else if (paramName.endsWith("IsNull")) {
            return "IS NULL";
        }
        return "="; // По умолчанию равенство
    }


    /**
     * Разрешает имя таблицы из класса сущности.
     */
    private String resolveTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (!table.name().isEmpty()) {
                return table.name();
            }
        }
        if (entityClass.isAnnotationPresent(Entity.class)) {
            // Если имя таблицы не указано явно, использовать имя класса в нижнем регистре
            return entityClass.getSimpleName().toLowerCase();
        }
        throw new IllegalArgumentException("Класс " + entityClass.getName() + " не является JPA-сущностью");
    }

    /**
     * Преобразует Java-тип в SQL-тип.
     */
    private String mapJavaTypeToSQLType(Class<?> javaType) {
        // Если это сущность (имеет аннотацию @Entity)
        if (javaType.isAnnotationPresent(Entity.class)) {
            return javaType.getName();
        }

        String sqlType = TYPE_MAPPING.get(javaType);
        if (sqlType == null) {
            throw new IllegalArgumentException("Неизвестный тип параметра: " + javaType.getName());
        }
        return sqlType;
    }
}
