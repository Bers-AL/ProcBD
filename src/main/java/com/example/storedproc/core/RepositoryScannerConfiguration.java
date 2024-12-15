package com.example.storedproc.core;

import com.example.storedproc.annotations.StoredProceduresRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryScannerConfiguration {

    @Bean
    public BeanPostProcessor repositoryScanner(RepositoryFactory repositoryFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean.getClass().isAnnotationPresent(StoredProceduresRepository.class)) {
                    Class<?> repositoryInterface = bean.getClass();
                    return repositoryFactory.createRepository(repositoryInterface);
                }
                return bean;
            }
        };
    }
}

