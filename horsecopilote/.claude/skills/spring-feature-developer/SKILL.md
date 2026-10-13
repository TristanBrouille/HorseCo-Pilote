---
name: spring-feature-developer
description: Implements and modifies backend features in Spring Boot Maven projects (Java 21+, typically Java 25) end to end - JPA entities, repositories, services, controllers, record DTOs, mappers, validation, exception handling, database migrations and unit tests - following a strict 3-tier architecture, then validates everything with mvn test without regressions. Use this skill whenever the user asks to add, create, implement, extend or change a feature, endpoint, REST resource, CRUD, entity or module in a Spring Boot application, even if they do not name the layers explicitly (for example "add products to the app", "I need an endpoint to archive orders", "manage customers").
---

# Spring Feature Developer

Build complete, layered Spring Boot features that compile, follow the project's existing conventions, respect a strict 3-tier architecture, and leave the test suite green.

Code templates live in two reference files. Read them before writing code:

- `references/code-templates.md` for entity, DTO, mapper, repository, service, exception handling, controller and migration templates, each labeled with its tier.
- `references/testing-templates.md` for service, mapper and controller tests, including the Spring Boot 3 vs 4 import differences.

## Architecture

Every feature follows a strict 3-tier architecture. Each class belongs to exactly one tier.

### Presentation tier

Controllers, DTOs and mappers.

- **Controllers**: annotated `@RestController`, with `@RequestMapping` on the class and `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` or `@DeleteMapping` on the methods. They handle HTTP only: routing, input validation with `@Valid`, conversion through the mapper, and status codes.
- **DTOs**: Java `record` types, one per file, with `jakarta.validation` annotations on request DTOs.
- **Mappers**: one `@Component` class per resource (for example `TaskMapper`) with `toEntity(requestDto)` and `toResponse(entity)` methods. The mapper is the only place where DTOs and entities are converted into each other. Response records contain no conversion logic.
- **Exception handler**: the global `@RestControllerAdvice` belongs here, since turning exceptions into HTTP responses is an HTTP concern.

### Service tier

Classes annotated `@Service`. All business logic and transactions (`@Transactional`) live here. A service receives and returns only entities or simple types (`Long`, `String`, `boolean`...), never DTOs. Business exceptions thrown by services, such as `ResourceNotFoundException`, belong to this tier.

### Repository tier (data access)

- **Repositories**: interfaces explicitly annotated `@Repository` that extend `JpaRepository`.
- **Entities**: JPA classes annotated `@Entity` and `@Table(name = "...")` with an explicit table name.

### Package layout: one package per feature, one sub-package per tier

The tiers are physical packages. Every feature has its own package under the root package, containing exactly three sub-packages named after the tiers:

```
com.epfjava.horsecopilote
├── HorsecopiloteApplication.java
├── common/                      # cross-cutting code, same tier split
│   ├── presentation/            # GlobalExceptionHandler (@RestControllerAdvice)
│   ├── service/                 # ResourceNotFoundException, BusinessRuleException
│   └── config/                  # SecurityConfig and other @Configuration
├── user/
│   ├── presentation/            # CoachController, CoachResponseDto, CoachMapper
│   ├── service/                 # UserService
│   └── repository/              # User, Role, UserRepository
└── weeklyslot/
    ├── presentation/            # WeeklySlotController, WeeklySlotRequestDto, WeeklySlotResponseDto, WeeklySlotMapper
    ├── service/                 # WeeklySlotService
    └── repository/              # WeeklySlot, WeeklySlotRepository
```

- **Feature package**: singular, lowercase, no separator (`horse`, `weeklyslot`, `lesson`, `lessonbooking`).
- **`presentation/`**: controllers, request/response DTO records and the `@Component` mapper, side by side (no further sub-packages).
- **`service/`**: `@Service` classes with the business logic. Service-tier exceptions shared by every feature live in `common/service/`.
- **`repository/`**: `@Entity` classes, the enums they store, and the `@Repository` interfaces.
- **Cross-feature access** follows the tier rules: a service may call another feature's service (`WeeklySlotService` → `UserService`), a mapper may reuse another feature's mapper (`WeeklySlotMapper` → `CoachMapper`), an entity may reference another feature's entity (`WeeklySlot` → `User`). A service never calls another feature's repository directly; it goes through that feature's service.
- **Tests** mirror the same packages under `src/test/java` (`weeklyslot/service/WeeklySlotServiceTest`, `weeklyslot/presentation/WeeklySlotControllerTest`...).
- `common/config/` is not a tier: it only holds Spring configuration (security, CORS...).

### Dependency rules

