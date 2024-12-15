package com.example.storedproc.core;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.example.storedproc.annotations.StoredProceduresRepository;

import java.util.Set;

public class StoredProceduresRepositoriesRegistrar {

    private final StoredProcedureRegistry registry;

    public StoredProceduresRepositoriesRegistrar(StoredProcedureRegistry registry) {
        this.registry = registry;
    }

    public void registerRepositories(BeanDefinitionRegistry registry, String basePackages) {
        // Сканируем пакеты на наличие аннотации @StoredProceduresRepository
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(StoredProceduresRepository.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackages);

        for (BeanDefinition candidate : candidates) {
            String beanClassName = candidate.getBeanClassName();
            registerRepository(registry, beanClassName);
        }
    }

    private void registerRepository(BeanDefinitionRegistry registry, String repositoryClassName) {
        try {
            // Загружаем класс репозитория
            Class<?> repositoryClass = Class.forName(repositoryClassName);

            // Создаём фабрику для репозитория
            StoredProcedureRepositoryFactoryBean<?> factoryBean =
                    new StoredProcedureRepositoryFactoryBean<>(
                            (Class<Object>) repositoryClass,
                            this.registry
                    );
            // Регистрируем фабрику как бин
            DefaultListableBeanFactory factory = (DefaultListableBeanFactory) registry;
            factory.registerSingleton(repositoryClassName, factoryBean.getObject());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Не удалось загрузить класс репозитория: " + repositoryClassName, e);
        }
    }
}
