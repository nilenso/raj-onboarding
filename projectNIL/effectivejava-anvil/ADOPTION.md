# Effective Java Adoption in ProjectNIL

This document provides a precise mapping of "Effective Java" principles to the ProjectNIL codebase.

## Item 2: Consider a builder when faced with many constructor parameters

We use Lombok's `@Builder` annotation to implement the Builder pattern for domain entities with numerous fields. This prevents the "telescoping constructor" anti-pattern and ensures readability during object instantiation.

*   **Implementation**:
    *   `com.projectnil.common.domain.Function`: Uses `@Builder` to manage fields including name, language, and source.
    *   `com.projectnil.common.domain.Execution`: Uses `@Builder` for fields like input, output, and error messages.
*   **Code Reference**:
    ```java
    // Function.java and Execution.java
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    ```

## Item 15: Minimize the accessibility of classes and members

We restrict constructor visibility to the minimum required for framework functionality (JPA/Hibernate) while preventing unauthorized instantiation.

*   **Implementation**: 
    *   Domain entities use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` to satisfy JPA requirements while remaining encapsulated.
    *   Builders use `@AllArgsConstructor(access = AccessLevel.PRIVATE)` to force use of the fluent API.
*   **Code Reference**:
    *   `Function.java`, Line 19: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
    *   `Execution.java`, Line 26: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`

## Item 16: In public classes, use accessor methods, not public fields

We enforce encapsulation by keeping all entity fields `private` and exposing them through generated getters and setters.

*   **Implementation**: Use of Lombok's `@Getter` and `@Setter` at the class level.
*   **Code Reference**:
    *   `Function.java`, Lines 16-17
    *   `Execution.java`, Lines 23-24

## Item 34: Use enums instead of int constants

We use strongly-typed enums for all state and status fields within the domain model.

*   **Implementation**:
    *   `com.projectnil.common.domain.FunctionStatus`: Defines lifecycle states for functions.
    *   `com.projectnil.common.domain.ExecutionStatus`: Defines lifecycle states for executions.
*   **Code Reference**:
    *   `Function.java`, Line 46: `private FunctionStatus status;`
    *   `Execution.java`, Line 45: `private ExecutionStatus status;`

## Item 17: Minimize mutability (using Records for DTOs)

We use Java `records` for Data Transfer Objects (DTOs) to enforce immutability for data crossing system boundaries. This ensures thread safety and simplifies the data flow by preventing accidental mutations.

*   **Implementation**:
    *   `com.projectnil.api.web` and `com.projectnil.api.queue` packages use `records` for all request, response, and job objects.
*   **Rationale**: DTOs are "data carriers." Making them immutable prevents side effects when passing objects between controllers, services, and message publishers.

## Async Pattern for Long-Running Tasks

While not a direct Effective Java item, we follow the best practice of asynchronous execution for non-deterministic tasks to maintain system responsiveness.

*   **Implementation**: Function execution creates a `PENDING` execution record and returns the identifier immediately, while a background worker manages the WASM runtime lifecycle.
*   **Rationale**: Prevents thread exhaustion in the web layer and provides a resilient polling mechanism for clients.

