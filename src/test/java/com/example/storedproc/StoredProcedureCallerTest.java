package com.example.storedproc;

import com.example.storedproc.core.DatabaseAdapter;
import com.example.storedproc.core.StoredProcedureCaller;
import com.example.storedproc.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoredProcedureCallerTest {

    @Mock
    private DatabaseAdapter databaseAdapter;
    private StoredProcedureCaller<User> caller;
    private Method getUserByIdMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        getUserByIdMethod = ProcedureSQLGeneratorTest.TestRepository.class.getMethod("getUserById", Long.class);

        caller = new StoredProcedureCaller<>(
                "get_user_by_id",
                getUserByIdMethod,
                User.class,
                databaseAdapter
        );
    }

    @Test
    void shouldCallSynchronously() {
        User expectedUser = new User(1L, "Test User", 25);
        when(databaseAdapter.execute("get_user_by_id", Map.of("p0", 1L), User.class))
                .thenReturn(expectedUser);

        User result = (User) caller.call(1L);

        assertThat(result)
                .isNotNull()
                .extracting(User::getId, User::getName, User::getAge)
                .containsExactly(1L, "Test User", 25);
    }

    @Test
    void shouldHandleVoidResult() throws NoSuchMethodException {
        Method deleteMethod = ProcedureSQLGeneratorTest.TestRepository.class.getMethod("deleteUser", Long.class);
        StoredProcedureCaller<Void> voidCaller = new StoredProcedureCaller<>(
                "delete_user",
                deleteMethod,
                Void.TYPE,
                databaseAdapter
        );

        voidCaller.call(1L);

        verify(databaseAdapter).execute("delete_user", Map.of("p0", 1L), Void.TYPE);
    }

    @Test
    void shouldHandleListResult() throws NoSuchMethodException {
        Method findUsersMethod = ProcedureSQLGeneratorTest.TestRepository.class.getMethod("findUsersByAge", Integer.class);
        StoredProcedureCaller<List<User>> listCaller = new StoredProcedureCaller<>(
                "find_users",
                findUsersMethod,
                (Class<List<User>>) (Class<?>) List.class,
                databaseAdapter
        );

        List<User> expectedUsers = List.of(
                new User(1L, "User 1", 25),
                new User(2L, "User 2", 25)
        );

        when(databaseAdapter.execute("find_users", Map.of("p0", 25), List.class))
                .thenReturn(expectedUsers);

        List<User> result = (List<User>) listCaller.call(25);

        assertThat(result)
                .hasSize(2)
                .extracting(User::getName)
                .containsExactly("User 1", "User 2");
    }

    @Test
    void shouldHandleNullResult() {
        when(databaseAdapter.execute("get_user_by_id", Map.of("p0", 999L), User.class))
                .thenReturn(null);

        User result = (User) caller.call(999L);

        assertThat(result).isNull();
    }

    @Test
    void shouldHandleDatabaseError() {
        when(databaseAdapter.execute("get_user_by_id", Map.of("p0", 1L), User.class))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> caller.call(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    void shouldHandleMultipleParameters() throws NoSuchMethodException {
        Method findByNameAndAgeMethod = ProcedureSQLGeneratorTest.TestRepository.class.getMethod("findByNameAndAge", String.class, Integer.class);
        StoredProcedureCaller<User> multiParamCaller = new StoredProcedureCaller<>(
                "find_user_by_name_and_age",
                findByNameAndAgeMethod,
                User.class,
                databaseAdapter
        );

        User expectedUser = new User(1L, "Test User", 25);
        when(databaseAdapter.execute("find_user_by_name_and_age",
                Map.of("p0", "Test User", "p1", 25), User.class))
                .thenReturn(expectedUser);

        User result = (User) multiParamCaller.call("Test User", 25);

        assertThat(result)
                .isNotNull()
                .extracting(User::getName, User::getAge)
                .containsExactly("Test User", 25);
    }
}