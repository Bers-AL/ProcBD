package com.example.storedproc;

import com.example.storedproc.utils.ReturnTypeResolver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnTypeResolverTest {

    interface TestRepository {
        User findById(Long id);
        List<User> findAll();
        void save(User user);
        Optional<User> findByEmail(String email);
        CompletableFuture<User> findByIdAsync(Long id);
        Mono<User> findByIdReactive(Long id);
        Flux<User> findAllReactive();
        Optional<List<User>> findByAgeGreaterThan(int age);
        CompletableFuture<List<User>> findByNameAsync(String name);
        Mono<List<User>> findByNameReactive(String name);
    }

    @Test
    void shouldResolveSingleRowType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findById", Long.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("SINGLE ROW");
    }

    @Test
    void shouldResolveTableType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findAll");
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("TABLE");
    }

    @Test
    void shouldResolveVoidType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("save", User.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("VOID");
    }

    @Test
    void shouldResolveOptionalSingleType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByEmail", String.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("SINGLE ROW");
    }

    @Test
    void shouldResolveOptionalTableType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByAgeGreaterThan", int.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("TABLE");
    }

    @Test
    void shouldResolveCompletableFutureSingleType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByIdAsync", Long.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("SINGLE ROW");
    }

    @Test
    void shouldResolveCompletableFutureTableType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByNameAsync", String.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("TABLE");
    }

    @Test
    void shouldResolveMonoType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByIdReactive", Long.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("SINGLE ROW");
    }

    @Test
    void shouldResolveFluxType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findAllReactive");
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("SINGLE ROW");
    }

    @Test
    void shouldResolveMonoTableType() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByNameReactive", String.class);
        assertThat(ReturnTypeResolver.determineReturnType(method)).isEqualTo("TABLE");
    }

    static class User {
        private Long id;
        private String name;
        private String email;
        private int age;
    }
}
