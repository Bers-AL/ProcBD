package com.example.storedproc;

import com.example.storedproc.core.StoredProcedureCaller;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StoredProcedureIntegrationTest {

//    @Autowired
//    private EntityManager entityManager;
//
//    @Test
//    void testCall_withRealDatabase() {
//        // Создание тестовой процедуры
//        entityManager.createNativeQuery("""
//            CREATE OR REPLACE PROCEDURE get_user_by_id(IN id BIGINT, OUT username VARCHAR)
//            AS $$
//            BEGIN
//                SELECT 'Test User' INTO username;
//            END;
//            $$ LANGUAGE plpgsql;
//        """).executeUpdate();
//
//        StoredProcedureCaller<String> caller = new StoredProcedureCaller<>(
//                "get_user_by_id",
//                Map.of("id", Long.class),
//                String.class,
//                null
//        );
//
//        String result = caller.call(entityManager, Map.of("id", 1L));
//
//        assertThat(result).isEqualTo("Test User");
//    }
}

