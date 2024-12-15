package com.example.storedproc.core;

public class DriverDetector {

    public static DriverType detectDriver() {
        try {
            Class.forName("jakarta.persistence.EntityManager");
            return DriverType.JPA;
        } catch (ClassNotFoundException e) {
            // Если JPA недоступен, проверяем реактивный драйвер
            try {
                Class.forName("io.r2dbc.spi.ConnectionFactory");
                return DriverType.REACTIVE;
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Не удалось определить тип драйвера. Убедитесь, что база данных настроена корректно.");
            }
        }
    }
}
