package com.example.storedproc.annotations;

import com.example.storedproc.core.StoredProceduresRepositoriesRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(StoredProceduresRepositoriesRegistrar.class)
public @interface EnableStoredProceduresRepositories {
    String basePackages() default ""; // Базовый пакет для сканирования репозиториев
}
