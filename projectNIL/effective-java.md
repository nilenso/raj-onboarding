# Effective Java Reference

A living document tracking principles from "Effective Java" incorporated into ProjectNIL. Both agents and developers should refer to practices documented here when reviewing code, suggesting improvements, or implementing features.

Update this document as you read through the book to maintain a project-specific guide for effective Java practices.

## Template Example

### Item Title (e.g., "Consider static factory methods instead of constructors")
- Brief principle statement
- Key consideration or rationale
- When to apply this pattern
- Common pitfall to avoid

---

## Items

### 1. Consider static factory methods instead of constructors
- Provides descriptive names (e.g., `BigInteger.probablePrime()` vs `new BigInteger(...)`)
- Can return subclasses or existing cached instances, reducing object creation
- Not required to create a new object each time they're invoked
- Returned objects don't need to exist at the time the class is written
- Reduces verbosity compared to requiring public constructors
- Consider naming conventions: `from`, `of`, `valueOf`, `instance`, `getInstance`, `create`, `newInstance`

### 2. Consider a builder when faced with many constructor parameters
- Use when a class has many optional or required parameters (typically 3+)
- Avoids "telescoping constructor" antipattern (multiple overloaded constructors with increasing parameter counts)
- Improves readability: `new User.Builder().name("John").age(30).email("john@example.com").build()`
- Enables flexible object construction without requiring all parameters upfront
- Builder can enforce invariants and validate state before calling `build()`
- Consider making the builder an inner class or a separate class depending on usage patterns
- Builder pattern works well with immutable objects (combined with private constructor)

### 3. Enforce the singleton property with a private constructor or an enum type
- A singleton is a class that is instantiated exactly once; use sparingly as it complicates testing
- Make the constructor private to prevent instantiation from outside the class
- Public static final field approach: `public static final Elvis INSTANCE = new Elvis()` (simple, but vulnerable to reflection)
- Static factory method approach: `private static final Elvis INSTANCE = new Elvis(); public static Elvis getInstance()` (more flexible, allows lazy initialization)
- Enum-based singleton: `public enum Elvis { INSTANCE; ... }` (preferred in modern Java; handles serialization and reflection automatically)
- Enum approach provides serialization "for free" and guarantees singleton property even under reflection/deserialization
- Avoid singletons when possible; dependency injection is often a better alternative for testability

### 4. Enforce non-instantiability with a private constructor
- Use for utility classes that contain only static methods and fields (e.g., `java.util.Collections`, `java.lang.Math`)
- Make the constructor private to prevent instantiation; Java will provide a default public no-arg constructor if you don't
- Add a throw statement in the constructor to prevent instantiation even via reflection: `throw new AssertionError("Cannot instantiate utility class")`
- Document the non-instantiability in a comment explaining the class is a namespace for static utilities
- This pattern clarifies intent and prevents accidental instantiation by users of the class
- Prevents subclassing as a side effect (subclass constructor must call `super()` which will throw)
- Ideal for classes like `Math`, `Arrays`, `Collections`, `Objects` that group related static methods

### 5. Item Title
- 

### 6. Item Title
- 

### 7. Item Title
- 

### 8. Item Title
- 

### 9. Item Title
- 

### 10. Item Title
- 

### 11. Item Title
- 

### 12. Item Title
- 

### 13. Item Title
- 

### 14. Item Title
- 

### 15. Item Title
- 

### 16. Item Title
- 

### 17. Item Title
- 

### 18. Item Title
- 

### 19. Item Title
- 

### 20. Item Title
- 

### 21. Item Title
- 

### 22. Item Title
- 

### 23. Item Title
- 

### 24. Item Title
- 

### 25. Item Title
- 

### 26. Item Title
- 

### 27. Item Title
- 

### 28. Item Title
- 

### 29. Item Title
- 

### 30. Item Title
- 

### 31. Item Title
- 

### 32. Item Title
- 

### 33. Item Title
- 

### 34. Item Title
- 

### 35. Item Title
- 

### 36. Item Title
- 

### 37. Item Title
- 

### 38. Item Title
- 

### 39. Item Title
- 

### 40. Item Title
- 

### 41. Item Title
- 

### 42. Item Title
- 

### 43. Item Title
- 

### 44. Item Title
- 

### 45. Item Title
- 

### 46. Item Title
- 

### 47. Item Title
- 

### 48. Item Title
- 

### 49. Item Title
- 

### 50. Item Title
- 

### 51. Item Title
- 

### 52. Item Title
- 

### 53. Item Title
- 

### 54. Item Title
- 

### 55. Item Title
- 

### 56. Item Title
- 

### 57. Item Title
- 

### 58. Item Title
- 

### 59. Item Title
- 

### 60. Item Title
- 

### 61. Item Title
- 

### 62. Item Title
- 

### 63. Item Title
- 

### 64. Item Title
- 

### 65. Item Title
- 

### 66. Item Title
- 

### 67. Item Title
- 

### 68. Item Title
- 

### 69. Item Title
- 

### 70. Item Title
- 

### 71. Item Title
- 

### 72. Item Title
- 

### 73. Item Title
- 

### 74. Item Title
- 

### 75. Item Title
- 

### 76. Item Title
- 

### 77. Item Title
- 

### 78. Item Title
- 

### 79. Item Title
- 

### 80. Item Title
- 

### 81. Item Title
- 

### 82. Item Title
- 

### 83. Item Title
- 

### 84. Item Title
- 

### 85. Item Title
- 

### 86. Item Title
- 

### 87. Item Title
- 

### 88. Item Title
- 

### 89. Item Title
- 

### 90. Item Title
- 
