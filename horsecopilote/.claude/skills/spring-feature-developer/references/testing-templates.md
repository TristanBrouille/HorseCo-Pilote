# Testing Templates

Tests for the `Task` feature from `code-templates.md`. Adapt names and packages to the project.

## Contents

1. Imports by Spring Boot version
2. Service unit test (JUnit 5 + Mockito)
3. Mapper unit test (JUnit 5)
4. Controller slice test (`@WebMvcTest`)
5. When to use `@SpringBootTest`

## 1. Imports by Spring Boot version

Check the Spring Boot version in `pom.xml` before writing tests. Several test classes moved between major versions.

| Element | Spring Boot 3.4+ | Spring Boot 4.x |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| Bean mocking | `org.springframework.test.context.bean.override.mockito.MockitoBean` | same |
| Test dependency | `spring-boot-starter-test` | `spring-boot-starter-test` plus `spring-boot-starter-webmvc-test` |

`@MockBean` is deprecated since Spring Boot 3.4 and removed in 4.0: use `@MockitoBean`.

Controller tests below send JSON as text blocks rather than serializing with `ObjectMapper`. This avoids depending on the Jackson version (Spring Boot 4 uses Jackson 3, whose packages changed) and keeps the request body explicit.

If the project's existing tests use different imports that compile, follow them.

## 2. Service unit test

**Tier under test: Service**

`src/test/java/com/example/demo/service/TaskServiceTest.java`

```java
package com.example.demo.service;

import com.example.demo.entity.Task;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskReturnsSavedTask() {
        Task newTask = new Task("Write tests", "Cover the service");
        when(taskRepository.save(newTask)).thenReturn(taskWithId(1L, "Write tests", "Cover the service"));

        Task result = taskService.createTask(newTask);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Write tests");
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void getAllTasksReturnsEveryEntity() {
        when(taskRepository.findAll()).thenReturn(List.of(
                taskWithId(1L, "First", null),
                taskWithId(2L, "Second", null)
        ));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).extracting(Task::getTitle).containsExactly("First", "Second");
    }

    @Test
    void getTaskThrowsWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateTaskCopiesChangesOntoExistingEntity() {
        Task existingTask = taskWithId(1L, "Old title", "Old description");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));

        Task result = taskService.updateTask(1L, new Task("New title", "New description", true));

        assertThat(result).isSameAs(existingTask);
        assertThat(existingTask.getId()).isEqualTo(1L);
        assertThat(existingTask.getTitle()).isEqualTo("New title");
        assertThat(existingTask.isCompleted()).isTrue();
    }

    @Test
    void updateTaskThrowsWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, new Task("Title", null, false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTaskThrowsWhenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void deleteTaskRemovesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }

    private Task taskWithId(Long id, String title, String description) {
        Task task = new Task(title, description);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
```

The service test only uses entities, repositories and Service-tier exceptions, mirroring the dependency rules. The id is generated by the database, so unit tests set it through `ReflectionTestUtils` rather than adding a setter to the entity just for tests.

## 3. Mapper unit test

**Tier under test: Presentation**

`src/test/java/com/example/demo/mapper/TaskMapperTest.java`

```java
package com.example.demo.mapper;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.entity.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    private final TaskMapper taskMapper = new TaskMapper();

    @Test
    void toEntityFromRequestBuildsNonPersistedTask() {
        Task task = taskMapper.toEntity(new TaskRequestDto("Write tests", "Cover the mapper"));

        assertThat(task.getId()).isNull();
        assertThat(task.getTitle()).isEqualTo("Write tests");
        assertThat(task.getDescription()).isEqualTo("Cover the mapper");
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void toEntityFromUpdateCopiesCompletedFlag() {
        Task task = taskMapper.toEntity(new TaskUpdateDto("New title", "New description", true));

        assertThat(task.getId()).isNull();
        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.isCompleted()).isTrue();
    }

    @Test
    void toResponseCopiesEveryField() {
        Task task = new Task("Write tests", "Cover the mapper", true);
        ReflectionTestUtils.setField(task, "id", 1L);

        TaskResponseDto response = taskMapper.toResponse(task);

        assertThat(response).isEqualTo(new TaskResponseDto(1L, "Write tests", "Cover the mapper", true));
    }
}
```

The mapper has no dependencies, so a plain JUnit test without Spring or Mockito is enough.

## 4. Controller slice test

**Tier under test: Presentation**

`src/test/java/com/example/demo/controller/TaskControllerTest.java`

Imports shown for Spring Boot 4. For Spring Boot 3.4+, swap the `WebMvcTest` import using the table above.

```java
package com.example.demo.controller;

import com.example.demo.entity.Task;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(TaskMapper.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createTaskReturns201WithLocation() throws Exception {
        when(taskService.createTask(any(Task.class)))
                .thenReturn(taskWithId(1L, "Write tests", "Cover the controller", false));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write tests", "description": "Cover the controller"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tasks/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Write tests"));
    }

    @Test
    void createTaskReturns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "description": "No title"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(taskService, never()).createTask(any());
    }

    @Test
    void getAllTasksReturns200() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(
                taskWithId(1L, "First", null, false),
                taskWithId(2L, "Second", null, true)
        ));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTaskReturns404WhenTaskDoesNotExist() throws Exception {
        when(taskService.getTask(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task with id 99 not found"));
    }

    @Test
    void updateTaskReturns200WithUpdatedTask() throws Exception {
        when(taskService.updateTask(eq(1L), any(Task.class)))
                .thenReturn(taskWithId(1L, "New title", "New description", true));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "New title", "description": "New description", "completed": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deleteTaskReturns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTaskReturns404WhenTaskDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Task", 99L)).when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    private Task taskWithId(Long id, String title, String description, boolean completed) {
        Task task = new Task(title, description, completed);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
```

`@WebMvcTest` does not load `@Component` beans such as the mapper, so `@Import(TaskMapper.class)` registers the real mapper: the test then covers the full DTO -> entity -> DTO conversion around the mocked service. `@WebMvcTest` does load `@RestControllerAdvice` beans, so the `404` and `400` responses come from the real `GlobalExceptionHandler`. That is exactly what these tests should prove.

## 5. When to use `@SpringBootTest`

Prefer the three fast test types above. Reach for `@SpringBootTest` (or `@DataJpaTest` for repository-only checks) when the feature depends on things mocks cannot reproduce:

- custom `@Query` methods or derived queries with non-trivial logic;
- `@EntityGraph` or fetch join methods that the mapper relies on;
- transactional behavior, cascades, or lazy loading;
- an end-to-end flow the user explicitly asks to verify.

These tests need a database. Reuse whatever the project already configures (H2, Testcontainers) rather than introducing a new one.
