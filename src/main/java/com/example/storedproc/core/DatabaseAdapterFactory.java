package com.example.storedproc.core;

import jakarta.persistence.EntityManager;
import io.r2dbc.spi.ConnectionFactory;

import java.util.Objects;

public class DatabaseAdapterFactory {

    /**
     * Создает адаптер базы данных в зависимости от типа драйвера.
     *
     * @param entityManager     JPA EntityManager (может быть null для реактивного режима).
     * @param connectionFactory R2DBC ConnectionFactory (может быть null для JPA).
     * @return Экземпляр DatabaseAdapter.
     */
    public static DatabaseAdapter createAdapter(EntityManager entityManager, ConnectionFactory connectionFactory) {
        DriverType driverType = DriverDetector.detectDriver();

        if (Objects.requireNonNull(driverType) == DriverType.JPA) {
            if (entityManager == null) {
                throw new IllegalArgumentException("EntityManager не может быть null для JPA адаптера.");
            }
            return new JpaDatabaseAdapter(entityManager);
        } else if (driverType == DriverType.REACTIVE) {
            if (connectionFactory == null) {
                throw new IllegalArgumentException("ConnectionFactory не может быть null для реактивного адаптера.");
            }
            return new ReactiveDatabaseAdapter(connectionFactory);
        }
        throw new IllegalStateException("Неизвестный тип драйвера: " + driverType);
    }
}
