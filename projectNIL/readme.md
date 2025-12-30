# ProjectNIL

A Function-as-a-Service (FaaS) platform where users submit source code, the platform compiles it to WebAssembly, and users execute functions on demand with JSON input/output.

**Documentation**: https://nilenso.github.io/raj-onboarding/

## Quick Navigation

| Looking for... | GitHub | GitHub Pages |
|----------------|--------|--------------|
| Getting started / Quick start | [docs/guides/getting-started.md](./docs/guides/getting-started.md) | [Getting Started](https://nilenso.github.io/raj-onboarding/guides/getting-started/) |
| API reference | [docs/api.md](./docs/api.md) | [API Reference](https://nilenso.github.io/raj-onboarding/api/) |
| Writing functions | [docs/guides/writing-functions.md](./docs/guides/writing-functions.md) | [Writing Functions](https://nilenso.github.io/raj-onboarding/guides/writing-functions/) |
| Architecture overview | [docs/architecture/overview.md](./docs/architecture/overview.md) | [Architecture](https://nilenso.github.io/raj-onboarding/architecture/overview/) |
| Infrastructure / Deployment | [docs/infrastructure.md](./docs/infrastructure.md) | [Infrastructure](https://nilenso.github.io/raj-onboarding/infrastructure/) |
| Contributing guide | [docs/development/contributing.md](./docs/development/contributing.md) | [Contributing](https://nilenso.github.io/raj-onboarding/development/contributing/) |
| Coding standards | [docs/development/coding-standards.md](./docs/development/coding-standards.md) | [Coding Standards](https://nilenso.github.io/raj-onboarding/development/coding-standards/) |
| Project workflow | [docs/development/workflow.md](./docs/development/workflow.md) | [Workflow](https://nilenso.github.io/raj-onboarding/development/workflow/) |
| Roadmap | [docs/roadmap.md](./docs/roadmap.md) | [Roadmap](https://nilenso.github.io/raj-onboarding/roadmap/) |
| Design decisions (ADRs) | [docs/decisions/](./docs/decisions/) | [Decisions](https://nilenso.github.io/raj-onboarding/decisions/001-wasm-runtime/) |

## External Resources

- [GitHub Project Board](https://github.com/orgs/nilenso/projects/24/views/1) - Kanban tracking
- [Journal Branch](https://github.com/nilenso/raj-onboarding/tree/journal) - Daily documentation entries

## Quick Start

```bash
# Start the stack
podman compose -f infra/compose.yml up -d

# Build and test
./gradlew build
./gradlew test
```

See [Getting Started](./docs/guides/getting-started.md) for detailed setup instructions.
