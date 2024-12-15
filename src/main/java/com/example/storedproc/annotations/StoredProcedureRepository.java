package com.example.storedproc.annotations;

import java.lang.annotation.*;

/**
 * Аннотация для пометки интерфейса как репозитория хранимых процедур.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StoredProcedureRepository {
}
