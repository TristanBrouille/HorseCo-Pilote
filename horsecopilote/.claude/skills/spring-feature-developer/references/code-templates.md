# Code Templates

Reference implementation of a complete `Task` feature following the 3-tier architecture described in `SKILL.md`. The `package` lines below use a generic layer-based layout (`entity`, `repository`, `service`, `exception`, `dto`, `mapper`, `controller`) for readability only: in this project, place each class according to the package layout in `SKILL.md`, i.e. `com.epfjava.horsecopilote.task.presentation` (controller, DTOs), `...task.service` (service, MapStruct mapper), `...task.repository` (entity, repository), `...common.service` (exceptions) and `...common.presentation` (global exception handler). Each class keeps the tier shown in its heading.

## Contents

1. Entity (Repository tier)
2. Repository (Repository tier)
3. Exception (Service tier)
4. Service (Service tier)
5. DTOs (Presentation tier)
6. Mapper (Service tier)
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

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.TaskUpdateDto;
import com.example.demo.entity.Task;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public TaskResponseDto createTask(TaskRequestDto request) {
        return taskMapper.toResponse(taskRepository.save(taskMapper.toEntity(request)));
    }

    public List<TaskResponseDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public TaskResponseDto getTask(Long id) {
        return taskMapper.toResponse(findTaskOrThrow(id));
    }

    @Transactional
    public TaskResponseDto updateTask(Long id, TaskUpdateDto request) {
        Task task = findTaskOrThrow(id);
        task.update(request.title(), request.description(), request.completed());
        return taskMapper.toResponse(task);
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

The service takes request DTOs and returns response DTOs: every conversion (list, sort, mapping) is logic and stays here, so the controller only handles HTTP. Entities never leave the service. `updateTask` copies the fields onto the managed entity and does not call `save`: inside a transaction, JPA dirty checking persists the change on commit.

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

Records only carry data. Conversion lives in the mapper, called by the service.

## 6. Mapper

**Tier: Service**

`mapper/TaskMapper.java` (in this project: `task/service/TaskMapper.java`)

```java
package com.example.demo.mapper;

import com.example.demo.dto.TaskRequestDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.entity.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    default Task toEntity(TaskRequestDto request) {
        return new Task(request.title(), request.description());
    }

    TaskResponseDto toResponse(Task task);
}
```

MapStruct generates `TaskMapperImpl` at compile time as a Spring bean. `toResponse` is generated from matching names (getters to record components); use `@Mapping(target = "...", source = "...")` when names differ, and `uses = OtherMapper.class` with `injectionStrategy = InjectionStrategy.CONSTRUCTOR` to reuse another mapper for a nested object. Entities without setters are built in a `default` method through their business constructor. The mapper never calls a service or a repository, it only reads data the service has already loaded, and only services call it.

Required in `pom.xml` (MapStruct is not managed by Spring Boot, declare its version):

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

and in every `annotationProcessorPaths` of `maven-compiler-plugin`, after Lombok: `org.mapstruct:mapstruct-processor:${mapstruct.version}` and `org.projectlombok:lombok-mapstruct-binding:0.2.0`.

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
@RequestMapping("/tasks")
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

Each endpoint follows the same sequence: validate, call one service method with the request DTO, return its response DTO in a `ResponseEntity`. Building the `Location` URI is an HTTP concern, so it belongs in the controller. The controller never calls a mapper or a repository, and URLs have no `/api` prefix.

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
