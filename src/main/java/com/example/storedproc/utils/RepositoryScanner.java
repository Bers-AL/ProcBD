package com.example.storedproc.utils;

import com.example.storedproc.annotations.StoredProcedureRepository;

import java.util.HashSet;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class RepositoryScanner {

    public static Set<Class<?>> findStoredProcedureRepositories(String basePackage) {
        Set<Class<?>> repositories = new HashSet<>();

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(basePackage))
                .setScanners(Scanners.TypesAnnotated));

        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(StoredProcedureRepository.class);
        repositories.addAll(annotatedClasses);

        return repositories;
    }
}
