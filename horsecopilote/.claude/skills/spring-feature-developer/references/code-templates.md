# Code Templates

Reference implementation of a complete `Task` feature following the 3-tier architecture described in `SKILL.md`. Adapt package names and layout to the existing project: these templates use a layer-based layout (`entity`, `repository`, `service`, `exception`, `dto`, `mapper`, `controller`) only as an example. Whatever the layout, each class keeps the tier shown in its heading.

## Contents

1. Entity (Repository tier)
2. Repository (Repository tier)
3. Exception (Service tier)
4. Service (Service tier)
5. DTOs (Presentation tier)
6. Mapper (Presentation tier)
7. Global exception handler (Presentation tier)
8. Controller (Presentation tier)
9. Flyway migration
10. Validation dependency

## 1. Entity

**Tier: Repository**

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
        this(title, description, false);
    }

    public Task(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
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

Mutations go through intention-revealing methods such as `update(...)` rather than a setter per field. The three-argument constructor lets the mapper build the non-persisted entity used for updates.

## 2. Repository

**Tier: Repository**

`repository/TaskRepository.java`

```java
package com.example.demo.repository;

import com.example.demo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCompleted(boolean completed);
}
```

`Task` has no relations, so nothing needs to be fetched eagerly. When an entity has a lazy relation that the mapper reads, add a repository method that loads it, for example with a hypothetical `project` relation:

```java
@EntityGraph(attributePaths = "project")
List<Task> findAllWithProjectBy();
```

The service then calls this method instead of `findAll()`, so the mapper never touches an uninitialized proxy outside the transaction.

## 3. Exception

**Tier: Service**

`exception/ResourceNotFoundException.java`

```java
package com.example.demo.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with id " + id + " not found");
    }
}
```

Services throw it, so it belongs to the Service tier and must not depend on anything from the Presentation tier. Create it only if the project has no equivalent exception already.

## 4. Service

**Tier: Service**

`service/TaskService.java`

```java
package com.example.demo.service;

import com.example.demo.entity.Task;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByStatus(boolean completed) {
        return taskRepository.findByCompleted(completed);
    }

    public Task getTask(Long id) {
        return findTaskOrThrow(id);
    }

    @Transactional
    public Task updateTask(Long id, Task changes) {
        Task task = findTaskOrThrow(id);
        task.update(changes.getTitle(), changes.getDescription(), changes.isCompleted());
        return task;
    }

    @Transactional
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

The service only imports entities, repositories and Service-tier exceptions. `updateTask` receives a non-persisted `Task` built by the mapper, copies its fields onto the managed entity and does not call `save`: inside a transaction, JPA dirty checking persists the change on commit.

## 5. DTOs

**Tier: Presentation**

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

public record TaskResponseDto(Long id, String title, String description, boolean completed) {
}
```

Records only carry data. Conversion lives in the mapper.

## 6. Mapper

**Tier: Presentation**

`mapper/TaskMapper.java`

```java
package com.example.demo.mapper;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequestDto request) {
        return new Task(request.title(), request.description());
    }

    public Task toEntity(TaskUpdateDto request) {
        return new Task(request.title(), request.description(), request.completed());
    }

    public TaskResponseDto toResponse(Task task) {
        return new TaskResponseDto(task.getId(), task.getTitle(), task.getDescription(), task.isCompleted());
    }
}
```

The mapper is the single place where DTOs and entities are converted. It never calls a service or a repository, and it only reads data the service has already loaded.

## 7. Global exception handler

**Tier: Presentation**

`controller/GlobalExceptionHandler.java`

```java
package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
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

Turning exceptions into HTTP responses is an HTTP concern, so the handler sits in the Presentation tier and imports the Service-tier exception, which respects the Presentation -> Service direction. If the project already has a `@RestControllerAdvice`, add handlers to it instead of creating a second one.

## 8. Controller

**Tier: Presentation**

`controller/TaskController.java`

```java
package com.example.demo.controller;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.entity.Task;
import com.example.demo.mapper.TaskMapper;
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
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto request) {
        Task createdTask = taskService.createTask(taskMapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.getId())
                .toUri();
        return ResponseEntity.created(location).body(taskMapper.toResponse(createdTask));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks().stream()
                .map(taskMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskMapper.toResponse(taskService.getTask(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateDto request) {
        Task updatedTask = taskService.updateTask(id, taskMapper.toEntity(request));
        return ResponseEntity.ok(taskMapper.toResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
```

Each endpoint follows the same sequence: validate, convert with the mapper, call one service method, convert the result with the mapper, return the `ResponseEntity`. Building the `Location` URI is an HTTP concern, so it belongs in the controller. The controller never calls the repository.

## 9. Flyway migration

Only when Flyway is present. `src/main/resources/db/migration/V{next}__create_tasks_table.sql`

```sql
CREATE TABLE tasks (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    completed BOOLEAN NOT NULL DEFAULT FALSE
);
```

Column types and lengths must match the entity's `@Column` definitions, and the table name must match `@Table(name = "...")`. For Liquibase, add an equivalent changeset in the format the project already uses (YAML, XML or SQL).

## 10. Validation dependency

Add to `pom.xml` if missing:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
