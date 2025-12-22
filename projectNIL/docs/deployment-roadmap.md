# Deployment Roadmap: Issue #20

This document outlines the strategy for moving ProjectNIL from local development to a live DigitalOcean Droplet.

## 1. Tooling Selection: Ansible vs. Kamal

We have chosen **Ansible** for this implementation.

### Rationale:
- **Compatibility:** User specifically requested `podman-compose`. Ansible represents the best tool for managing configuration files (the compose YAML) and system-level services (Podman) on a remote host.
- **Scope:** Kamal is highly opinionated towards Traefik and Docker. Ansible is more flexible for the specialized needs of our multi-module Java/Node project.
- **Reproducibility:** Ansible playbooks will serve as documentation for our server setup (FW, Podman, SSH).

## 2. Multi-Module Image Strategy

We will use **Multi-stage Dockerfiles** to handle the Gradle dependency graph.

### The Challenge:
The `:services:api` depends on `:common`. A naive build within the API Dockerfile would fail because the `common` source is outside its context.

### The Solution:
- We will build at the **Project Root** level.
- The Docker context will be the root directory.
- The Dockerfile will use a specific stage to run `./gradlew :services:api:bootJar`.

## 3. Implementation Phases

### Phase 1: Containerization
- [ ] Create `infra/docker/api.Dockerfile` (JDK 25, multi-stage).
- [ ] Create `infra/docker/compiler.Dockerfile` (Node.js).
- [ ] Implement `infra/prod.compose.yml` for Podman.

### Phase 2: Server Provisioning (Ansible)
- [ ] Define Inventory (Droplet IP).
- [ ] Playbook: Install Podman, `podman-compose`, and configure UFW.
- [ ] Playbook: Setup app directory structure on server.

### Phase 3: CI/CD Pipeline
- [ ] Update `deployment.yml` to:
    - Build images using GitHub Actions.
    - Save images to `.tar` files.
    - SCP images and the `prod.compose.yml` to the Droplet.
    - Run `podman load` and `podman-compose up`.

## 4. Risks & Considerations
- **Architecture:** The Droplet is x86_64. Ensure build steps happen on the correct architecture or use `buildx`.
- **Secrets:** Use GitHub Secrets for DB passwords and SSH keys.
- **Podman Rootless:** We will aim for rootless Podman to follow security best practices.
