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

## Branching Strategy

This project follows a three-branch model:

| Branch | Purpose | CI | CD |
|--------|---------|----|----|
| `main` | Production branch | ✅ | ✅ |
| `dev` | Development/integration branch | ✅ | ❌ |
| `journal` | Onboarding logs & documentation | ❌ | ❌ |

**Important**: All commits to the `journal` branch **must include `[skip ci]` in the commit message** to prevent unnecessary CI triggers.

## Commit Message Guidelines
- Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) style.
- Format: `type(scope): subject` with optional body and footer.
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- Subject: Imperative, lowercase, no period. Max 50 characters.
- Body: Wrap at 72 characters; explain what and why, not how.
- **Journal Branch**: All commits must include `[skip ci]` in the subject or message.
- Examples:
  - `feat(parser): add support for nested expressions`
  - `fix(build): resolve checkstyle configuration issue`
  - `docs(readme): update installation instructions [skip ci]` (if on journal branch)
  - `refactor(core): simplify error handling logic`

## Project Management

See [project-management.md](./project-management.md) for detailed guidelines on:
- Issue types and labels (`type: feature`, `type: technical`, `type: spike`, `type: bug`, `type: chore`)
- Kanban workflow (Backlog → Ready → In Progress → In Review → Done)
- Writing good issues with acceptance criteria
- Linking technical tasks to the features they enable

### Key Principles

1. **Unit of Work**: Each issue should ideally be a "vertical slice" delivering end-to-end value, following the INVEST criteria (Independent, Negotiable, Valuable, Estimable, Small, Testable).

2. **Link to Issues**: Reference issues in commits (`feat(auth): implement login (#41)`) and PRs (`Closes #41`).

3. **Technical Tasks**: Always link enabler/technical work to the feature it supports using "Enables: #XX" and "Depends on: #XX".

Reference: [The Common Sense Unit of Work](https://blog.nilenso.com/blog/2025/09/17/the-common-sense-unit-of-work/)
