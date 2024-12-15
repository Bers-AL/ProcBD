package com.example.storedproc.core;

import com.example.storedproc.annotations.StoredProceduresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.Reflections.log;

@Component
@DependsOn("databaseAdapter") // Убедитесь, что адаптер базы данных проинициализирован
public class StoredProcedureInitializer {

    private final DatabaseAdapter databaseAdapter;
    private final StoredProcedureRegistry registry;
    private final ProcedureSQLGenerator sqlGenerator;

    public StoredProcedureInitializer(
            @Autowired DatabaseAdapter databaseAdapter,
            @Autowired StoredProcedureRegistry registry,
            @Autowired ProcedureSQLGenerator sqlGenerator) {
        this.databaseAdapter = databaseAdapter;
        this.registry = registry;
        this.sqlGenerator = sqlGenerator;
    }

    @PostConstruct
    public void initialize() {
        StoredProcedureSynchronizer synchronizer = new StoredProcedureSynchronizer(
                databaseAdapter, registry, sqlGenerator);

        try {
            scanRepositories().forEach(repositoryClass -> {
                try {
                    Class<?> entityClass = determineEntityClass(repositoryClass);
                    synchronizer.synchronizeProcedures(repositoryClass, entityClass);
                } catch (Exception e) {
                    log.error("Ошибка синхронизации для репозитория {}: {}",
                            repositoryClass.getName(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Ошибка инициализации хранимых процедур", e);
            throw new IllegalStateException("Ошибка инициализации хранимых процедур", e);
        }
    }

    private Set<Class<?>> scanRepositories() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(StoredProceduresRepository.class));

        // Сканируем все пакеты в classpath
        Set<BeanDefinition> candidates = new HashSet<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources("");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                candidates.addAll(scanner.findCandidateComponents(resource.getPath()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка сканирования classpath", e);
        }

        if (candidates.isEmpty()) {
            log.warn("Не найдено репозиториев с аннотацией @StoredProceduresRepository");
        }

        return candidates.stream()
                .map(this::loadClass)
                .collect(Collectors.toSet());
    }

    private Class<?> loadClass(BeanDefinition definition) {
        try {
            return Class.forName(definition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Не удалось загрузить класс: " +
                    definition.getBeanClassName(), e);
        }
    }

    private Class<?> determineEntityClass(Class<?> repositoryClass) {
        // Анализируем сигнатуры методов репозитория для определения класса сущности
        for (Method method : repositoryClass.getMethods()) {
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                for (Type actualType : parameterizedType.getActualTypeArguments()) {
                    if (actualType instanceof Class) {
                        return (Class<?>) actualType;
                    }
                }
            } else if (returnType instanceof Class) {
                return (Class<?>) returnType;
            }
        }
        throw new IllegalStateException("Не удалось определить класс сущности для репозитория: " + repositoryClass.getName());
    }
}
