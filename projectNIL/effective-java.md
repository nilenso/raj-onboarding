# Effective Java - Key Principles and Practices

A living document to track important principles from "Effective Java" as they're read and incorporated into ProjectNIL.

## Items

### Object Creation and Destruction

- **Item 1**: Consider static factory methods instead of constructors
- **Item 2**: Consider a builder when faced with many constructor parameters

### Classes and Interfaces

- **Item 13**: Minimize the accessibility of classes and members
- **Item 15**: Minimize mutability

### Methods

- **Item 50**: Override equals appropriately
- **Item 51**: Override hashCode when you override equals

### General Programming

- **Item 57**: Use checked exceptions for recoverable conditions; runtime exceptions for programming errors
- **Item 59**: Avoid unnecessary use of checked exceptions

## Coding Standards Applied in ProjectNIL

- Immutability by default: `private final` fields, `List.of()`, `Map.of()`
- Static imports last
- Specific exception handling (no broad Exception class)
- Try-with-resources for AutoCloseable resources
- No printStackTrace() - log properly instead
- 100-character line limit (checkstyle enforced)
- 4-space indentation
- Use var keyword when type is obvious

---

**Note**: This document should be reviewed during code reviews and updated as new items from Effective Java are incorporated into the project's practices.
