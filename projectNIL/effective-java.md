# Effective Java - Key Principles and Practices

A living document to track important principles from "Effective Java" as they're read and incorporated into the ProjectNIL codebase. This file should be updated regularly and referenced during code reviews and development iterations.

## Items by Category

### Object Creation and Destruction

#### Item 1: Consider static factory methods instead of constructors
- **Key Benefits**: 
  - Named methods can clarify intent better than constructors
  - Don't need to create new objects on every invocation (can cache instances)
  - Can return subtype instances instead of the class itself
- **When to Use**: Complex object creation, multiple ways to construct, caching benefits
- **Examples in ProjectNIL**: 
  - (To be populated as patterns emerge)

#### Item 2: Consider a builder when faced with many constructor parameters
- **Key Benefits**:
  - Cleaner API than telescoping constructors or JavaBeans pattern
  - Immutable objects possible
  - Better readability
- **When to Use**: Classes with 3+ optional parameters
- **Examples in ProjectNIL**: 
  - (To be populated)

### Classes and Interfaces

#### Item 13: Minimize the accessibility of classes and members
- **Key Principles**:
  - Keep implementation details hidden
  - Use package-private classes when possible
  - Default to private for class members
  - Only expose what's necessary for the public API
- **Enforcement**: Checkstyle and spotbugs help catch over-exposed members
- **Examples in ProjectNIL**: 
  - (To be populated)

#### Item 15: Minimize mutability
- **Key Principles**:
  - Immutable objects are inherently thread-safe
  - Easier to reason about and debug
  - Can be shared freely without defensive copying
- **Implementation Strategies**:
  - Make fields `private final`
  - Don't provide setter methods
  - Ensure defensive copying when necessary
  - Document immutability
- **Examples in ProjectNIL**: 
  - (To be populated)

### Methods

#### Item 50: Override equals appropriately
- **Key Requirements**:
  - Must maintain reflexive, symmetric, transitive, and consistent properties
  - Use `Objects.equals()` for comparing fields
  - Override hashCode if overriding equals
- **Anti-patterns**:
  - Incorrect null handling
  - Type mismatches
  - Missing hashCode override
- **Examples in ProjectNIL**: 
  - (To be populated)

#### Item 51: Override hashCode when you override equals
- **Key Principles**:
  - Must have same contract consistency as equals
  - Use `Objects.hash()` for simplicity unless performance critical
  - Document the hash function if it's important for callers to know
- **Examples in ProjectNIL**: 
  - (To be populated)

### General Programming

#### Item 57: Use checked exceptions for recoverable conditions; runtime exceptions for programming errors
- **Key Distinction**:
  - Checked exceptions: Expected, caller can reasonably recover
  - Runtime exceptions: Programming errors (null checks, bounds, etc.)
  - Errors: Reserved for severe problems
- **Implementation**:
  - Don't wrap exceptions unnecessarily
  - Provide actionable information in exception messages
- **Examples in ProjectNIL**: 
  - (To be populated)

#### Item 59: Avoid unnecessary use of checked exceptions
- **Anti-patterns**:
  - Checked exceptions that are never caught
  - Forcing try-catch blocks that can't do anything useful
  - Using them for control flow
- **Better Alternatives**:
  - Return Optional for "not found" conditions
  - Use runtime exceptions for truly exceptional conditions
- **Examples in ProjectNIL**: 
  - (To be populated)

## Coding Standards Applied in ProjectNIL

These practices are enforced or encouraged in this project:

- **Immutability by default**: Use `private final` fields, prefer `List.of()`, `Map.of()`, etc.
- **Static imports last**: Maintain import order for readability
- **Specific exceptions**: Catch and handle specific exceptions, not broad Exception class
- **Try-with-resources**: Use for resources that implement AutoCloseable
- **No printStackTrace()**: Log properly instead of dumping to stderr
- **100-character line limit**: Enforced by checkstyle, improves readability
- **4-space indentation**: Consistent formatting across codebase
- **var keyword**: Use when type is obvious from context (Java 10+)

## Recent Reads and Updates

- **Date**: [To be filled as items are studied]
- **Items Studied**: [List items from Effective Java]
- **Key Takeaways**: [Brief summary of what was learned]
- **Action Items**: [Any code changes or patterns to implement]

---

**Note**: This document should be reviewed during code reviews and updated as new items from Effective Java are incorporated into the project's practices.
