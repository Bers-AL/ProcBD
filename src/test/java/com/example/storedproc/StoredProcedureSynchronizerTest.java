package com.example.storedproc;

import com.example.storedproc.annotations.StoredProceduresRepository;
import com.example.storedproc.core.DatabaseAdapter;
import com.example.storedproc.core.ProcedureSQLGenerator;
import com.example.storedproc.core.StoredProcedureCaller;
import com.example.storedproc.core.StoredProcedureRegistry;
import com.example.storedproc.core.StoredProcedureSynchronizer;
import com.example.storedproc.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoredProcedureSynchronizerTest {

    @Mock
    private DatabaseAdapter adapter;

    @Mock
    private StoredProcedureRegistry registry;

    @Mock
    private ProcedureSQLGenerator sqlGenerator;

    private StoredProcedureSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new StoredProcedureSynchronizer(adapter, registry, sqlGenerator);
    }

    @StoredProceduresRepository
    interface TestRepository {
        User findById(Long id);
        List<User> findAll();
        void save(User user);
    }

    @StoredProceduresRepository
    interface EmptyRepository {
    }

    @Test
    void shouldCreateTableIfNotExists() {
        // Given
        String checkTableSQL = """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_name = 'stored_procedures'
    """;

        String createTableSQL = """
        CREATE TABLE stored_procedures (
            id SERIAL PRIMARY KEY,
            procedure_name VARCHAR(255) NOT NULL UNIQUE
        );
    """;

        when(adapter.execute(eq(checkTableSQL), eq(Map.of()), eq(Long.class)))
                .thenReturn(0L);
        when(adapter.execute(eq(createTableSQL), eq(Map.of()), eq(Void.class)))
                .thenReturn(null);
        when(adapter.execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class)))
                .thenReturn(Collections.emptyList());

        // When
        synchronizer.synchronizeProcedures(TestRepository.class, User.class);

        // Then
        verify(adapter).execute(eq(createTableSQL), eq(Map.of()), eq(Void.class));
        verify(adapter).execute(eq(checkTableSQL), eq(Map.of()), eq(Long.class));
        verify(adapter).execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class));
    }

    @Test
    void shouldNotCreateTableIfExists() {
        // Given
        when(adapter.execute(contains("SELECT COUNT(*)"), eq(Map.of()), eq(Long.class)))
                .thenReturn(1L);

        // When
        synchronizer.synchronizeProcedures(TestRepository.class, User.class);

        // Then
        verify(adapter, never()).execute(contains("CREATE TABLE"), any(), any());
    }

    @Test
    void shouldCreateNewProcedures() {
        // Given
        when(adapter.execute(contains("SELECT COUNT(*)"), eq(Map.of()), eq(Long.class)))
                .thenReturn(1L);
        when(adapter.execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class)))
                .thenReturn(Collections.emptyList());
        when(sqlGenerator.generateProcedureSQL(any(), eq(User.class)))
                .thenReturn("SQL1")
                .thenReturn("SQL2")
                .thenReturn("SQL3");

        // When
        synchronizer.synchronizeProcedures(TestRepository.class, User.class);

        // Then
        verify(adapter, times(3)).execute(anyString(), eq(Map.of()), eq(Void.class));
        verify(registry, times(3)).registerProcedure(anyString(), any());
    }

    @Test
    void shouldUpdateExistingProcedures() {
        // Given
        when(adapter.execute(contains("SELECT COUNT(*)"), eq(Map.of()), eq(Long.class)))
                .thenReturn(1L);
        when(adapter.execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class)))
                .thenReturn(Arrays.asList("findById", "findAll"));
        when(sqlGenerator.generateProcedureSQL(any(), eq(User.class)))
                .thenReturn("NEW_SQL");

        // When
        synchronizer.synchronizeProcedures(TestRepository.class, User.class);

        // Then
        verify(adapter, times(2)).execute(contains("DROP PROCEDURE"), any(), any());
        verify(adapter, times(3)).execute(contains("NEW_SQL"), any(), any());
        verify(registry, times(3)).registerProcedure(anyString(), any(StoredProcedureCaller.class));
    }

    @Test
    void shouldRemoveObsoleteProcedures() {
        // Given
        when(adapter.execute(contains("SELECT COUNT(*)"), eq(Map.of()), eq(Long.class)))
                .thenReturn(1L);
        when(adapter.execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class)))
                .thenReturn(Arrays.asList("oldProc1", "oldProc2"));

        // Мокируем генерацию SQL для каждого метода в TestRepository
        when(sqlGenerator.generateProcedureSQL(any(), any()))
                .thenReturn("CREATE OR REPLACE PROCEDURE ...");

        // When
        synchronizer.synchronizeProcedures(TestRepository.class, User.class);

        // Then
        verify(adapter, times(2)).execute(contains("DROP PROCEDURE"), any(), any());
        verify(registry, times(2)).removeProcedure(anyString());
        verify(registry, times(3)).registerProcedure(anyString(), any(StoredProcedureCaller.class));
    }

    @Test
    void shouldHandleEmptyRepository() {
//        // Given
//        when(adapter.execute(contains("SELECT COUNT(*)"), eq(Map.of()), eq(Long.class)))
//                .thenReturn(1L);  // Таблица существует
//        when(adapter.execute(contains("SELECT procedure_name"), eq(Map.of()), eq(List.class)))
//                .thenReturn(Collections.emptyList());  // Нет процедур

        // When & Then
        assertDoesNotThrow(() ->
                synchronizer.synchronizeProcedures(EmptyRepository.class, User.class)
        );
    }

    @Test
    void shouldFailOnNonRepositoryInterface() {
        // Given
        class NotARepo {}

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                synchronizer.synchronizeProcedures(NotARepo.class, User.class)
        );
    }

    @Test
    void shouldFailOnNullParameters() {
        assertThrows(NullPointerException.class, () ->
                synchronizer.synchronizeProcedures(null, User.class)
        );

        assertThrows(NullPointerException.class, () ->
                synchronizer.synchronizeProcedures(TestRepository.class, null)
        );
    }

    @Test
    void shouldHandleDatabaseErrors() {
        // Given
        when(adapter.execute(anyString(), any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                synchronizer.synchronizeProcedures(TestRepository.class, User.class)
        );
    }
}
