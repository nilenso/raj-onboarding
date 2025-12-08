# AGENTS.md - Guidelines for agentic coding in ProjectNIL

## Build/Lint/Test Commands
- Build: `./gradlew app:build`
- Lint/Check: `./gradlew app:check` (runs checkstyle, spotbugs)
- Test all: `./gradlew app:test`
- Single test: `./gradlew app:test --tests "rnil.enso.AppTest.testMethod"`
- Run app: `./gradlew app:run`
- Clean: `./gradlew app:clean`

## Code Style Guidelines
- Java 25+ (foojay-resolver).
- Imports: Static imports last; avoid wildcard imports.
- Formatting: Checkstyle enforced (4-space indent, 100-char lines).
- Naming: camelCase methods/vars, PascalCase classes, UPPER_CASE constants.
- Types: Prefer immutable (e.g., List.of()), var where obvious.
- Error handling: Try-with-resources, specific exceptions, no printStackTrace.
- Patterns: Follow Gradle Java app conventions; minimal comments.

## Commit Message Guidelines
- Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) style.
- Format: `type(scope): subject` with optional body and footer.
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- Subject: Imperative, lowercase, no period. Max 50 characters.
- Body: Wrap at 72 characters; explain what and why, not how.
- Examples:
  - `feat(parser): add support for nested expressions`
  - `fix(build): resolve checkstyle configuration issue`
  - `docs(readme): update installation instructions`
  - `refactor(core): simplify error handling logic`
