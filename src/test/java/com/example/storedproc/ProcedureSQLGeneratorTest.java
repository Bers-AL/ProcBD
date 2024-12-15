package com.example.storedproc;

import com.example.storedproc.annotations.StoredProceduresRepository;
import com.example.storedproc.core.ProcedureSQLGenerator;
import com.example.storedproc.entities.User;
import com.example.storedproc.utils.SQLUtils;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProcedureSQLGeneratorTest {

    private ProcedureSQLGenerator sqlGenerator;

    @BeforeEach
    void setUp() {
        sqlGenerator = new ProcedureSQLGenerator();
    }

    @StoredProceduresRepository
    interface TestRepository {
        User findById(Long id);
        List<User> findByNameAndAge(String name, Integer age);
        List<User> findByAgeOrderByNameDesc(Integer age);
        Long countByStatus(String status);
        void deleteById(Long id);
        void updateNameById(String name, Long id);
        void save(User user);
    }

    @Test
    void shouldGenerateSelectProcedure() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findById", Long.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "findById",
                        "IN id BIGINT",
                        "SELECT * FROM users WHERE id = id;"
                )
        );
    }

    @Test
    void shouldGenerateSelectWithMultipleConditions() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByNameAndAge", String.class, Integer.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "findByNameAndAge",
                        "IN name VARCHAR, IN age INTEGER",
                        "SELECT * FROM users WHERE name = name AND age = age;"
                )
        );
    }

    @Test
    void shouldGenerateSelectWithOrderBy() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByAgeOrderByNameDesc", Integer.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "findByAgeOrderByNameDesc",
                        "IN age INTEGER",
                        "SELECT * FROM users WHERE age = age ORDER BY name DESC;"
                )
        );
    }

    @Test
    void shouldGenerateCountProcedure() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("countByStatus", String.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "countByStatus",
                        "IN status VARCHAR",
                        "SELECT COUNT(*) FROM users WHERE status = status;"
                )
        );
    }

    @Test
    void shouldGenerateDeleteProcedure() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("deleteById", Long.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "deleteById",
                        "IN id BIGINT",
                        "DELETE FROM users WHERE id = id;"
                )
        );
    }

    @Test
    void shouldGenerateUpdateProcedure() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("updateNameById", String.class, Long.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "updateNameById",
                        "IN name VARCHAR, IN id BIGINT",
                        "UPDATE users SET name = name WHERE id = id;"
                )
        );
    }

    @Test
    void shouldGenerateInsertProcedure() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("save", User.class);
        String sql = sqlGenerator.generateProcedureSQL(method, User.class);

        assertThat(sql).isEqualTo(
                SQLUtils.generateCreateProcedureSQL(
                        "save",
                        "IN user " + User.class.getName(),
                        "INSERT INTO users (id, name, age) VALUES (id, name, age);"
                )
        );
    }

    @Test
    void shouldHandleNullTableName() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findById", Long.class);
        assertThatThrownBy(() -> sqlGenerator.generateProcedureSQL(method, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cannot invoke \"java.lang.Class.isAnnotationPresent(java.lang.Class)\" because \"entityClass\" is null");
    }

    @Test
    void shouldHandleEmptyTableName() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByNameAndAge", String.class, Integer.class);

        // Создаем класс без @Table или с пустым name()
        @Entity
        class TestEntity {}

        String sql = sqlGenerator.generateProcedureSQL(method, TestEntity.class);

        assertThat(sql).isEqualTo(
                "CREATE OR REPLACE PROCEDURE findByNameAndAge (IN name VARCHAR, IN age INTEGER) " +
                        "LANGUAGE plpgsql AS $$ " +
                        "BEGIN " +
                        "SELECT * FROM testentity WHERE name = name AND age = age; " +
                        "END; " +
                        "$$"
        );
    }

    @Test
    void shouldHandleNullMethod() {
        assertThatThrownBy(() -> sqlGenerator.generateProcedureSQL(null, User.class))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cannot invoke \"java.lang.reflect.Method.getName()\" because \"method\" is null");
    }

    @Test
    void shouldHandleInvalidMethodName() throws NoSuchMethodException {
        Method method = Object.class.getMethod("toString");
        assertThatThrownBy(() -> sqlGenerator.generateProcedureSQL(method, User.class))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Invalid method name pattern: " + method.getName());
    }
}
