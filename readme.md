# Raj's Onboarding Logs 

[![CI - dev](https://github.com/nilenso/raj-onboarding/actions/workflows/ci-dev.yml/badge.svg?branch=dev)](https://github.com/nilenso/raj-onboarding/actions/workflows/ci-dev.yml)
[![CI/CD - main](https://github.com/nilenso/raj-onboarding/actions/workflows/ci-cd-main.yml/badge.svg?branch=main)](https://github.com/nilenso/raj-onboarding/actions/workflows/ci-cd-main.yml)

### Agentic Tooling Disclaimer

 - LLMs were explicitly employed (write access) for some portions of the project: 
   - Documentation
     - diagramming DSLs (mermaid/plantuml)
     - minor documentation commentary over code when necessary 
     - commit messages (conventional commits style) : tag lines and descriptions
   - CI/CD setup
   
 - Apart from the above, they were granted read-only access to the repository to help me discuss the domain-oriented aspects (SWE Wisdom, writing effective Java, Design, Architecture) I wish to improve my grasp of: the source code (other than deterministically auto-generated (`gradle init` for instance) files) is hand-crafted and have marked any exceptions (if any) in [LLM generated]  fences. 

## Overview 

### [Journal](journal)
 - commentary upon dailies

### [projectNIL](projectNIL/readme.md) 
 - onboarding project towards picking up the tech stack

### [Effective Java](projectNIL/effective-java.md)
 - documenting an index into the items from the book for quick reference during implementation of project NIL

## Workflow & Branching Strategy

### Three-Branch Model

| Branch    | Purpose                        | CI     | CD     | Guidelines                                                                                          |
|-----------|--------------------------------|--------|--------|-----------------------------------------------------------------------------------------------------|
| `main`    | Production branch              | ✅ Yes | ✅ Yes | Merged from `dev` only. Full CI/CD pipeline runs.                                                   |
| `dev`     | Development/integration branch | ✅ Yes | ❌ No  | Main development branch. CI runs on all commits.                                                    |
| `journal` | Personal documentation & logs  | ❌ No  | ❌ No  | Branch for onboarding logs and commentary. **All commits MUST include `[skip ci]` in the message.** |

### Important Guidelines

#### Journal Branch Commits
- **All commits to the `journal` branch must include `[skip ci]` in their commit message** to prevent unnecessary CI triggers
- Example: `git commit -m "Update journal entry [skip ci]"`

#### Merging from Journal to Main
- When merging from `journal` to `main`, ensure the merge commit includes `[skip ci]` to bypass CI checks
- Example: `git merge journal --no-ff -m "Merge journal branch [skip ci]"`

#### Which Branch to Use
- **For feature development**: Create feature branches off `dev`, then merge into `dev` via PR
- **For onboarding logs**: Commit directly to `journal` with `[skip ci]` tag
- **For production**: Merge `dev` into `main` when ready for deployment 
