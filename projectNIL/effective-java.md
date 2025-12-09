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

### 5. Prefer dependency injection to hardwiring resources
- Don't hardwire dependencies (e.g., `private static final SpellChecker checker = new SpellChecker()`)
- Pass required resources/dependencies as constructor or method parameters instead
- Enables flexibility: swap implementations, mock for testing, support multiple configurations
- Constructor injection is preferred over setter injection (makes immutable objects, clarifies required dependencies)
- Works well with interfaces/abstractions; depend on abstractions, not concrete implementations
- Static factory methods and builders can facilitate dependency injection patterns
- Consider a dependency injection framework (Spring, Guice) for large applications with many dependencies
- Factory pattern can be passed as dependency for deferred or conditional resource creation

### 6. Avoid creating unnecessary objects
- Reuse immutable objects when possible (e.g., `"hello"` string literals are interned, not recreated)
- Use static initializers for expensive-to-create objects that are used repeatedly: `private static final Pattern PATTERN = Pattern.compile("regex")`
- Prefer primitives to autoboxed types when performance matters; boxing creates new objects
- Use object pools cautiously (modern JVMs optimize well; pools often add complexity without benefit)
- Be aware of hidden object creation in common operations (e.g., `String.substring()` used to create copies; use carefully)
- Lazy initialization can defer expensive object creation, but adds complexity; use only when initialization is truly expensive
- Profile before optimizing; don't prematurely optimize based on assumptions about object creation

### 7. Eliminate obsolete object references
- Clear references to objects no longer needed to allow garbage collection (set to `null` or let scope end)
- Common problem: objects removed from collections but their references still held elsewhere in the class
- Stack implementations are particularly vulnerable: popped elements remain in the internal array
- Classes that manage their own memory (e.g., custom Stack, List) must null out obsolete references
- Collections, caches, and listeners are common sources of memory leaks if references aren't cleared
- Register event listeners/callbacks and forget to unregister them → memory leak over time
- Weak references and soft references can help with caches and pools where objects can be reclaimed
- Use try-finally or try-with-resources to ensure cleanup of resources (files, connections, streams)
- Memory profilers and heap dumps help identify unintentional object retention

### 8. Avoid finalizers and cleaners
- Finalizers (`finalize()`) are unpredictable; execution timing depends on GC algorithm (deprecated since Java 9)
- Cleaners are the modern replacement for finalizers but still problematic; use only as safety net
- Never rely on finalizers/cleaners for timely resource cleanup; they may never run or run very late
- Finalizers can cause performance degradation; objects with finalizers are treated specially by GC
- If a finalizer throws an exception, it's ignored and the object may be left in corrupted state
- Use try-with-resources (implements `AutoCloseable`) for guaranteed, timely resource cleanup instead
- If you must use Cleaner (rare): register it as a safety net, but implement `AutoCloseable` for normal cleanup
- For resource management, prefer explicit `close()` methods called from try-finally or try-with-resources
- Objects that hold native resources should implement `AutoCloseable` interface for proper cleanup

### 9. Prefer try-with-resources to try-finally
- try-with-resources (Java 7+) automatically closes resources implementing `AutoCloseable`
- Cleaner and more readable than try-finally; eliminates need for explicit `close()` calls
- Suppressed exceptions: if resource.close() throws while handling another exception, it's automatically suppressed (accessible via `getSuppressed()`)
- try-finally can mask exceptions: if both try block and finally throw, finally's exception replaces try's (context is lost)
- Multiple resources can be managed in single try-with-resources declaration (semicolon separated)
- Resource initialization happens in try statement; resource goes out of scope and closes automatically
- Even if exception occurs, resources are guaranteed to close (including cleanup exceptions)
- Backwards compatible pattern: any resource implementing `AutoCloseable` works with try-with-resources
- Rarely need try-finally for resource cleanup anymore; use only for non-resource cleanup logic

### 10. Obey the general contract when overriding equals
- Reflexive: `x.equals(x)` must be true
- Symmetric: if `x.equals(y)` then `y.equals(x)` must be true
- Transitive: if `x.equals(y)` and `y.equals(z)` then `x.equals(z)` must be true
- Consistent: repeated calls to `equals()` return the same result (if object hasn't changed)
- For null: `x.equals(null)` must return false (not throw NullPointerException)
- Use `instanceof` check first to verify object type and handle null simultaneously
- Compare primitive fields with `==`; compare object fields recursively with `equals()`; compare float/double with `Float.compare()` or `Double.compare()` to handle NaN
- Override `hashCode()` whenever you override `equals()` (equal objects must have equal hash codes)
- Don't try to make equals "smarter" than necessary; stick to value comparison for the type
- Override equals only when necessary; if value semantics aren't meaningful for your class, don't override it

### 11. Always override hashCode when you override equals
- If two objects are equal according to `equals()`, they MUST have the same hash code
- Violating this breaks hash-based collections: HashMap, HashSet, Hashtable
- Hash code doesn't need to be unique; many objects can have the same hash code
- Use same fields in hashCode() that you use in equals() for consistency
- Simple formula: multiply field hash codes by a prime (e.g., 31): `result = 31 * result + field.hashCode()`
- For primitive fields: use wrapper class hashCode (e.g., Integer.hashCode(int), Long.hashCode(long))
- For object fields: use Objects.hashCode(field) to handle null safely
- For arrays: use Arrays.hashCode() or Objects.hash() for multiple fields
- Objects.hash(field1, field2, ...) provides convenient varargs approach but slower (due to autoboxing)
- Cache hashCode if expensive to compute and object is immutable; mark field transient if serializing

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
