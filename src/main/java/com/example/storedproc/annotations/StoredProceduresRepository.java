package com.example.storedproc.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Помечает интерфейс как репозиторий для работы с хранимыми процедурами.
 * Все методы внутри будут автоматически интерпретироваться как методы,
 * для которых нужно генерировать хранимые процедуры.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StoredProceduresRepository {
}
