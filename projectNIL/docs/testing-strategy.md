# Testing Strategy: ProjectNIL

To ensure code robustness and prevent regressions, ProjectNIL employs a multi-layered testing strategy aligned with Software Engineering best practices.

## 1. The Testing Pyramid

### Unit Testing (JUnit 5 + Mockito)
- **Scope**: Individual classes, methods, and pure business logic (e.g., DTO to Entity mapping).
- **Tooling**: JUnit 5, Mockito for dependency mocking.
- **Constraints**: Must NOT require a Spring Context or Database. Must be extremely fast (< 100ms per test).
- **Location**: `src/test/java` in respective modules.
- **Baseline**: `FunctionTest` in the `common` module will serve as the reference for unit testing domain entities.

### Integration Testing (Spring Boot Test + Testcontainers)
- **Scope**: Interaction between components and external systems (Postgres, PGMQ, Liquibase).
- **Tooling**: `@SpringBootTest`, **Testcontainers**.
- **Strategy**: 
    - Real containerized Postgres/PGMQ instances are launched per test suite to verify actual SQL/Queue behavior. 
    - No "H2" or embedded databases; we test against production-identical engines.
- **Constraints**: May be slower. Grouped to minimize container startup overhead.
- **Baseline**: `PostgresIntegrationTest` in the `api` service will verify repository persistence and Liquibase migrations.

### Architecture Testing (ArchUnit)
- **Scope**: Package layering and architectural constraints.
- **Tooling**: ArchUnit.
- **Rules**:
    - `com.projectnil.common.domain` must not depend on `com.projectnil.api.web`.
    - DTOs must remain immutable (Record-based).
    - Controllers must not access Repositories directly (Service layer enforcement).
- **Baseline**: `ArchitectureTest` in the `api` service to enforce these rules.

## 4. Integration Testing Strategy: Repository Layer

To ensure our data access layer is robust, we follow these principles:

1. **Production Parity**: We use Testcontainers with Postgres to avoid "H2-only" bugs (e.g., JSONB, specialized indexes).
2. **Schema Validation**: Tests must run existing Liquibase migrations to verify the schema matches the entities.
3. **Data Isolation**: Every test must run in a transaction that is rolled back at the end, or the database must be truncated between tests.
4. **Shared Containers**: To minimize overhead, we use a single static container instance for the entire test suite where possible.

## 2. CI/CD Integration

Tests are executed automatically on every push and Pull Request via the `integrations.yml` workflow.

### Test Coverage Goals
- **Domain/Core Logic**: 90%+
- **Controllers/Services**: 70%+
- **Overall**: We favor *meaningful* tests (testing behavior) over chasing arbitrary percentage numbers.

## 3. Local Development Flow

Developers must ensure tests pass locally before pushing:
```bash
./gradlew test
```
*Note: Ensure Podman/Docker is running locally for Testcontainer support.*
