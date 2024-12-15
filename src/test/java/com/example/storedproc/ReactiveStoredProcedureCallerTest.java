package com.example.storedproc;

import com.example.storedproc.core.DatabaseAdapter;
import com.example.storedproc.core.StoredProcedureCaller;
import com.example.storedproc.core.StoredProcedureRegistry;
import com.example.storedproc.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveStoredProcedureCallerTest {

    @Mock
    private DatabaseAdapter databaseAdapter;
    private StoredProcedureRegistry registry;
    private StoredProcedureCaller<User> userCaller;
    private StoredProcedureCaller<Void> voidCaller;
    private StoredProcedureCaller<User> multipleParamsCaller;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        when(databaseAdapter.isReactive()).thenReturn(true);

        userCaller = new StoredProcedureCaller<>(
                "get_user_by_id",
                ProcedureSQLGeneratorTest.TestRepository.class.getMethod("getUserById", Long.class),
                User.class,
                databaseAdapter
        );

        voidCaller = new StoredProcedureCaller<>(
                "delete_user",
                ProcedureSQLGeneratorTest.TestRepository.class.getMethod("deleteUser", Long.class),
                Void.class,
                databaseAdapter
        );

        multipleParamsCaller = new StoredProcedureCaller<>(
                "find_user",
                ProcedureSQLGeneratorTest.TestRepository.class.getMethod("findByNameAndAge", String.class, Integer.class),
                User.class,
                databaseAdapter
        );
    }

    @Test
    void shouldSuccessfullyReturnSingleUser() {
        User expectedUser = new User(1L, "Test User", 25);
        when(databaseAdapter.executeReactive("get_user_by_id", Map.of("p0", 1L), User.class))
                .thenReturn(Mono.just(expectedUser));

        StepVerifier.create((Publisher<User>) userCaller.call(1L))
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void shouldSuccessfullyReturnMultipleUsers() {
        User user1 = new User(1L, "User 1", 25);
        User user2 = new User(2L, "User 2", 30);
        when(databaseAdapter.executeReactive("get_user_by_id", Map.of("p0", 1L), User.class))
                .thenReturn(Flux.fromIterable(Arrays.asList(user1, user2)));

        StepVerifier.create((Publisher<User>) userCaller.call(1L))
                .expectNext(user1)
                .expectNext(user2)
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyResult() {
        when(databaseAdapter.executeReactive("get_user_by_id", Map.of("p0", 999L), User.class))
                .thenReturn(Flux.empty());

        StepVerifier.create((Publisher<User>) userCaller.call(999L))
                .verifyComplete();
    }

    @Test
    void shouldHandleVoidResult() {
        when(databaseAdapter.executeReactive("delete_user", Map.of("p0", 1L), Void.class))
                .thenReturn(Mono.empty());

        StepVerifier.create((Publisher<Void>) voidCaller.call(1L))
                .verifyComplete();
    }

    @Test
    void shouldHandleMultipleParameters() {
        User expectedUser = new User(1L, "Test User", 25);
        when(databaseAdapter.executeReactive("find_user",
                Map.of("p0", "Test User", "p1", 25), User.class))
                .thenReturn(Mono.just(expectedUser));

        StepVerifier.create((Publisher<User>) multipleParamsCaller.call("Test User", 25))
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void shouldHandleDatabaseError() {
        when(databaseAdapter.executeReactive("get_user_by_id", Map.of("p0", 1L), User.class))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        StepVerifier.create((Publisher<User>) userCaller.call(1L))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleNullParameter() {
        User expectedUser = new User(1L, null, 25);
        when(databaseAdapter.executeReactive("get_user_by_id", Map.of("p0", null), User.class))
                .thenReturn(Mono.just(expectedUser));

        StepVerifier.create((Publisher<User>) userCaller.call((Object) null))
                .expectNext(expectedUser)
                .verifyComplete();
    }
}
