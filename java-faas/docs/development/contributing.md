# Contributing Guide

This guide covers development workflows, coding standards, and conventions for contributing to ProjectNIL.

## Build Commands

### All Modules

```bash
./gradlew build          # Build all
./gradlew test           # Test all
./gradlew check          # Runs checkstyle
./gradlew clean          # Clean all
```

### Per-Module Commands

```bash
./gradlew :services:api:test        # API service tests
./gradlew :services:compiler:test   # Compiler service tests
./gradlew :common:test              # Common module tests
```

### Single Test

```bash
./gradlew :services:api:test --tests "com.projectnil.api.runtime.ChicoryWasmRuntimeTest"
```

### Running Services

```bash
# Full stack with Podman
podman compose -f infra/compose.yml up -d

# Individual services with Gradle
./gradlew :services:api:bootRun
./gradlew :services:compiler:bootRun
```

## Code Style

### General

- **Java 25+** (via foojay-resolver)
- **Checkstyle enforced**: 4-space indent, 100-char line limit
- **Imports**: Static imports last; avoid wildcard imports
- **Naming**: camelCase methods/vars, PascalCase classes, UPPER_CASE constants
- **Types**: Prefer immutable (`List.of()`), use `var` where obvious
- **Error handling**: Try-with-resources, specific exceptions, no `printStackTrace()`
- **Patterns**: Follow Gradle Java app conventions; minimal comments

### Code Review Focus

The agent/reviewer should check for:
- Code quality and consistency
- Naming and patterns
- Adherence to established conventions
- Technical improvements

The user drives:
- Domain modeling and entity definitions
- User stories and acceptance criteria
- Business logic implementations
- Database schema designs

## Branching Strategy

| Branch | Purpose | CI | CD | Merge to Main |
|--------|---------|----|----|---------------|
| `main` | Production | Yes | Yes | - |
| `dev` | Technical tasks | Yes | No | PR required, squash |
| `journal` | Documentation | No | No | Daily squash, `[skip ci]` |
| `feature-issue-<N>` | Feature work | Yes | No | PR required, squash |

### Branch Naming

- **Feature branches**: `feature-issue-<number>` (e.g., `feature-issue-19`)
- **Technical tasks**: Use `dev` branch directly
- **Documentation**: Use `journal` branch

### PR Requirements

| Source Branch | PR Required | Merge Strategy | Delete After Merge |
|---------------|-------------|----------------|-------------------|
| `dev` | Yes | Squash and merge | No (persistent) |
| `feature-issue-*` | Yes | Squash and merge | Yes |
| `journal` | No | Squash with `[skip ci]` | No (persistent) |

PRs should reference the issue number using `Closes #XX` in the description.

### Journal Workflow

1. Commit to `journal` branch with `[skip ci]` in message
2. Daily squash merge to `main` (no PR required)
3. Use descriptive commit messages: `jour: <description> [skip ci]`

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): subject

[optional body]
[optional footer]
```

### Types

| Type | Use For |
|------|---------|
| `feat` | New features |
| `fix` | Bug fixes |
| `docs` | Documentation |
| `style` | Formatting (no code change) |
| `refactor` | Code restructuring |
| `test` | Adding tests |
| `chore` | Maintenance tasks |

### Guidelines

- **Subject**: Imperative, lowercase, no period, max 50 characters
- **Body**: Wrap at 72 characters; explain what and why
- **Journal commits**: Must include `[skip ci]`

### Examples

```
feat(parser): add support for nested expressions
fix(build): resolve checkstyle configuration issue
docs(readme): update installation instructions [skip ci]
refactor(core): simplify error handling logic
```

## Project Management

### GitHub Project Board

- **Project ID**: 24
- **Owner**: nilenso
- **URL**: https://github.com/orgs/nilenso/projects/24

```bash
# List items with status
gh project item-list 24 --owner nilenso

# View project
gh project view 24 --owner nilenso
```

Issues are automatically added to the board via GitHub workflows.

### Key Principles

1. **Unit of Work**: Each issue should be a "vertical slice" delivering end-to-end value
2. **INVEST Criteria**: Independent, Negotiable, Valuable, Estimable, Small, Testable
3. **Link to Issues**: Reference in commits (`feat(auth): implement login (#41)`) and PRs (`Closes #41`)
4. **Technical Tasks**: Link enabler work using "Enables: #XX" and "Depends on: #XX"

See [Workflow](workflow.md) for detailed project management guidelines.
