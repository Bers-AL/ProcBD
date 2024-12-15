package com.example.storedproc.core;

import com.example.storedproc.exceptions.ConfigurationException;
import org.springframework.stereotype.Component;


@Component
public class ConfigurationValidator {

    public void validateConfiguration(DatabaseAdapter databaseAdapter,
                                      StoredProcedureRegistry registry) {
        validateDatabaseAdapter(databaseAdapter);
        validateRegistry(registry);
    }

    private void validateDatabaseAdapter(DatabaseAdapter adapter) {
        if (adapter instanceof DefaultDatabaseAdapterConfiguration.NoOpDatabaseAdapter) {
            throw new ConfigurationException("DatabaseAdapter не настроен. Необходимо настроить JPA или R2DBC");
        }
    }

    private void validateRegistry(StoredProcedureRegistry registry) {
        if (registry == null || registry.isEmpty()) {
            throw new ConfigurationException("Не найдены репозитории с хранимыми процедурами");
        }
    }
}