- Calls flow in one direction only: Presentation -> Service -> Repository.
- A controller never calls a repository directly.
- A service never imports a class from the Presentation tier (DTO, mapper, controller).
- A controller contains no business logic: it validates the input, converts it with the mapper, calls one service method, converts the result with the mapper and returns the `ResponseEntity`.
- A service loads all the data the mapper will need, using a fetch join or `@EntityGraph` for lazy relations, because mapping happens outside the transaction.

### Updating a resource

The controller converts the request DTO into a non-persisted entity with the mapper, then calls a service method such as `updateTask(Long id, Task changes)`. The service loads the existing entity and copies the fields from `changes` onto it.

## Procedure

### 0. Inspect the project and establish a baseline

Before changing anything:

1. Read `pom.xml` and note the `java.version`, the Spring Boot version (parent or BOM), and which starters are present (`validation`, `data-jpa`, `flyway`, `liquibase`, `lombok`, test starters). Java 25 requires a recent Spring Boot release; if the declared versions look incompatible, tell the user instead of silently changing them.
2. Detect the build command: use `./mvnw` if the Maven wrapper exists, otherwise `mvn`.
3. Use the package layout described above (feature package with `presentation/`, `service/` and `repository/` sub-packages). Put a new class in the feature it belongs to, creating the feature package if needed; shared exceptions and configuration go to `common/`. Never recreate layer-wide packages such as `controller/`, `dto/` or `entity/` at the root. Whatever the package, the tier rules in the Architecture section still apply.
4. Note existing conventions: how DTOs are mapped, how exceptions are handled, whether a global `@RestControllerAdvice` already exists, whether Lombok is used. Reuse what exists rather than introducing a parallel mechanism, as long as it does not break the Architecture rules.
5. Run `./mvnw test` (or `mvn test`) once and record the result. If tests already fail before your changes, report which ones to the user and ask whether to proceed. Pre-existing failures are not yours to fix silently, and without a baseline you cannot tell a regression from an old bug.

### 1. Clarify the request

Before writing any code, analyze the request and list what is ambiguous or missing, for example:

- the entity fields, their types, and which are required or optional;
- validation rules (lengths, formats, unique values);
- relationships with existing entities (`@OneToMany`, `@ManyToOne`...) and cascade behavior;
- the expected endpoints (full CRUD or only some operations);
- the fields exposed in response DTOs (all of them or a subset);
- business rules (statuses, transitions, calculations, constraints);
- deletion behavior (physical or logical);
- pagination, sorting or filtering needs.

Rules for this step:

- Ask every question in a single message, grouped and numbered, and suggest a reasonable default for each one so the user can simply confirm it.
- Never guess a point that changes the database schema or a business rule: wait for the answer.
- If the request is already clear and complete, do not ask unnecessary questions: show a short summary of the assumptions you are making and continue.

### 2. Create a feature branch

Never implement a feature directly on the default branch. Once the request is clarified, before editing any file:

1. Run `git status`. If there are uncommitted changes unrelated to the feature, tell the user and ask whether to commit, stash or keep them before switching branches. Never discard them.
2. Update the base branch: check out the default branch (`main` or `master`) and run `git pull` if a remote exists. If the pull fails, report it instead of forcing anything.
3. Create and switch to a new branch: `git checkout -b feature/<short-kebab-case-name>` (for example `feature/horse-crud`, or `fix/<name>` for a bug fix). Follow the project's naming convention if one exists in existing branches or in `CLAUDE.md`. If the branch already exists, ask the user whether to reuse it or pick another name.
4. If the user is already on a non-default branch dedicated to this work, stay on it.

Do not commit, push or open a pull request unless the user asks. Never add Claude as a co-author of commits: no `Co-Authored-By: Claude …` trailer (nor any other Claude/Anthropic attribution) in commit messages or pull request descriptions.

### 3. Protect existing JPA entities

Creating new entities is fine. Modifying an existing entity (adding, renaming, removing or retyping a field, changing relationships, constraints or table/column names) changes the database schema and can break production data.

When a feature requires such a change, stop and ask the user for confirmation before editing the entity. Explain exactly which fields change and what migration that implies.

### 4. Verify `.gitignore`

Make sure these entries are present and add any that are missing: `.env`, `.idea/`, `.vscode/`, `*.iml`, `target/`. This prevents secrets and IDE files from being committed.

### 5. Implement the feature

Follow these conventions in every tier:

- **Language**: all identifiers, table names, column names, DTOs, messages and tests in English.
- **Style**: `camelCase` for fields and methods, `PascalCase` for types. No comments in code; express intent through clear names and small methods.
- **Injection**: constructor injection with `final` fields. No field injection with `@Autowired`.
- **Lombok**: only use it if the project already does.

