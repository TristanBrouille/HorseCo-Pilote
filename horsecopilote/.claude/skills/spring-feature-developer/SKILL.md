---
name: spring-feature-developer
description: Implements and modifies backend features in Spring Boot Maven projects (Java 21+, typically Java 25) end to end - JPA entities, repositories, services, controllers, record DTOs, validation, exception handling, database migrations and unit tests - then validates everything with mvn test without regressions. Use this skill whenever the user asks to add, create, implement, extend or change a feature, endpoint, REST resource, CRUD, entity or module in a Spring Boot application, even if they do not name the layers explicitly (for example "add products to the app", "I need an endpoint to archive orders", "manage customers").
---

# Spring Feature Developer

Build complete, layered Spring Boot features that compile, follow the project's existing conventions, and leave the test suite green.

Code templates live in two reference files. Read them before writing code:

- `references/code-templates.md` for entity, DTO, mapper, repository, service, exception handling, controller and migration templates.
- `references/testing-templates.md` for service unit tests and controller slice tests, including the Spring Boot 3 vs 4 import differences.

## Procedure

### 0. Inspect the project and establish a baseline

Before changing anything:

1. Read `pom.xml` and note the `java.version`, the Spring Boot version (parent or BOM), and which starters are present (`validation`, `data-jpa`, `flyway`, `liquibase`, `lombok`, test starters). Java 25 requires a recent Spring Boot release; if the declared versions look incompatible, tell the user instead of silently changing them.
2. Detect the build command: use `./mvnw` if the Maven wrapper exists, otherwise `mvn`.
3. Look at the existing package layout. If the project is organized by feature (`product/ProductController`, `product/ProductService`) follow that; if it is organized by layer (`controller/`, `service/`, `dto/`) follow that. Matching the existing structure matters more than any template in this skill.
4. Note existing conventions: how DTOs are mapped, how exceptions are handled, whether a global `@RestControllerAdvice` already exists, whether Lombok is used. Reuse what exists rather than introducing a parallel mechanism.
5. Run `./mvnw test` (or `mvn test`) once and record the result. If tests already fail before your changes, report which ones to the user and ask whether to proceed. Pre-existing failures are not yours to fix silently, and without a baseline you cannot tell a regression from an old bug.

### 1. Protect existing JPA entities

Creating new entities is fine. Modifying an existing entity (adding, renaming, removing or retyping a field, changing relationships, constraints or table/column names) changes the database schema and can break production data.

When a feature requires such a change, stop and ask the user for confirmation before editing the entity. Explain exactly which fields change and what migration that implies.

### 2. Verify `.gitignore`

Make sure these entries are present and add any that are missing: `.env`, `.idea/`, `.vscode/`, `*.iml`, `target/`. This prevents secrets and IDE files from being committed.

### 3. Implement the feature

Follow these conventions in every layer:

- **Language**: all identifiers, table names, column names, DTOs, messages and tests in English.
- **Style**: `camelCase` for fields and methods, `PascalCase` for types. No comments in code; express intent through clear names and small methods.
- **Injection**: constructor injection with `final` fields. No field injection with `@Autowired`.
- **Lombok**: only use it if the project already does.

Layer by layer:

- **Entity**: JPA entity with a protected or public no-arg constructor, a business constructor, getters, and setters only for mutable fields. New entities can be created directly.
- **DTOs**: Java `record` types, one public record per file. Use separate request and response records. Put Bean Validation annotations (`@NotBlank`, `@Size`, `@NotNull`, `@Positive`...) on request record components. Never expose entities in controllers.
- **Mapping**: give each response record a static `from(Entity entity)` factory so the conversion lives in exactly one place. If the project already uses MapStruct or a mapper class, use that instead.
- **Repository**: interface extending `JpaRepository<Entity, Long>`. Add derived query methods only when needed.
- **Service**: all business logic lives here. Annotate the class with `@Transactional(readOnly = true)` and override write methods with `@Transactional`. Use Java Streams (`stream()`, `map()`, `filter()`, `toList()`) for collection processing. When an entity is not found, throw a dedicated `ResourceNotFoundException` rather than returning `null` or an `Optional` to the controller.
- **Exception handling**: if no global handler exists, create one `@RestControllerAdvice` that maps `ResourceNotFoundException` to `404` and `MethodArgumentNotValidException` to `400`, returning `ProblemDetail`. If a handler exists, extend it. This is what keeps status-code logic out of controllers.
- **Controller**: HTTP routing only. Delegate to one service method per endpoint and wrap the result in a `ResponseEntity`. Use `@Valid` on every `@RequestBody`. Status codes: `200` for reads and updates, `201` with a `Location` header for creation, `204` for deletion, `400` and `404` produced by the advice. No `if`, loops, mapping or repository access in controllers.
- **Dependencies**: if validation is needed and `spring-boot-starter-validation` is missing from `pom.xml`, add it and mention it in the summary.

### 4. Database migrations

If Flyway (`src/main/resources/db/migration`) or Liquibase is present, every new entity or confirmed entity change needs a migration script. Use the next free version number and a descriptive name, for example `V7__create_products_table.sql`. Never edit a migration that already exists, since it may already have run in other environments.

If neither tool is present, the project probably relies on `spring.jpa.hibernate.ddl-auto`; do not add a migration tool unless the user asks.

### 5. Write tests

Cover the new code with:

- **Service unit tests** using JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`): nominal cases, not-found cases, and any business rule.
- **Controller slice tests** using `@WebMvcTest` with the service mocked: success status codes, validation errors (`400`) and not-found (`404`).

Use `@SpringBootTest` only when the feature genuinely needs the full context (complex queries, transactional behavior). Use the import paths matching the project's Spring Boot version (see `references/testing-templates.md`).

### 6. Run tests and fix regressions

Run the full suite with `./mvnw test` (or `mvn test`).

When something fails, read the failure, fix the cause in the production code or in the new tests, and run again. Respect these limits:

- Never delete, disable (`@Disabled`), comment out or weaken an existing test to make the build pass. An existing test failing after your change is a regression signal, not an obstacle.
- If the same failure persists after 3 fix attempts, stop and explain the problem, what you tried, and your hypothesis to the user.
- If Maven itself cannot run (missing JDK, network issues downloading dependencies), report it instead of guessing.

The feature is not done until the suite is fully green, apart from failures that were already present in the baseline and that the user agreed to leave.

### 7. Summary

Finish with a short summary:

- What the feature does and its endpoints (verb, path, status codes).
- Files created, modified and deleted, as three lists.
- Any dependency added, migration created, or entity change the user approved.
- The final test result (number of tests run, all passing).
