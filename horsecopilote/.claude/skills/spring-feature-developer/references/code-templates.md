# Code Templates

Reference implementation of a complete `Task` feature. Adapt package names and layout to the existing project: these templates use a layer-based layout (`entity`, `dto`, `repository`, `service`, `controller`, `exception`) only as an example.

## Contents

1. Entity
2. DTOs (records, one per file)
3. Repository
4. Exception
5. Global exception handler
6. Service
7. Controller
8. Flyway migration
9. Validation dependency

## 1. Entity

`entity/Task.java`

```java
package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean completed;

    protected Task() {
    }

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
    }

    public void update(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }
}
```

Mutations go through intention-revealing methods such as `update(...)` rather than a setter per field.

## 2. DTOs

Each public record in its own file. Java does not allow two public top-level types in one file.

`dto/TaskRequestDto.java`

```java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequestDto(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description
) {
}
```

`dto/TaskUpdateDto.java`

```java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskUpdateDto(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description,
        @NotNull Boolean completed
) {
}
```

`dto/TaskResponseDto.java`

```java
package com.example.demo.dto;

import com.example.demo.entity.Task;

public record TaskResponseDto(Long id, String title, String description, boolean completed) {

    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(task.getId(), task.getTitle(), task.getDescription(), task.isCompleted());
    }
}
```

The static `from` factory is the single place where the entity becomes a DTO.

## 3. Repository

`repository/TaskRepository.java`

```java
package com.example.demo.repository;

import com.example.demo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCompleted(boolean completed);
}
```

## 4. Exception

`exception/ResourceNotFoundException.java`

```java
package com.example.demo.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with id " + id + " not found");
    }
}
```

Create it only if the project has no equivalent exception already.

## 5. Global exception handler

`exception/GlobalExceptionHandler.java`

```java
package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> String.valueOf(fieldError.getDefaultMessage()),
                        (first, second) -> first
                ));
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }
}
```

If the project already has a `@RestControllerAdvice`, add handlers to it instead of creating a second one.

## 6. Service

`service/TaskService.java`

```java
package com.example.demo.service;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.entity.Task;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    public TaskResponseDto createTask(TaskRequestDto request) {
        Task savedTask = taskRepository.save(new Task(request.title(), request.description()));
        return TaskResponseDto.from(savedTask);
    }

    public List<TaskResponseDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    public List<TaskResponseDto> getTasksByStatus(boolean completed) {
        return taskRepository.findByCompleted(completed).stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    public TaskResponseDto getTask(Long id) {
        return TaskResponseDto.from(findTaskOrThrow(id));
    }
    
    public TaskResponseDto updateTask(Long id, TaskUpdateDto request) {
        Task task = findTaskOrThrow(id);
        task.update(request.title(), request.description(), request.completed());
        return TaskResponseDto.from(task);
    }
    
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
```

`updateTask` does not call `save`: inside a transaction, JPA dirty checking persists the change on commit.

## 7. Controller

`controller/TaskController.java`

```java
package com.example.demo.controller;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto request) {
        TaskResponseDto createdTask = taskService.createTask(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.id())
                .toUri();
        return ResponseEntity.created(location).body(createdTask);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateDto request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
```

Building the `Location` URI is an HTTP concern, so it belongs in the controller. Everything else is delegated.

## 8. Flyway migration

Only when Flyway is present. `src/main/resources/db/migration/V{next}__create_tasks_table.sql`

```sql
CREATE TABLE tasks (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    completed BOOLEAN NOT NULL DEFAULT FALSE
);
```

Column types and lengths must match the entity's `@Column` definitions. For Liquibase, add an equivalent changeset in the format the project already uses (YAML, XML or SQL).

## 9. Validation dependency

Add to `pom.xml` if missing:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