Class by class:

- **Entity** (Repository tier): JPA entity annotated `@Entity` and `@Table(name = "...")`, with a protected or public no-arg constructor, a business constructor, getters, and setters only for mutable fields. New entities can be created directly.
- **Repository** (Repository tier): interface annotated `@Repository` extending `JpaRepository<Entity, Long>`. Add derived query methods only when needed, and add fetch joins or `@EntityGraph` methods when the mapper needs lazy relations.
- **Service** (Service tier): all business logic lives here. Annotate the class with `@Transactional(readOnly = true)` and override write methods with `@Transactional`. Methods take and return entities or simple types, never DTOs. Use Java Streams (`stream()`, `map()`, `filter()`, `toList()`) for collection processing. When an entity is not found, throw a dedicated `ResourceNotFoundException` rather than returning `null` or an `Optional` to the controller.
- **DTOs** (Presentation tier): Java `record` types, one public record per file. Use separate request and response records. Put Bean Validation annotations (`@NotBlank`, `@Size`, `@NotNull`, `@Positive`...) on request record components. Never expose entities in controllers.
- **Mapper** (Presentation tier): one `@Component` class per resource with `toEntity(requestDto)` and `toResponse(entity)` methods, overloaded per request DTO when needed. It is the only place where DTOs and entities are converted. If the project already uses MapStruct, a MapStruct mapper with `componentModel = "spring"` exposing the same methods is acceptable.
- **Exception handling** (Presentation tier): if no global handler exists, create one `@RestControllerAdvice` that maps `ResourceNotFoundException` to `404` and `MethodArgumentNotValidException` to `400`, returning `ProblemDetail`. If a handler exists, extend it. This is what keeps status-code logic out of controllers.
- **Controller** (Presentation tier): HTTP only. Validate with `@Valid` on every `@RequestBody`, convert with the mapper, delegate to one service method per endpoint, convert the result with the mapper and wrap it in a `ResponseEntity`. Status codes: `200` for reads and updates, `201` with a `Location` header for creation, `204` for deletion, `400` and `404` produced by the advice. No `if`, loops, business logic or repository access in controllers.
- **Dependencies**: if validation is needed and `spring-boot-starter-validation` is missing from `pom.xml`, add it and mention it in the summary.

### 6. Database migrations

If Flyway (`src/main/resources/db/migration`) or Liquibase is present, every new entity or confirmed entity change needs a migration script. Use the next free version number and a descriptive name, for example `V7__create_products_table.sql`. Never edit a migration that already exists, since it may already have run in other environments.

If neither tool is present, the project probably relies on `spring.jpa.hibernate.ddl-auto`; do not add a migration tool unless the user asks.

### 7. Verify the architecture

Re-read every class created or modified and confirm that:

- each class belongs to exactly one tier;
- no controller calls a repository;
- no service imports a DTO, mapper or controller, and no service method takes or returns a DTO;
- no controller contains business logic;
- every DTO-entity conversion goes through the mapper;
- every relation the mapper reads is loaded by the service.

Fix any violation before moving on to the tests.

### 8. Write tests

Cover the new code with:

- **Service unit tests** using JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`), working with entities: nominal cases, not-found cases, and any business rule.
- **Mapper unit tests**: plain JUnit tests checking `toEntity` and `toResponse`.
- **Controller slice tests** using `@WebMvcTest` with the service mocked and the mapper imported with `@Import(TaskMapper.class)`: success status codes, validation errors (`400`) and not-found (`404`).

Use `@SpringBootTest` only when the feature genuinely needs the full context (complex queries, transactional behavior). Use the import paths matching the project's Spring Boot version (see `references/testing-templates.md`).

### 9. Run tests and fix regressions

Run the full suite with `./mvnw test` (or `mvn test`).

When something fails, read the failure, fix the cause in the production code or in the new tests, and run again. Respect these limits:

- Never delete, disable (`@Disabled`), comment out or weaken an existing test to make the build pass. An existing test failing after your change is a regression signal, not an obstacle.
- If the same failure persists after 3 fix attempts, stop and explain the problem, what you tried, and your hypothesis to the user.
- If Maven itself cannot run (missing JDK, network issues downloading dependencies), report it instead of guessing.

The feature is not done until the suite is fully green, apart from failures that were already present in the baseline and that the user agreed to leave.

### 10. Summary

Finish with a short summary:

- What the feature does and its endpoints (verb, path, status codes).
- The git branch the work was done on.
- Files created, modified and deleted, as three lists, each file tagged with its tier.
- Any dependency added, migration created, or entity change the user approved.
- Confirmation that the architecture check passed.
- The final test result (number of tests run, all passing).
