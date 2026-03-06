# Java Coding Standards

This document tracks "Effective Java" principles and their application in ProjectNIL. Both developers and code reviewers should refer to these practices.

## Applied Patterns in ProjectNIL

### Item 2: Builder Pattern

We use Lombok's `@Builder` for domain entities with many fields, preventing the "telescoping constructor" anti-pattern.

**Implementation**:
- `com.projectnil.common.domain.Function`: Uses `@Builder` for name, language, source, etc.
- `com.projectnil.common.domain.Execution`: Uses `@Builder` for input, output, error messages

```java
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
```

### Item 15: Minimize Accessibility

We restrict constructor visibility to the minimum required for JPA while preventing unauthorized instantiation.

**Implementation**:
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA requirements
- `@AllArgsConstructor(access = AccessLevel.PRIVATE)` to force use of builders

### Item 16: Use Accessor Methods

All entity fields are `private` with generated getters/setters via Lombok's `@Getter` and `@Setter`.

### Item 17: Minimize Mutability (Records for DTOs)

We use Java `records` for DTOs to enforce immutability across system boundaries.

**Implementation**:
- `com.projectnil.api.web` package: Request/response records
- `com.projectnil.api.queue` package: Job and result records

DTOs are data carriers - immutability prevents side effects between controllers, services, and publishers.

### Item 34: Use Enums Instead of Constants

We use strongly-typed enums for all status fields:
- `com.projectnil.common.domain.FunctionStatus`: PENDING, COMPILING, READY, FAILED
- `com.projectnil.common.domain.ExecutionStatus`: PENDING, RUNNING, COMPLETED, FAILED

---

## Quick Reference

### Object Creation (Items 1-9)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 1 | Static factory methods | Descriptive names, can return subtypes |
| 2 | Builder pattern | Use for 3+ parameters |
| 3 | Singleton with enum | Preferred over static fields |
| 4 | Private constructor for utilities | Prevent instantiation |
| 5 | Dependency injection | Don't hardwire dependencies |
| 6 | Avoid unnecessary objects | Reuse immutables, use primitives |
| 7 | Eliminate obsolete references | Clear references for GC |
| 8 | Avoid finalizers/cleaners | Use try-with-resources |
| 9 | try-with-resources | Preferred over try-finally |

### equals/hashCode/toString (Items 10-12)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 10 | equals() contract | Reflexive, symmetric, transitive, consistent |
| 11 | hashCode() with equals() | Equal objects must have equal hash codes |
| 12 | Always override toString() | Include significant fields |

### Classes and Interfaces (Items 15-25)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 15 | Minimize accessibility | Private by default |
| 16 | Accessor methods | No public fields |
| 17 | Minimize mutability | Immutable by default |
| 18 | Composition over inheritance | Prefer has-a to is-a |
| 19 | Design for inheritance or prohibit | Document or make final |
| 20 | Interfaces over abstract classes | Support multiple inheritance |
| 22 | Interfaces define types | Not for constants |
| 24 | Static member classes | Prefer over nonstatic |

### Generics (Items 26-33)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 26 | No raw types | Always parameterize |
| 27 | Eliminate unchecked warnings | Suppress narrowly with docs |
| 28 | Lists over arrays | Type-safe, invariant |
| 31 | Bounded wildcards | PECS: Producer Extends, Consumer Super |

### Enums and Annotations (Items 34-41)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 34 | Enums over int constants | Type-safe, namespaced |
| 35 | Instance fields over ordinals | Don't depend on declaration order |
| 36 | EnumSet over bit fields | Type-safe flags |
| 37 | EnumMap over ordinal indexing | Type-safe mapping |
| 40 | Always use @Override | Compiler catches errors |

### Lambdas and Streams (Items 42-48)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 42 | Lambdas over anonymous classes | Concise for functional interfaces |
| 43 | Method references over lambdas | When just forwarding |
| 44 | Standard functional interfaces | Use java.util.function |
| 45 | Streams judiciously | Balance readability |
| 46 | Side-effect-free functions | Pure functions in streams |
| 47 | Collection over Stream return | Unless too large |

### Methods (Items 49-56)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 49 | Check parameters | Validate at entry |
| 50 | Defensive copies | Copy mutable params |
| 54 | Return empty, not null | Empty collections/arrays |
| 55 | Optional judiciously | Not for collections |
| 56 | Document all APIs | @param, @return, @throws |

### Exceptions (Items 69-77)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 69 | Exceptions for exceptional | Not control flow |
| 70 | Checked for recoverable | Runtime for programming errors |
| 72 | Standard exceptions | IllegalArgumentException, etc. |
| 73 | Appropriate abstraction | Translate low-level exceptions |
| 75 | Failure-capture info | Include values in messages |
| 76 | Failure atomicity | Leave object unchanged on failure |
| 77 | Don't ignore exceptions | At minimum, log |

### Concurrency (Items 78-84)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 78 | Synchronize shared data | Both reads and writes |
| 79 | Avoid excessive sync | Don't hold locks during alien calls |
| 80 | Executors over Threads | Use thread pools |
| 81 | Concurrency utilities | CountDownLatch, BlockingQueue, etc. |
| 82 | Document thread safety | Callers need to know |

### Serialization (Items 85-90)

| Item | Principle | Key Point |
|------|-----------|-----------|
| 85 | Avoid Java serialization | Prefer JSON/Protobuf |
| 86 | Implement Serializable cautiously | Hard to change later |

---

## Code Review Checklist

When reviewing code, verify:

- [ ] Parameters validated at method entry
- [ ] Mutable inputs defensively copied
- [ ] Empty collections returned, not null
- [ ] Exception messages include context
- [ ] `@Override` used consistently
- [ ] No raw generic types
- [ ] Streams used appropriately (not overly complex)
- [ ] Thread safety documented for shared state
- [ ] Resources closed with try-with-resources
