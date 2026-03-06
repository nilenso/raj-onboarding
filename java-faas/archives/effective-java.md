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

### 12. Always override toString
- Default `Object.toString()` is nearly useless; returns class name + hash code (e.g., `Point@5f2a8da1`)
- Provide a meaningful string representation that makes debugging easier
- Include all significant fields in the representation so developers can understand the object state
- Document the format of `toString()` in javadoc if it has a specific structure (helps parsing/backwards compatibility)
- Decide whether to use a spec-like format or informal format; be consistent with decision
- Example good formats: `Point{x=1, y=2}` or `User(name=John, age=30, email=john@example.com)`
- Never include sensitive information (passwords, API keys, SSNs) in toString output
- `toString()` should never throw exceptions; handle nulls and edge cases gracefully
- Use IDE or Lombok to generate `toString()` to avoid missing fields
- Override `toString()` makes logging, debugging, and error messages much more informative

### 13. Override clone judiciously
- `Cloneable` interface is problematic; it doesn't declare `clone()` method but signals intent via marker
- Implementing `Cloneable` and calling `super.clone()` returns a shallow copy of the object
- Shallow copy means: primitive fields are copied, object field references are NOT copied (both point to same object)
- Override `clone()` only if you truly need object copying; prefer copy constructor or copy factory instead
- If overriding clone, call `super.clone()` first, then make defensive copies of mutable fields
- For arrays: use `array.clone()` to create independent copy of the array
- `clone()` throws `CloneNotSupportedException` (checked exception) even though it's typically unchecked in practice
- Copy constructor or factory method is simpler, safer, and doesn't require implementing `Cloneable`
- Example: `public Point(Point p) { this.x = p.x; this.y = p.y; }` instead of overriding clone()
- Immutable classes don't need clone() since copies would be indistinguishable from originals

### 14. Consider implementing Comparable
- `Comparable<T>` interface defines natural ordering for objects; implement when natural order exists
- Single method: `int compareTo(T o)` returns negative/zero/positive if this < o / == o / > o
- Enables sorting, binary search, and use in sorted collections (TreeMap, TreeSet)
- compareTo() must be consistent with equals(): if `a.equals(b)` then `a.compareTo(b) == 0` (recommended but not enforced)
- Contract: compareTo must be transitive, consistent, and reflexive (same as equals contract)
- Compare fields in order of importance; stop at first difference for efficiency
- Use field.compareTo() for objects, Comparator.compare() for primitives, or Integer.compare() for primitive wrappers
- For multiple fields: return immediately upon first non-zero comparison, don't sum results
- Use Comparator.comparing() for complex/chained comparisons with better readability
- Don't subtract primitive values: `a.age - b.age` can overflow; use Integer.compare(a.age, b.age) instead
- Implement Comparable only for classes with a clear, natural ordering; use Comparator for alternative orderings

### 15. Minimize the accessibility of classes and members
- Use the principle of least privilege: make classes and members as private as possible
- Access modifiers in Java: private < package-private < protected < public
- Private: accessible only within the class; most restrictive, preferred default
- Package-private: accessible within same package; used for internal implementation details
- Protected: accessible within package + subclasses; only use when inheritance is intended
- Public: part of public API; once public, must be maintained for backwards compatibility
- Make fields private and expose through public methods if access is needed
- Mutable public fields are dangerous; prefer immutable public fields or accessor methods
- Top-level classes should rarely be public; most should be package-private
- Minimize the public API; it represents a contract you must maintain forever
- Classes used internally should be package-private to allow internal refactoring without affecting clients

### 16. In public classes, use accessor methods, not public fields
- Public fields provide no encapsulation; clients depend on internal representation
- If you expose a field, you can never change its implementation without breaking clients
- Accessor methods (getters/setters) allow validation, lazy initialization, and side effects
- Immutable public fields are acceptable (field is `public static final` and value is truly immutable)
- Mutable public fields are dangerous; even `public final` fields can be modified if they reference mutable objects
- Private fields with public accessor methods enable refactoring without API changes
- For package-private or private classes, public fields are acceptable (less important to hide implementation)
- Consider whether you need both getter and setter; read-only fields need only getter
- When adding validation to a setter, existing code still works but now enforced
- Use `final` fields when appropriate to signal immutability; combine with private + accessor for public API

### 17. Minimize mutability
- Immutable objects are simpler, safer, and can be shared freely without defensive copying
- Make classes immutable by default; provide mutable alternatives only when necessary
- To create an immutable class: make fields final and private; don't provide mutators; ensure subclasses can't override
- Make class final or use private constructor with factory to prevent subclassing
- All fields should be final; if computed fields needed, cache them (always return same value)
- Immutable objects are inherently thread-safe; no synchronization needed
- Share immutable objects freely; no defensive copies required (String, Integer, etc. are reused)
- BigDecimal mistake: provides mutable methods; avoid mutating in multi-threaded contexts
- Disadvantage of immutability: need new object for each state change (create builder for multi-step construction)
- Performance: immutable objects can be pooled/cached; garbage collection is simpler for short-lived objects

### 18. Favor composition over inheritance
- Inheritance violates encapsulation; subclass depends on superclass implementation details
- "Fragile base class problem": changes to superclass can break subclasses unexpectedly
- Inheritance is appropriate only when subclass is truly a subtype of superclass (is-a relationship)
- Use composition (has-a) for most cases: wrap inner object, delegate to it, provide new API
- Composition more flexible: can change wrapped object at runtime; inheritance is static
- Forwarding: delegate method calls to wrapped object; decorator/wrapper pattern
- Document the self-use of inherited methods; if subclass overrides a method that calls itself, subclass may break
- Design for inheritance or prohibit it: provide detailed documentation of internal method dependencies
- Prefer interfaces to abstract classes for defining contracts; interfaces allow composition + implementation reuse

### 19. Design and document for inheritance, or else prohibit it
- Classes open for extension must document their self-use: which methods call which other methods internally
- Use `@implSpec` javadoc tag to document implementation contracts (how, not just what)
- Protected methods and fields must be intentional; make class final if not designed for extension
- Provide reasonable protected hooks for extension: protected constructors, protected utility methods
- Never call overridable methods from constructors (subclass initialization may not be complete)
- Similarly, avoid calling overridable methods during deserialization or clone()
- Write test subclasses during development to verify extensibility works correctly
- Document performance characteristics relevant to subclasses (time/space complexity)
- Either design for inheritance explicitly or make class final to prevent accidental misuse
- Inheritance is costly: once a class is extended, you're committed to its internal implementation

### 20. Prefer interfaces to abstract classes
- Interfaces support multiple inheritance (implement multiple interfaces); classes allow only single inheritance
- Interfaces define types; a class can implement any interface without being a subclass
- Existing classes can be retrofitted to implement new interfaces (don't need to extend superclass)
- Interfaces are perfect for defining mixins: optional functionality added to existing classes
- Interfaces can serve as service provider frameworks: define service interface, implementation, and registry
- Java 8+: interfaces can have default methods (provide implementation); classes not limited to abstract patterns
- Skeletal implementation classes: provide abstract class with default implementations, paired with interface
- Example: Collection interface + AbstractCollection skeletal class for convenience
- Abstract classes better only for: defining state variables (interfaces can't have instance fields)
- Combining both: interface for contract + abstract class as convenience (minimal boilerplate for implementers)

### 21. Design interfaces for posterity
- Interfaces are part of your public contract; breaking changes affect all implementations across codebase and clients
- Java 8+ default methods allow adding methods to interfaces without breaking existing implementations
- Default methods require careful design: they must have sensible default behavior that doesn't break contracts
- Document default methods thoroughly; implementers may not override them and depend on defaults
- Never add default methods that conflict with Object methods (equals, hashCode, toString)
- Interface evolution: use @Deprecated annotations with replacement guidance when adding or changing behavior
- Consider "sealed" interfaces (Java 15+): restrict which classes can implement, enabling safer evolution
- Avoid concrete return types in interface methods; use supertypes to allow implementation flexibility
- Design for common use cases; rare edge cases handled by concrete implementations or utilities
- Keep interfaces focused and cohesive; don't create "god interfaces" that do too many things
- Prefer smaller, composable interfaces over large monolithic ones (Single Responsibility Principle)

### 22. Use interfaces only to define types
- Interfaces should define what a class *is*, not what constants it *contains*
- Constant interface antipattern: interface with only `static final` fields (e.g., `interface PhysicsConstants { double PI = 3.14; }`)
- Problems with constant interfaces: pollutes API, leaks implementation detail, forces implementers to carry constants forever
- Implementing a constant interface commits you to its values as part of your exported API (can't remove later)
- Constants belong in: the class/interface they're closely associated with, or a utility class with private constructor
- Example: `Integer.MAX_VALUE`, `Math.PI` — constants tied to their relevant class, not a separate interface
- Use static imports (`import static java.lang.Math.PI`) for frequently used constants instead of implementing an interface
- If constants are truly shared across multiple classes, create a noninstantiable utility class (Item 4)
- Interfaces are for defining types that permit multiple implementations; not for exporting constants
- A class implementing an interface says "I am this type"; implementing a constant interface says nothing meaningful

### 23. Prefer class hierarchies to tagged classes
- Tagged class: a single class with a "tag" field indicating which variant/flavor it represents
- Antipattern example: `class Shape { int shapeType; int radius; int width; int height; }` (confusing, error-prone)
- Problems with tagged classes: cluttered with fields for different variants, switch statements on tag, hard to extend, fragile
- Different variants may need different fields; mixing them in one class creates dead fields and confusion
- Class hierarchies solve this: abstract base class `Shape` with concrete subclasses `Circle`, `Rectangle`, `Triangle`
- Each subclass has only relevant fields; no dead/unused fields like in tagged class variant
- Extensibility: add new shapes by creating new subclasses, not modifying existing code (Open/Closed Principle)
- Type safety: compiler enforces correct fields for each shape; no tag-switching required
- Polymorphism: call methods like `shape.area()` without switch statements; correct implementation called automatically
- Tagged classes are verbose, error-prone, and inefficient; hierarchies are cleaner and leverage OOP
- Example improvement: tagged class with `switch(shapeType)` everywhere → class hierarchy with polymorphic `area()` method
- Hierarchies allow each variant to override behavior differently; tagged classes require large switch statements

### 24. Favor static member classes over nonstatic
- Nested classes: classes defined inside another class; four kinds: static member class, nonstatic member class, anonymous class, local class
- Static member class: a nested class declared `static`; has no implicit reference to enclosing class instance
- Nonstatic member class (inner class): nested class without `static`; each instance holds implicit reference to enclosing class instance
- Inner class vs. static member class: inner class wastes memory (extra reference) and can cause memory leaks (enclosing instance never GC'd)
- Use static member class by default; only use nonstatic when you need access to enclosing instance
- Static member class: can exist independently, no reference overhead, cleaner; use when relationship is "has-a" or utility
- Nonstatic member class: only when you need `Outer.this` to access enclosing instance methods/fields; use when relationship is "is-a"
- Common pitfall: making inner classes nonstatic when they don't need enclosing instance access; wastes memory
- Anonymous classes: unnamed class instantiated in place; used for one-off implementations (callbacks, small listener implementations)
- Local classes: classes declared inside methods; rarely used; less useful than anonymous classes in modern code
- Each nonstatic inner class instance holds hidden reference to enclosing instance; prevents GC and causes memory leaks if inner class outlives outer

### 25. Limit source files to a single top-level class
- Define at most one top-level class per source file; avoid multiple top-level classes in same `.java` file
- Problem: multiple top-level classes in one file → brittle, fragile code; compiler order-dependent behavior
- Example antipattern: file `Utensil.java` contains both `class Utensil` and `class Dessert`; if files included in wrong order, compilation behavior varies
- One class per file makes ownership clear: file name matches class name; enables straightforward mapping (IDE navigation works correctly)
- IDE and build tools assume one top-level class per file; violating this assumption causes confusion and tool failures
- If you need a small utility class alongside main class: make it a static member class (Item 24), not a separate top-level class
- Maintaining multiple top-level classes in one file violates Single Responsibility Principle; each class should have separate concerns
- Version control and code review become harder; changes to one class affect the entire file, complicating blame/history tracking
- Compilation is unpredictable: which class gets compiled depends on order of file inclusion in build system
- Modern practice: strict convention is one file, one public class; helps all tools (IDE, compiler, build system, developers) understand intent

### 26. Don't use raw types
- Raw type: generic class or interface used without type parameters (e.g., `List` instead of `List<String>`)
- Raw types exist for backwards compatibility with pre-generics Java code; don't use them in new code
- Using raw types defeats the purpose of generics: no type safety, no compile-time checking, casts required at runtime
- Problem example: `List list = new ArrayList(); list.add("hello"); String s = (String) list.get(0);` (manual cast, error-prone)
- Better: `List<String> list = new ArrayList<>(); list.add("hello"); String s = list.get(0);` (no cast, type-safe)
- Generics provide: type safety (errors caught at compile time, not runtime), eliminate casts, allow generic algorithms
- Raw types lose type information: compiler can't check if you're adding correct types to collection
- If you don't know/care about type parameter: use unbounded wildcard `List<?>` instead of raw `List`
- `List<?>` is type-safe: can't add anything except null; enforces safety without knowing specific type
- Difference: `List` (raw) is unsafe; `List<?>` (wildcard) is safe but less flexible
- Exceptions: `instanceof` checks require raw types (e.g., `if (obj instanceof List)` not `if (obj instanceof List<String>)`)
- Class literals require raw types (e.g., `String.class` works, but `List<String>.class` is syntax error; use `List.class`)
- Always use parameterized types in declarations, even if instantiating with raw constructor: `List<String> list = new ArrayList();` (inferred)

### 27. Eliminate unchecked warnings
- Unchecked warnings: compiler alerts about potential type safety issues with generics (e.g., raw type usage, unsafe casts)
- Goal: write generic code with zero unchecked warnings; clean compilation = type-safe code
- Common sources: raw types (Item 26), unsafe casts, varargs with generics, method signature mismatches
- Eliminate warnings at source: use parameterized types, avoid raw types, use proper generic syntax
- If you can't eliminate a warning: use `@SuppressWarnings("unchecked")` annotation with explanatory comment
- `@SuppressWarnings("unchecked")` should be scoped narrowly: apply to variable/method, not entire class
- Always document why the warning is safe when suppressing: comment explains type safety reasoning
- Example: `@SuppressWarnings("unchecked") T[] result = (T[]) new Object[size];` (generic array creation requires unsafe cast)
- Generic array creation is illegal in Java: `new T[]` doesn't compile; workaround is unsafe cast with suppression
- Compiler warnings exist for a reason: ignoring them leads to ClassCastException at runtime
- Each unchecked warning represents potential runtime failure; even if you believe it's safe, suppress narrowly with documentation
- Use IDE inspection tools to identify warnings; refactor code to eliminate them before resorting to suppression
- Varargs and generics don't mix well: `void foo(List<String>... lists)` generates warning; use `List<List<String>>` instead when possible
- Keep warnings list visible: compile with `-Xlint:unchecked` to catch new warnings during development

### 28. Prefer lists to arrays
- Arrays are covariant: if `Subclass extends Superclass`, then `Subclass[]` is subtype of `Superclass[]`
- Generics are invariant: `List<Subclass>` is NOT a subtype of `List<Superclass>`; types must match exactly
- Covariance in arrays enables runtime type errors: `Object[] arr = new String[10]; arr[0] = 1;` (compiles, fails at runtime with ArrayStoreException)
- Generics are type-safe: `List<String> list = new ArrayList<String>(); list.add(1);` (compile-time error, type mismatch)
- Arrays are reified: arrays carry runtime type information; generics are erased (type info available only at compile time)
- Type erasure: at runtime, `List<String>` and `List<Integer>` are both just `List`; generic type info is erased for backwards compatibility
- Arrays can't be parameterized: `new String[10]` works, but `new List<String>[10]` is syntax error
- Generics can't be instantiated with primitives: `List<int>` is illegal; use `List<Integer>` instead
- Due to erasure and covariance issues: arrays and generics don't mix well; prefer lists with generics
- Mixing arrays and generics leads to warnings and unsafe code: `@SuppressWarnings("unchecked")` (Item 27)
- Example improvement: `String[] arr = new String[10]` → `List<String> list = new ArrayList<>();`
- Lists provide type safety with generics; arrays lose it due to covariance and reification mismatch
- Collections framework (`List`, `Set`, `Map`) integrates seamlessly with generics; arrays do not
- Performance: modern JVMs optimize lists well; performance gap between arrays and lists is minimal in practice

### 29. Favor generic types
- Generic type: a class or interface with one or more type parameters (e.g., `List<E>`, `Map<K, V>`)
- Writing your own generic types enables code reuse and type safety for custom data structures
- Example: stack implementation; without generics `Stack` returns `Object` (requires casting); with generics returns `E` (type-safe)
- Define generic type with syntax: `class Stack<E> { ... }` where `E` is type parameter (convention: single uppercase letter)
- Common type parameter names: `E` (element), `K` (key), `V` (value), `T` (type), `N` (number)
- In generic class, use type parameter like any other type: fields, method parameters, return types
- Instance fields can't be arrays of generic types: `private E[] elements;` won't work cleanly; use `Object[]` with casts or `List<E>`
- When instantiating generic class: provide concrete type `Stack<String>` not raw `Stack` (Item 26)
- Type parameter constraints: `class Comparable<T extends Comparable<T>>` bounds type parameter
- Bounded type parameters: `<T extends Number>` restricts `T` to Number and subclasses; enables calling Number methods
- Generic methods: `public static <T> T choose(T a, T b)` allows parameterizing method independent of class
- Generic types eliminate need for casts in client code; compiler ensures type safety
- Generic variance: covariance (producer), contravariance (consumer), invariance (both); use wildcards `? extends` and `? super`
- Pay attention to type erasure (Item 28): `instanceof` checks don't work with generic types; use raw type or bounds check
- Generic types are worth the complexity: enable type-safe reusable components essential for modern Java

### 30. Favor generic methods
- Generic method: a method with type parameters independent of the class it's defined in
- Syntax: `public static <T> T method(T param)` where `<T>` declares type parameter before return type
- Generic methods can be defined in both generic and non-generic classes; enables flexible, reusable code
- Type inference: caller often doesn't need to specify type explicitly; compiler infers from arguments
- Example: `String result = genericMethod("hello");` (compiler infers `T` is `String`) vs `String result = <String>genericMethod("hello");` (explicit)
- Generic methods eliminate need for casting: `Object obj = genericMethod(...); String s = (String) obj;` → `String s = genericMethod(...);`
- Bounded type parameters enable method to call methods on type parameter: `public static <T extends Comparable<T>> T max(T a, T b)`
- Recursive type bound pattern: `<T extends Comparable<T>>` enables comparison operations while maintaining type safety
- Helper method pattern: use private generic helper to implement public non-generic method (avoids type parameter leakage)
- Generic varargs: `public static <T> void addAll(Collection<T> c, T... elements)` but generates unchecked warning (Item 27)
- Type parameter shadowing: method type parameter can shadow class type parameter; careful with naming to avoid confusion
- Generic methods work well with wildcards for maximum flexibility: `void process(List<? extends Number> list)`
- Prefer generic methods over methods taking `Object` parameters; type safety and no casting required
- Generic methods enable utility classes like `Collections` with reusable algorithms (e.g., `sort()`, `reverse()`, `shuffle()`)

### 31. Use bounded wildcards to increase API flexibility
- Bounded wildcard: `? extends Type` (upper bound) or `? super Type` (lower bound); increases API flexibility and accepts wider variety of types
- Covariance (`? extends`): read-only, producer; accepts subtypes; enables passing `List<Integer>` to method expecting `List<? extends Number>`
- Contravariance (`? super`): write-only, consumer; accepts supertypes; enables passing `List<Object>` to method accepting `List<? super Integer>`
- PECS principle: Producer Extends, Consumer Super; use `? extends` for read-only, `? super` for write-only
- Example: `public void pushAll(Iterable<? extends E> src)` accepts `Iterable<E>` and subtypes; flexible producer of elements
- Example: `public void popAll(Collection<? super E> dst)` accepts `Collection<E>` and supertypes; flexible consumer of elements
- Without wildcards: `pushAll(Iterable<E>)` only accepts exact type `E`; `popAll(Collection<E>)` only accepts exact type; inflexible
- Don't use wildcards in return types: confuses client code; use bounded wildcard in parameters only
- Generic methods vs. bounded wildcards: both flexible; use method if wildcard makes declaration complex
- Type inference with wildcards can be tricky; compiler can't infer wildcard types in all contexts
- Bounded wildcards with recursion: `<T extends Comparable<? super T>>` enables safe comparison of objects and supertypes
- Unbounded wildcard `?`: rarely useful; use for unknown type or when `? extends Object` sufficient
- Wildcard types enable seamless interoperability: list of integers can be passed to method expecting list of numbers
- API design: use bounded wildcards in public method signatures for maximum usability; clients appreciate flexibility

### 32. Combine generics and varargs judiciously
- Varargs parameter: `public void method(T... args)` allows variable number of arguments; internally creates array `T[]`
- Problem: mixing generics and varargs creates type safety issue; generic array creation is prohibited but varargs arrays are allowed
- Heap pollution: generic varargs method creates array of generic type and mixes with non-generic code; type information lost at runtime
- Example problem: `public static <T> void dangerous(T... args)` creates generic array internally; can cause ClassCastException
- Varargs with generic types generates unchecked warning: "Type safety: Potential heap pollution via varargs parameter"
- Safe varargs: use `@SafeVarargs` annotation to suppress unchecked warning only if method is provably type-safe
- Method is safe if: (1) never stores elements in varargs array, (2) never leaks array reference outside method, (3) never calls unsafe methods with array
- `@SafeVarargs` only applies to methods; can't be used on constructors (use different pattern if needed)
- Better alternatives to varargs with generics: use `List<T>` parameter instead of `T...`; `List<T>` parameter is type-safe
- Example: `public static <T> void safe(List<T> list)` instead of `public static <T> void unsafe(T... args)`
- `List<T>` approach: slight verbosity for callers (wrap array in `Arrays.asList()`) but gains type safety
- Only use `@SafeVarargs` when you're certain method is type-safe; false positives can introduce bugs
- Varargs mainly useful with non-generic types: `void log(String... messages)` is fine, no type safety issue
- If designing public API with generics varargs: document it's safe or prefer `List<T>` to avoid suppressing warnings

### 33. Consider type-safe heterogeneous containers
- Heterogeneous container: a container that holds values of different types safely; e.g., `Map<Class<?>, Object>` with type safety
- Problem: `Map<String, Object>` loses type information; retrieving values requires casting: `String s = (String) map.get("key");`
- Solution: use `Class` object as key and parameterize by key type; `Map<Class<T>, T>` enables type-safe retrieval
- Type-safe heterogeneous container pattern: use `Class<T>` as key, `T` as value; compiler enforces type safety
- Example: `private Map<Class<?>, Object> favorites = new HashMap<>();` with typed getter/setter methods
- Getter method: `public <T> T getFavorite(Class<T> type) { return type.cast(favorites.get(type)); }` (cast is safe)
- Setter method: `public <T> void setFavorite(Class<T> type, T instance) { favorites.put(type, instance); }` (type-safe)
- Benefits: single container holds multiple types safely; no raw types or unchecked casts needed in client code
- Type information preserved at runtime via `Class` object; enables safe casting and type checking
- Client code: `container.setFavorite(String.class, "Hello"); String s = container.getFavorite(String.class);` (type-safe)
- Limitations: bounded types don't work cleanly; can't use `Class<List<String>>` (type erasure issues)
- Workaround for bounded types: create super type token or use TypeToken pattern for complex generic types
- Real-world example: Spring Framework dependency injection container uses type-safe heterogeneous pattern
- Use sparingly: adds complexity; suitable when you need type-safe storage of unrelated types in single container

### 34. Use enums instead of int constants
- Int constant antipattern: `public static final int APPLE = 0; public static final int ORANGE = 1;` (type-unsafe, fragile)
- Problems with int constants: no type safety (any int accepted), no namespace (name collisions), brittle (recompilation required on value change)
- Printing int constant prints number, not name; confusing in logs/debuggers; enum prints name automatically
- Enums are type-safe: `AppleType.RED` is distinct from `OrangeType.RED`; compiler prevents mixing types
- Enums have namespace: constants are scoped to enum type; no global namespace pollution
- Enum constants are singletons created at class load time; thread-safe by default
- Enums can have methods and fields: `enum Planet { MERCURY(3.8, ...) { ... }; private double mass; Planet(double m) { mass = m; } }`
- Add behavior to enums: override abstract methods in constants, add instance methods, implement interfaces
- Example: `enum Operation { PLUS("+") { public double apply(double x, double y) { return x + y; } }, ...}`
- Enums can implement interfaces: `enum Operation implements Calculator { ... }` enables polymorphic behavior
- Use `switch` with enums: cases exhaustive if enum doesn't grow; compiler warns if new cases added without updating switch
- Enum constants can have state: each constant can carry associated data (planets' mass, diameter, etc.)
- Performance: enums slightly more memory overhead than int constants but negligible; type safety worth the cost
- Never use ordinal() to retrieve constant values; use enum instance variables instead for maintainability
- Enums can be `Serializable`, `Comparable` automatically; serialization preserves singleton property

### 35. Use instance fields instead of ordinals
- Ordinal method: `Enum.ordinal()` returns the position of constant in enum declaration (0-based index)
- Antipattern: using `ordinal()` to retrieve data associated with enum constants; brittle and error-prone
- Problem: ordinal depends on declaration order; reordering enum constants breaks code relying on ordinal values
- Example antipattern: `enum Ensemble { SOLO, DUET, TRIO, ...; int numberOfMusicians() { return ordinal() + 1; }}`
- Issue: if you reorder constants or insert new ones, ordinal values change, breaking logic
- Solution: store associated data in instance fields, not ordinal position
- Better approach: `enum Ensemble { SOLO(1), DUET(2), TRIO(3), ...; final int musicians; Ensemble(int m) { musicians = m; }}`
- Then retrieve with instance field: `ensemble.musicians` is explicit, maintainable, safe from reordering
- Instance fields with constructor: each enum constant can initialize its data independently
- Instance fields are self-documenting: code clearly shows what data each constant carries
- Ordinal is an implementation detail; only use in internal data structures (e.g., index into array for performance)
- Never expose ordinal in public API; if you need to index, use `EnumMap` instead
- `EnumMap<E, V>`: optimized map using enum ordinals internally; type-safe and efficient without exposing ordinal
- Example: `Map<Ensemble, Integer> map = new EnumMap<>(Ensemble.class);` uses ordinal internally but API hides it
- Adding new enum constants with instance fields: natural and safe; old code still works
- Ordinal() creates maintenance burden; instance fields are clear, maintainable, resistant to changes

### 36. Use EnumSet instead of bit fields
- Bit field antipattern: use int/long constants as bit flags combined with bitwise operations
- Example antipattern: `public static final int STYLE_BOLD = 1 << 0; int style = STYLE_BOLD | STYLE_ITALIC;` (obscure, error-prone)
- Problems: bit fields obscure meaning; bitwise operations confusing; no type safety; hard to print/debug (just an integer)
- EnumSet solution: use `EnumSet<E>` to represent a set of enum constants efficiently
- EnumSet is backed by bit vector internally; memory-efficient like bit fields but type-safe and clear
- Example: `enum Style { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH }; Set<Style> styles = EnumSet.of(BOLD, ITALIC);`
- EnumSet API is intuitive: `styles.add(UNDERLINE)`, `styles.remove(BOLD)`, `styles.contains(ITALIC)`
- Type-safe and self-documenting: code clearly shows which enum values are stored
- Printing EnumSet is readable: `[BOLD, ITALIC]` vs. bit field printing just `3`
- Performance: EnumSet internally uses single long/bit vector (or byte array for large enums); efficient as bit fields
- Immutable variant: `Set<Style> immutableStyles = Collections.unmodifiableSet(EnumSet.of(BOLD))` for compile-time sets
- Factory methods: `EnumSet.of(...)`, `EnumSet.allOf(...)`, `EnumSet.noneOf(...)`, `EnumSet.range(...)`
- EnumSet is not thread-safe; use `Collections.synchronizedSet()` or `ConcurrentHashMap` if needed
- Methods accepting bit fields: retrofit to accept `Set<E>` instead of int; EnumSet is efficient parameter type

### 37. Use EnumMap instead of ordinal indexing
- Ordinal indexing antipattern: use enum constant's `ordinal()` as array/map index to store related data
- Example antipattern: `Plant[] plants = new Plant[Type.values().length]; plants[Type.ANNUAL.ordinal()] = annual;`
- Problems: ordinal-based indexing is error-prone, type-unsafe, brittle (reordering enum breaks everything), unclear intent
- EnumMap solution: `Map<Type, Plant> map = new EnumMap<>(Type.class);` designed specifically for enum keys
- EnumMap internally uses ordinal for efficient implementation (compact array); no manual indexing needed
- Type-safe: compiler prevents using wrong enum type as key; clear that keys are enum constants
- Self-documenting: code shows intent; accessing `map.get(Type.ANNUAL)` is clearer than `array[Type.ANNUAL.ordinal()]`
- Reordering enum constants: safe with EnumMap; ordinal changes internally handled correctly
- Performance: EnumMap as efficient as ordinal-based arrays; same internal representation, better API
- Supports all Map operations: `put()`, `get()`, `remove()`, `containsKey()`, etc.; familiar interface
- Null values allowed: EnumMap can store null; ordinal array would need special handling
- EnumMap iteration order: in constant declaration order; predictable and useful
- Example: `enum Phase { SOLID, LIQUID, GAS }; Map<Phase, Map<Phase, Transition>> transitions = new EnumMap<>(Phase.class);`
- Nested EnumMaps: support complex mappings (e.g., phase transitions); both maps type-safe
- Never use ordinal-based arrays when EnumMap is available; always prefer EnumMap for enum keys

### 38. Emulate extensible enums with interfaces
- Limitation: enums can't extend other enums or classes (except Object); fixed set of constants in single enum type
- Extensibility requirement: sometimes you want enum-like types that can be extended by clients (e.g., operation codes)
- Solution: use interface to define enum behavior; implement interface with different enum types
- Pattern: define interface `interface Operation { double apply(double x, double y); }` and multiple enum implementations
- Example: `enum BasicOperation implements Operation { PLUS, MINUS, TIMES, DIVIDE; ... }`
- Client can extend: `enum ExtendedOperation implements Operation { POWER, MOD; ... }` adds new operations
- Both enums implement same interface; clients can accept interface type `Operation` instead of specific enum
- Benefits: clients code against interface; new enums can be added in different modules without modifying existing code
- Utility function pattern: `public static <T extends Enum<T> & Operation> void test(Class<T> opSet) { ... }`
- Bounded type parameter: `<T extends Enum<T> & Operation>` requires both enum (singleton) and operation interface
- Reflection: use `T.getEnumConstants()` to iterate all constants of enum type `T`
- Downside: can't express "all types implementing interface are enums"; Java's type system doesn't support that
- API design: accept interface type `Operation`, not specific enum; enables future extensions
- Collections of mixed implementations: `Set<Operation>` can hold PLUS, MINUS, POWER, MOD from different enums
- Trade-off: extensibility vs. enum guarantees (singletons, serialization, reflection-safe)

### 39. Prefer annotations to naming patterns
- Naming pattern antipattern: use method/class naming convention to indicate metadata (e.g., test methods start with "test")
- Example: test frameworks look for methods matching `test.*` pattern; relies on convention, not type safety
- Problems: compiler can't verify pattern compliance; typos create silent bugs (e.g., `testSafetyOverride()` not recognized as test)
- Annotations solve this: use `@Test` marker to explicitly declare intent; compiler and tools can verify
- Annotation advantages: type-safe, can't be ignored, provide structure for tools, self-documenting
- Marker annotation: `@interface Test {}` no parameters; just marks presence (e.g., `@Test void someMethod()`)
- Parameterized annotation: `@interface ExceptionTest { Class<? extends Throwable>[] value(); }` carries metadata
- Annotations processed by: compiler, annotation processors (compile-time), runtime reflection
- Built-in annotations: `@Override`, `@Deprecated`, `@FunctionalInterface` enable compiler checking
- Custom annotations: define test behavior, validation rules, documentation metadata
- Meta-annotations: `@Target`, `@Retention` control where annotation applies and how long it lasts
- `@Target(ElementType.METHOD)` limits annotation to methods only; prevents misuse
- `@Retention(RetentionPolicy.RUNTIME)` keeps annotation in runtime bytecode; enables reflection-based processing
- Processing annotations: `Class.getDeclaredMethods()` + check for `@Test` annotation; enables dynamic behavior
- Migration: add annotations alongside naming patterns; gradually deprecate patterns as tools support annotations
- Modern Java: annotations are standard for frameworks (Spring, JUnit); naming patterns rarely appropriate anymore

### 40. Consistently use the @Override annotation
- `@Override` annotation: marks a method as overriding a superclass/interface method; enables compiler verification
- Purpose: prevent bugs from typos or signature mismatches; compiler catches when override intent fails
- Problem without `@Override`: subtle bugs if you misspell method name or parameters; method becomes new method, not override
- Example bug: `public boolean equals(Object obj)` intended to override, but typo `public boolean equals(String obj)` (overload, not override)
- With `@Override`: compiler error if signature doesn't match any superclass/interface method; catches intent failure
- Rule: always use `@Override` when overriding method from superclass or interface; no exceptions
- Applies to: instance methods, static methods (for hidden methods), interface methods
- Abstract methods: override `@Override` even if providing abstract implementation (documents intent)
- `@Override` is purely compile-time; not retained at runtime; zero runtime overhead
- IDE support: modern IDEs warn if `@Override` is missing; easy to add automatically
- Refactoring safety: if superclass method is removed/renamed, `@Override` on subclass causes compiler error (early detection)
- Default methods in interfaces: use `@Override` if implementing class overrides the default implementation
- Multi-level hierarchy: `@Override` works across inheritance chains; compiler checks against any supertype
- Convention: some teams make `@Override` mandatory via code style checkers; highly recommended
- Never ignore compiler warnings about `@Override`; they indicate real issues in code logic

### 41. Use marker interfaces to define types
- Marker interface: interface with no methods; signals that implementing class is intended for special treatment
- Purpose: indicate a semantic property or capability without adding methods; used by frameworks/tools
- Examples: `Serializable`, `Cloneable`, `RandomAccess` (all marker interfaces in Java standard library)
- Advantage over marker annotation: defines a type; implementing class can be used wherever that type expected
- Type-safety benefit: `void process(Serializable s)` only accepts types that implement `Serializable`; compile-time checked
- Marker annotations (`@interface`) alternative: carries no methods; signals intent via annotation instead of interface
- When to use marker interface vs annotation: if the marked type will be used as type in method signatures, use interface
- Method parameter: `void persist(Serializable obj)` is type-safe with interface; can't restrict parameter to annotated types
- Instanceof check: `if (obj instanceof Serializable)` works with interface; annotations don't support instanceof
- Framework processing: tools can check `if (clazz.isInstance(obj))` at runtime to apply special handling
- Naming convention: marker interfaces often named after capability (e.g., `Serializable` = "can be serialized")
- Downsides of marker interfaces: less flexible than annotations (can't retroactively mark existing classes)
- Retrofit: `Serializable` was retrofitted to existing classes after interface introduction; some classes can't be modified
- Annotation upside: can be applied to any type retroactively (method, field, class); interface requires code modification
- Hybrid approach: use interface when you need to enforce typing; use annotation for metadata without typing requirement
- `java.io.Serializable`: exemplar marker interface; used to indicate objects can be serialized/deserialized

### 42. Prefer lambdas to anonymous classes
- Lambdas (Java 8+): concise syntax for functional interfaces; replace verbose anonymous class boilerplate
- Example: `Collections.sort(list, (a, b) -> a.compareTo(b));` vs anonymous `Comparator` class
- Works with functional interfaces: single abstract method (SAM); e.g., `Comparator<T>`, `Runnable`, `Consumer<T>`
- Type inference: compiler infers parameter types from context
- Variable capture: can access enclosing scope; must be effectively final
- Keep anonymous classes when: need state (fields), multiple methods, or multiple interfaces
- Method/constructor references preferred: `String::compareTo`, `ArrayList::new` more concise than lambdas

### 43. Prefer method references to lambdas
- Method reference: shorthand for lambda calling single existing method; more concise and readable
- Four kinds: static (`Integer::sum`), bound instance (`str::length`), unbound instance (`String::compareTo`), constructor (`ArrayList::new`)
- Example: `System.out::println` vs `v -> System.out.println(v)`
- Use when: lambda just forwards to existing method; use lambda when adding logic not in single method
- Same performance; method reference clearer and names intent explicitly

### 44. Favor the use of standard functional interfaces
- Use `java.util.function` package (43+ interfaces); don't create custom if standard exists
- Core six: `UnaryOperator<T>` (T→T), `BinaryOperator<T>` (T,T→T), `Predicate<T>` (T→boolean), `Function<T,R>` (T→R), `Supplier<T>` (→T), `Consumer<T>` (T→void)
- Primitive variants: `IntConsumer`, `LongFunction<R>`, `DoubleUnaryOperator`; avoid autoboxing
- Multi-argument: `BiFunction<T,U,R>`, `BiConsumer<T,U>`, `BiPredicate<T,U>`
- Create custom only if: standard inadequate (e.g., checked exceptions); annotate with `@FunctionalInterface`

### 45. Use streams judiciously
- Stream pipeline: source → intermediate ops (lazy: filter, map) → terminal op (triggers execution: collect, reduce)
- When streams shine: filtering, mapping, combining; short clear pipelines
- When to avoid: complex logic, multiple variables, break/continue, debugging (harder stack traces)
- Performance: overhead exists; simple ops may be slower than loops
- Avoid side effects: use `collect()` not `forEach()` for accumulation; stateful lambdas problematic
- Balance: choose based on readability and performance; switch to loop if stream too complex

### 46. Prefer side-effect-free functions in streams
- Pure functions: return result based on inputs only; no side effects (no external state modification)
- Antipattern: `stream.forEach(x -> list.add(x))` or `forEach(x -> total += x)` (mutating state)
- Use collectors: `collect(toList())`, `collect(summingInt(...))` instead of forEach with side effects
- Pure predicates/transformations: `filter(x -> x > 5)` good; `filter(x -> seen.add(x))` bad (stateful)
- Pure functions enable: safe parallelization, predictable debugging, clean composition

### 47. Prefer Collection to Stream as a return type
- Collections preferred: reusable (iterate multiple times), familiar API; streams consumed after terminal operation
- Tradeoff: `Collection<T>` materializes all; `Stream<T>` lazy (memory-efficient for large datasets)
- If fits in memory: return `Collection<T>`; if too large: return `Stream<T>` or `Iterable<T>`
- Collection return enables: clients can iterate multiple times or convert to stream if needed: `method().stream().filter(...)`

### 48. Use caution when making streams parallel
- Parallel streams: `parallel()` distributes across threads; often slower due to overhead (thread creation, coordination)
- Best case: large dataset + expensive per-element operation; worst case: small dataset, cheap operation
- Data source matters: arrays/ArrayList parallelizable (random access); LinkedList poor (sequential)
- Stateless pure functions safe; stateful lambdas cause race conditions
- Common mistake: parallelizing I/O-heavy ops (network latency dominates; no benefit from parallelization)
- Benchmark first; default to sequential; use parallel only when profiling proves benefit

### 49. Check parameters for validity
- Validate immediately at method entry; catch errors early before propagation
- Use `Objects.requireNonNull()`: throw NPE if null; `Objects.checkIndex()` for ranges
- Document constraints in javadoc: invalid values, exceptions thrown
- Constructor validation: prevent partially-constructed objects from escaping
- Public methods validate; package-private/private can skip if caller controlled
- Validate before modifying state; ensure no side effects if validation fails

### 50. Make defensive copies when needed
- Copy mutable parameters on entry: `this.start = new Date(start.getTime());` before storing
- Copy mutable fields on return: `return new Date(start.getTime());` prevent external modification
- Don't trust caller; defense in depth principle
- Immutable alternatives preferred (`Instant` vs `Date`)
- Arrays: `array.clone()` or `Arrays.copyOf()`; Collections: `Collections.unmodifiableList(...)`

### 51. Design Method Signatures Carefully
- **Choose method names wisely**: clear, consistent with naming conventions; avoid misleading names
- **Don't have too many parameters**: 3-4 max; use helper objects or builders for many related params
- **Avoid long parameter lists**: confusing, error-prone; group related params into types
- **Parameter order matters**: put most frequently used first; group related types together
- **Use overloading sparingly**: can obscure intent; prefer descriptive names
- **Prefer enum params over multiple booleans**: `boolean showWarnings` → `WarningLevel level`
- **Return empty collections, not null**: `Collections.emptyList()` vs `null`; easier to use, fewer null checks

### 52. Use Overloading Judiciously
- **Overloading resolved at compile time**: picked based on compile-time types, not runtime types
- **Avoid confusing overloads**: when overloads have overlapping parameter types, behavior becomes unpredictable
- **Safe overloading**: parameter types should differ radically (no shared supertype that both could match)
- **Prefer explicitly named methods**: `writeInt()`, `writeList()` clearer than overloaded `write()` with different params
- **Varargs + overloading = danger**: consider which overload gets called; hard to reason about
- **Example pitfall**: `Collection.remove(Object)` vs `List.remove(int)` — easy to call wrong one accidentally
- **When you must overload**: document clearly; keep behavior identical across overloads

### 53. Use Varargs Judiciously
- **Varargs create array internally**: `void foo(int... args)` → new array allocated on every call
- **Performance cost**: avoid varargs in performance-critical paths; use fixed-arg overloads instead
- **Combine with overloading carefully**: varargs + overloading = ambiguity risk (see Item 52)
- **Common pattern**: fixed arg + varargs overload — `foo(int a)` and `foo(int a, int... rest)`
- **Varargs require at least 1 arg normally**: use `int... args` but consider if 0 args is valid use case
- **Don't abuse for optional params**: varargs suggests "0 or more"; if semantically "exactly 3", use 3 fixed params
- **Documentation crucial**: clarify expected usage; varargs signature less obvious than fixed params

### 54. Return Empty Collections or Arrays, Not Nulls
- **Null forces callers to check**: every call site must guard `if (result != null)`, error-prone
- **Empty collections are safe**: no NPE risk; iteration over empty collection does nothing safely
- **Collections.emptyList/Set/Map**: immutable, reusable singletons; zero allocation cost
- **Arrays.copyOf() for arrays**: `return new int[0];` or use Collections if possible; never return null
- **Streams**: empty stream `Stream.empty()` safe to chain operations on
- **Performance myth**: "allocating empty collection is wasteful" — use singletons; same object returned repeatedly
- **Cleaner caller code**: `for (Item item : getItems()) {}` works if getItems() returns `Collections.emptyList()`
- **Document the guarantee**: if you never return null, state it; callers can rely on non-null contract

### 55. Return Optionals Judiciously
- **Optional for "no result" cases**: use when result may legitimately be absent (not all inputs have answer)
- **Never return `Optional.empty()` from collections/arrays**: return empty collection instead (Item 54)
- **Avoid Optional<List<T>>**: return `Collections.emptyList()` if empty; Optional adds redundant wrapping
- **Performance cost**: Optional allocation overhead; avoid in hot paths or collection elements
- **Optional chaining**: `opt.map().filter().flatMap()` cleaner than null checks for chain transformations
- **Unwrapping**: `.get()` throws if empty (use only when you're sure); `.orElse(default)` or `.ifPresent()` safer
- **Don't overuse**: if most calls have result, Optional is noise; nullability is implicit in method signature
- **Document when absent**: if Optional, explain conditions under which result is absent

### 56. Write Doc Comments for All Exposed API Elements
- **Every public class, interface, method, field**: needs doc comment explaining purpose and behavior
- **Javadoc syntax**: `/** ... */`; processed by `javadoc` tool to generate HTML documentation
- **@param, @return, @throws**: document all parameters, return value, checked exceptions
- **Summary sentence first**: one-line description; should be readable as standalone summary
- **Use third person**: "Returns the foo" not "Return the foo"; passive voice for clarity
- **Include preconditions/postconditions**: what must be true before call, what's guaranteed after
- **Thread safety**: document if methods are thread-safe or require external synchronization
- **Example code in comments**: `@example` or code block helps clarify non-obvious behavior
- **Don't duplicate obvious information**: don't document what code already makes clear

### 57. Minimize the Scope of Local Variables
- **Declare close to first use**: reduces distance between declaration and use; easier to track lifetime
- **Prefer loop variables in for-loop**: `for (int i = 0; i < n; i++)` scopes `i` to loop only, not entire method
- **Nearly final variables**: declare as close to assignment as possible; less code between declaration and use
- **Declare in narrowest block**: if var used only in if-branch, declare inside `if` block, not method scope
- **Smaller scope = fewer conflicts**: less chance of accidental reuse or shadowing; easier to refactor
- **Methods should be short**: easier to minimize scope if methods do one thing; long methods force wider scopes
- **Iterator scope matters**: `for (Iterator<Foo> it = list.iterator(); it.hasNext();)` vs `Iterator<Foo> it = ...` outside loop

### 58. Prefer For-Each Loops to Traditional For Loops
- **For-each syntax clearer**: `for (Item item : items)` vs `for (int i = 0; i < items.size(); i++)`; intent obvious
- **Works with Iterable**: any type implementing `Iterable` works with for-each; arrays, Collections, custom types
- **No index management**: eliminates off-by-one errors; can't accidentally misuse index variable
- **Hides iterator details**: implementation handles iteration; caller focuses on element processing
- **Streams as alternative**: `items.stream().forEach(item -> ...)` functional style for complex transformations
- **When traditional for needed**: accessing index simultaneously, modifying/removing during iteration, nested loop control
- **Performance identical**: for-each compiles to same bytecode as explicit iterator or indexed loop
- **Parallel iteration pitfall**: for-each doesn't expose enough control for parallel streams; use `parallelStream()` instead

### 59. Know and Use the Libraries
- **Don't reinvent the wheel**: standard library (java.util, java.lang, etc.) is battle-tested, optimized
- **Common task? Check Collections/Arrays/Streams**: before writing utility, search if it exists
- **Example**: `Collections.shuffle()`, `Collections.sort()`, `Math.random()` — don't DIY
- **Performance gains**: library implementations often use algorithms optimized for real-world usage patterns
- **Bug fixes and security patches**: library code gets maintained; custom code becomes your maintenance burden
- **Learn API surface**: knowing what's available saves time and reduces bugs; skim Collections, Arrays javadoc
- **Third-party libraries**: Apache Commons, Guava solve common problems; evaluate before building custom solutions
- **Legacy code warning**: older JDK versions lacked some conveniences (Streams, Optional); modern Java is richer

### 60. Avoid Float and Double If Exact Answers Are Required
- **Floating-point imprecision**: `0.1 + 0.2 != 0.3` due to binary representation; cascades in calculations
- **Use BigDecimal for money**: `BigDecimal` exact decimal arithmetic; required for financial calculations
- **int/long for small ranges**: if values fit in `int` or `long`, use those; exact integer arithmetic
- **BigDecimal drawback**: slower than primitives, more verbose; only use when precision required
- **Never use float/double for currency**: `double price = 0.1 + 0.2` gives wrong answer; use `BigDecimal`
- **String constructor for BigDecimal**: `new BigDecimal("0.1")` exact; `new BigDecimal(0.1)` still wrong
- **Scale/rounding**: specify `scale` (decimal places) and `RoundingMode` (HALF_UP, etc.) explicitly
- **Alternative: cents as long**: store monetary amounts as integers (cents), avoid decimals entirely

### 61. Prefer Primitive Types to Boxed Primitives
- **Boxed primitives are objects**: `Integer`, `Long`, etc.; heap allocation, null-able, slower
- **Three differences**: identity vs equality, null values possible, performance cost
- **Autoboxing/unboxing**: `Integer i = 5;` boxes automatically, but creates object overhead
- **Use primitives in arrays**: `int[]` far cheaper than `Integer[]`; no null handling needed
- **Collections require boxed types**: `List<Integer>` not `List<int>`; unavoidable in generics
- **Comparison pitfall**: `Integer a = 1; Integer b = 1; a == b` may be true (cache) or false; use `.equals()`
- **Null handling**: boxed primitives can be null; check before unboxing to avoid NPE
- **Performance-critical code**: prefer primitives; boxing allocation visible in profiler
- **Default values differ**: primitive `int` defaults to 0; boxed `Integer` defaults to null

### 62. Avoid Strings Where Other Types Are More Appropriate
- **Strings are overused**: tempting for any data, but lose type safety and expressiveness
- **Instead of string keys**: use enums, types, or dedicated classes; `enum Suit { HEARTS, ... }` vs `String "HEARTS"`
- **Numeric data as String**: parse overhead, error-prone; use `int`, `BigDecimal` directly
- **Type-safe alternatives**: `boolean parseBoolean(String)` vs storing as `"true"` string
- **Thread names not identifiers**: use `Thread.currentThread().setName()` for logging; not for identity
- **Enum instead of string constants**: `Color.RED` vs `"RED"`; compiler checks, no typos possible
- **Capability tokens as String?**: `String capability = "admin"` — use dedicated `Role` type instead
- **Performance**: string manipulation (parsing, concatenation) slower than using appropriate types

### 63. Beware the Performance of String Concatenation
- **String concatenation creates new objects**: `s = s + x;` creates intermediate String each time; quadratic time in loop
- **Concatenation in loop is O(n²)**: n iterations each allocating new String; use StringBuilder instead
- **StringBuilder for dynamic strings**: `StringBuilder sb = new StringBuilder(); sb.append(...); s = sb.toString();` linear time
- **Compiler optimizes simple cases**: `String s = "a" + "b" + "c";` optimized to single constant; but loops are not
- **Don't use String concatenation in performance-critical paths**: profiler often flags it
- **toString() with concatenation**: avoid `return "foo" + x + "bar";` in frequently-called methods; use StringBuilder
- **Logging concatenation**: lazy logging preferred — `logger.debug(() -> "msg: " + expensiveCall())` avoids concat if log level off
- **Arrays.toString(), String.format()**: alternatives sometimes clearer; evaluate for specific case

### 64. Refer to Objects by Their Interfaces
- **Interface types in signatures**: parameter and return types should be interfaces, not concrete classes
- **Flexibility to change implementation**: if you depend on interface, swapping implementation doesn't break callers
- **Example**: `List<String> items = new ArrayList<>();` not `ArrayList<String> items = new ArrayList<>();`
- **Appropriate interface only**: use `List` not `Iterable`; use `Set` not `Collection` if set semantics matter
- **No interface exists?**: only then use concrete class; but consider if interface should exist
- **Collections advantages**: `List` hides implementation (ArrayList, LinkedList, etc.); callers don't know or care
- **Streams less prescriptive**: return `Stream<T>` to avoid "what implementation?" question entirely
- **Refactoring benefit**: implementation can change (ArrayList → CopyOnWriteArrayList) without API change

### 65. Prefer Interfaces to Reflection
- **Reflection loss of benefits**: no compile-time type checking; method discovery at runtime, slower
- **Reflective calls are slow**: runtime lookup, no JIT optimization; avoid in hot paths
- **Static typing vs reflection**: `foo.doWork()` (interface) vs `clazz.getMethod("doWork").invoke(foo)` (reflection)
- **Exception handling burden**: reflection throws checked exceptions (`IllegalAccessException`, etc.); verbose try-catch
- **Use reflection sparingly**: factory patterns, dependency injection frameworks (internal use okay); don't expose to API
- **Better alternative**: accept interface parameter; let caller provide implementation; call directly
- **Service loader pattern**: if plugin system needed, use `ServiceLoader` (reflection hidden); public API takes interface
- **Proxy classes reflection okay**: frameworks like Spring use reflection internally; public interfaces still exposed

### 66. Use Native Methods Judiciously
- **Native methods (JNI) cross Java boundary**: call C/C++ code; adds complexity, platform-dependent
- **Rarely worth the cost**: JVM optimizations (JIT, inlining) don't apply to native code; often slower than Java
- **Legacy reasons only**: wrapping existing C library; don't write new native code for performance
- **Security risk**: native code can crash JVM, bypass security manager; hard to debug
- **Maintenance burden**: requires C/C++ expertise, separate build steps, testing on multiple platforms
- **Modern alternatives**: most "native needed" problems solved by Java (NIO for I/O, etc.)
- **Profiling first**: before writing native, profile Java version; JIT often surprises with optimization
- **If you must**: isolate native code in separate library; well-document interface; use sparingly

### 67. Optimize Judiciously
- **First rule: don't optimize**: most code speed isn't the bottleneck; clarity > premature optimization
- **Second rule: profile before optimizing**: use profiler (JProfiler, YourKit) to find actual bottleneck; guessing wastes time
- **Measure impact**: optimization must measurably improve performance; 5% gain not worth complex code
- **API design first**: good API enables future optimization without breaking clients; premature micro-opts do neither
- **Clarity over speed**: obscure optimized code is harder to maintain, debug, and further optimize later
- **JVM is smart**: JIT compilation, inlining, dead code elimination; write clear code and let JVM optimize
- **Algorithmic improvement > constant factors**: O(n) vs O(n²) matters more than unrolling loops or cache tricks
- **Hotspot matters**: optimization in 1% of code used 99% of time has huge impact; optimization elsewhere wastes effort

### 68. Adhere to Generally Accepted Naming Conventions
- **Package names lowercase**: `com.example.myapp`; reversed domain notation standard
- **Class/interface names PascalCase**: `MyClass`, `MyInterface`; noun phrases
- **Method names camelCase**: `doSomething()`, `getFoo()`; verb phrases
- **Constant names SCREAMING_SNAKE_CASE**: `MAX_SIZE`, `DEFAULT_TIMEOUT`; all caps with underscores
- **Boolean getter prefix is**: `isActive()`, `hasItems()` not `getActive()` or `getHasItems()`
- **Method naming patterns**: `get/set` (accessor/mutator), `is/has` (boolean), `create/make` (factory)
- **Abbreviations sparingly**: `HttpUrl` not `HTTPUrl`; capitalize acronyms as single letter if short
- **Consistency within codebase**: follow project style; inconsistent naming confuses readers
- **Type variable naming**: single uppercase letter — `<T>` for type, `<E>` for element, `<K, V>` for key/value

### 69. Use Exceptions for Exceptional Conditions
- **Exceptions for truly exceptional control flow**: not for normal operation; don't use as if-statement replacement
- **Anti-pattern: loop termination via exception**: catching `ArrayIndexOutOfBoundsException` to exit loop is wrong
- **Proper loop termination**: check `hasNext()` or use `for-each`; normal control flow is fast
- **Performance difference**: exception path slower (stack unwinding); normal path optimized by JIT
- **Readability**: exception flow harder to follow than explicit condition checks
- **When exceptional**: null return when not found (checked with condition); exception for "impossible" state
- **Exceptions are for errors**: precondition violation, resource exhaustion, constraints broken — use appropriately
- **Reserved for exceptional**: not high-frequency events; use boolean return for common success/failure cases

### 70. Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors
- **Checked exceptions**: caller must handle or declare; signals recoverable condition (e.g., `FileNotFoundException`)
- **Runtime exceptions**: indicate programming error (null dereference, bad argument); caller usually can't recover
- **IOException vs NullPointerException**: IOException checked (file might not exist, try again); NPE unchecked (bug in code)
- **Throw checked if recoverable**: caller can retry, use fallback, or gracefully degrade
- **Throw unchecked if not recoverable**: precondition violated, impossible state; caller shouldn't catch
- **Avoid meaningless catch blocks**: don't catch checked exceptions just to rethrow or log; let propagate if unrecoverable
- **Throwable hierarchy**: `Exception` (checked) and `RuntimeException` (unchecked) are direct children of `Throwable`
- **If unsure, prefer unchecked**: easier to change unchecked → checked later; harder to remove checked requirement

### 71. Avoid Unnecessary Use of Checked Exceptions
- **Checked exception burden**: forces caller to handle with try-catch or declare throws; clutters API
- **Anti-pattern**: wrapping all exceptions in checked exceptions "just in case"; makes API harder to use
- **Cost/benefit analysis**: checked exception only worth cost if caller can meaningfully react
- **Overuse example**: `readFile()` throws `FileNotFoundException`; most callers can't recover, just propagates
- **Better approach**: return empty result, use Optional, or throw unchecked exception instead
- **Single method throwing many checked**: sign that API is poorly designed; consider refactoring
- **Streams avoid checked exceptions**: using checked exceptions with streams (`map()`, `filter()`) is cumbersome
- **Pragmatism**: unchecked exceptions for unlikely errors; checked only when recovery is realistic

### 72. Favor the Use of Standard Exceptions
- **Reuse standard exceptions**: `IllegalArgumentException`, `IllegalStateException`, `NullPointerException`, `IndexOutOfBoundsException`, etc.
- **Benefits of standard exceptions**: familiar to all Java programmers; reduces code duplication; consistent behavior
- **IllegalArgumentException**: parameter value improper but legal type (e.g., negative size when positive required)
- **IllegalStateException**: method called at wrong time (e.g., close already called on stream)
- **NullPointerException**: null argument where null forbidden; consider checking preconditions and throwing explicitly
- **UnsupportedOperationException**: operation not supported (e.g., unmodifiable collection attempting modification)
- **ConcurrentModificationException**: concurrent modification detected during iteration
- **Don't create custom exceptions unless**: you want to catch and handle separately; provide useful context
- **Benefit of standards**: less chance of accidental misuse; well-documented, expected behavior

### 73. Throw Exceptions Appropriate to the Abstraction
- **Exception translation**: catch low-level exceptions and throw high-level exceptions matching API abstraction
- **Example**: `List.get()` throws `IndexOutOfBoundsException`, not `ArrayIndexOutOfBoundsException` (hides array implementation)
- **Caller perspective**: exception should relate to method contract, not internal details
- **Bad example**: method throws `SQLException` when it's a database operation detail; throw domain exception instead
- **Exception chaining**: pass cause to constructor — `new HighLevelException("context", lowLevelException)` preserves stack trace
- **getCause()**: use to get underlying exception; useful for debugging without breaking abstraction
- **Don't suppress**: never catch and ignore exceptions silently; if you catch, handle meaningfully or rethrow
- **Abstraction layers**: each layer translates exceptions to its own level; caller shouldn't know about lower layers

### 74. Document All Exceptions Thrown by Each Method
- **Use @throws Javadoc tag**: document every checked and unchecked exception thrown
- **Unchecked exceptions too**: document `NullPointerException`, `IllegalArgumentException`, etc. in Javadoc
- **Preconditions in docs**: state what must be true for method to succeed; what exceptions if violated
- **Avoid catch-all descriptions**: don't say "may throw Exception"; be specific about each exception type
- **Explain recovery**: mention what caller can do if exception thrown (retry, fallback, etc.)
- **Example format**: `@throws IndexOutOfBoundsException if index is out of range [0, size())`
- **Completeness**: if method overridden, document inherited exceptions plus any new ones
- **Don't document impossible exceptions**: don't document exceptions that can't actually be thrown
- **Tool support**: IDE and javadoc tools use @throws documentation; callers rely on it

### 75. Include Failure-Capture Information in Detail Messages
- **Exception message should aid diagnosis**: include values that contributed to failure; not just "Invalid argument"
- **Bad message**: `throw new IllegalArgumentException("Invalid age");` — doesn't help debug
- **Good message**: `throw new IllegalArgumentException("age must be > 0, was: " + age);` — shows actual value
- **Include bounds**: for range violations, include min, max, and actual value — `"size (" + size + ") must be positive"`
- **Object state matters**: if state affects exception, include relevant state in message
- **Don't expose secrets**: avoid including passwords, API keys, or sensitive data in exception messages
- **Make messages parseable**: avoid vague terms; be specific and measurable
- **Chaining context**: when rethrowing as different exception type, add context to constructor message

### 76. Strive for Failure Atomicity
- **Atomicity on failure**: method should leave object in same state if exception thrown; as if call never happened
- **Validate preconditions first**: check arguments before modifying object state; fail early before changes
- **Example**: `List.add()` should validate bounds before modifying; exception means list unchanged
- **Ordering matters**: perform reads, validation first; mutations and side effects last
- **Copy-modify-swap pattern**: work on copy, swap in if successful; revert to original if error
- **Immutable objects help**: creating immutable object means either fully initialized or not created
- **Thread safety aspect**: atomicity on failure easier to reason about in concurrent scenarios
- **Caveat**: not always achievable; network errors, out-of-memory; document what's guaranteed

### 77. Don't Ignore Exceptions
- **Empty catch blocks are dangerous**: silently swallowing exceptions hides bugs; exception exists for a reason
- **Anti-pattern**: `try { ... } catch (Exception e) { }` — problem disappears from stack trace
- **At minimum, log it**: `catch (Exception e) { logger.error("...", e); }` — preserves evidence
- **If truly ignorable**: document why in comment — "Safe to ignore; cleanup, no side effects"
- **Example**: closing `Closeable` in finally might throw; safe to ignore if already closing
- **Consider alternatives**: return empty result, set default value, or propagate exception upstream
- **Testing**: in unit tests, if exception is expected, use `@Test(expected = ...)` or `assertThrows()`
- **Recovery vs suppression**: if unrecoverable, let propagate; don't hide failure from caller

### 78. Synchronize Access to Shared Mutable Data
- **Race condition if unsynchronized**: multiple threads modifying shared state; reads/writes can interleave; unpredictable
- **synchronized block/method**: ensures mutual exclusion; only one thread at a time in critical section
- **Visibility**: synchronization also ensures visibility of changes across threads (memory barrier)
- **Shared mutable data rule**: every access must be synchronized if any thread modifies; both read and write
- **volatile alternative**: for simple flag flips, `volatile boolean` works; guarantees visibility without lock
- **Atomics for numbers**: `AtomicInteger`, `AtomicLong` for thread-safe counters; lock-free
- **Immutable better than synchronized**: if possible, make objects immutable; no synchronization needed
- **Document synchronization policy**: state which fields are synchronized, which lock protects them

### 79. Avoid Excessive Synchronization
- **Lock contention**: too much synchronization reduces concurrency; threads wait for locks instead of working
- **Holding locks too long**: avoid expensive operations inside synchronized block; release quickly
- **Synchronizing method body**: better to synchronize only critical section with lock object
- **Calls during sync are dangerous**: if synchronized method calls client code, deadlock or corruption risk
- **Alien method problem**: calling external/unknown code while holding lock; can't control what it does
- **Solution**: copy shared data, release lock, then operate on copy; then synchronize and update original
- **CopyOnWriteArrayList example**: immutable copy for reads; synchronize only for writes; good when reads >> writes
- **Performance profile first**: synchronization overhead real; measure contention; don't synchronize "just in case"
- **Immutability again**: if possible, eliminate need for synchronization entirely

### 80. Prefer Executors, Tasks, and Streams to Threads
- **Don't create threads directly**: `new Thread(...)` is low-level; prefer `ExecutorService` (thread pools)
- **ExecutorService benefits**: reuses threads, manages queue, scales workload; avoids thread creation overhead
- **Example**: `Executors.newFixedThreadPool(n)` creates n-thread pool; submit `Runnable` or `Callable` tasks
- **Streams for parallel work**: `stream().parallel()` manages threading; nicer syntax than managing threads
- **ForkJoinPool**: `ExecutorService` subclass for divide-and-conquer; used by `parallelStream()`
- **java.util.concurrent**: provides high-level utilities (`CountDownLatch`, `CyclicBarrier`, etc.); build on these
- **Thread creation expensive**: spawning new thread slower and memory-intensive than reusing pooled threads
- **Complexity hidden**: Executors/Streams hide synchronization, scheduling complexity; less bug-prone
- **Graceful shutdown**: `shutdownNow()`, `awaitTermination()` for clean shutdown; manual threads harder to manage

### 81. Prefer Concurrency Utilities to Wait and Notify
- **Avoid wait()/notify()**: low-level, error-prone, hard to use correctly (spurious wakeups, etc.)
- **java.util.concurrent utilities**: higher-level abstractions for common patterns; less code, fewer bugs
- **CountDownLatch**: one-time synchronization; wait for n threads to finish — `latch.countDown()`, `latch.await()`
- **CyclicBarrier**: reusable barrier; threads wait for all to reach point — parties can repeat
- **Semaphore**: permits-based; acquire/release; useful for resource pools and rate limiting
- **BlockingQueue**: thread-safe queue with blocking put/take; producer-consumer pattern built-in
- **Exchanger**: threads exchange values; synchronizes at meeting point
- **When you must use wait/notify**: document pattern clearly; always use in loop with condition (spurious wakeups)
- **Strategy**: if task fits pattern (latch, barrier, queue, etc.), use that instead of raw wait/notify

### 82. Document Thread Safety
- **Thread safety level must be documented**: callers need to know if safe for concurrent access or single-threaded only
- **Not-thread-safe**: method/class must be externally synchronized; use synchronized block around calls
- **Thread-safe**: method/class safe for concurrent calls; no external synchronization needed
- **Conditionally thread-safe**: some sequences are thread-safe, others require external sync (e.g., `Iterator`)
- **Immutable**: objects immutable are inherently thread-safe; no synchronization needed
- **Effectively immutable**: object not thread-safe, but once published, not modified; can be shared
- **Javadoc @ThreadSafe annotation**: document level clearly; don't assume readers guess correctly
- **Example**: `Collections.synchronizedList()` is thread-safe for all ops; `ArrayList` is not
- **Deadlock documentation**: document if specific lock ordering required to avoid deadlock

### 83. Use Lazy Initialization Judiciously
- **Lazy initialization delays init until first use**: can improve startup time if field rarely used
- **Adds complexity**: synchronization needed if multi-threaded; null checks everywhere; harder to debug
- **Profile first**: don't lazy-init "just in case"; measure startup impact; might be negligible
- **Double-checked locking pitfall**: naive attempt `if (field == null) synchronized { if (field == null) ... }` broken
- **Volatile + double-checked**: `private volatile Field field;` works for primitives/objects; still complex
- **Holder class pattern**: inner class initialized on first access — `return LazyHolder.field;` clean, no sync
- **Supplier/Callable pattern**: wrap in lambda — `field = () -> expensiveInit();` defer until `.get()` called
- **Cost-benefit**: startup cost rarely matters; only optimize if profiling proves it's bottleneck
- **Readability trade-off**: simpler eager init if field always needed; defer complexity only when justified

### 84. Don't Depend on the Thread Scheduler
- **Scheduling is OS-dependent**: JVM doesn't guarantee thread execution order; varies by platform/OS
- **Thread.yield() not reliable**: tells scheduler to pause, but might have no effect; don't use for synchronization
- **Thread.sleep() hackish**: never use sleep to control thread timing; use proper synchronization primitives
- **Busy-wait is wasteful**: spinning loop checking flag wastes CPU; use `wait()`, `await()`, or `BlockingQueue`
- **Portable code assumption**: code working on one system might fail on another due to scheduling differences
- **Priority manipulation weak**: `setPriority()` not reliable across platforms; most code should use normal priority
- **Starvation risk**: high-priority threads can starve others; avoid relying on priority-based execution
- **Robust design**: code should work regardless of scheduler behavior; don't assume thread execution timing
- **Test concurrency thoroughly**: concurrent code often has hidden bugs that appear under different schedules

### 85. Prefer Alternatives to Java Serialization
- **Java serialization dangerous**: arbitrary code execution during deserialization; gadget chains allow RCE
- **Binary format fragile**: hard to evolve; version incompatibility common; forward/backward compatibility hard
- **Performance mediocre**: not particularly fast; produces large byte footprint compared to alternatives
- **Prefer JSON/Protocol Buffers**: human-readable (JSON), language-neutral (Protobuf), security-conscious design
- **JSON libraries**: Jackson, Gson — safe by default; explicit config for features; widely used
- **Protocol Buffers**: binary, efficient, versioning-friendly; schema-based; Google-developed
- **Avoid ObjectInputStream.readObject()**: if you must deserialize, use serial filters (JDK 9+) to allowlist types
- **Immutable objects help**: deserialize to immutable; prevents post-deserialization mutation attacks
- **If forced to use Java serialization**: never deserialize untrusted data; always assume hostile input

### 86. Implement Serializable with Great Caution
- **Once serializable, hard to unsupport**: changing class after serialization breaks compatibility; committed long-term
- **serialVersionUID required**: explicit `serialVersionUID` needed to control versioning; default calculated, fragile
- **Declaring serialVersionUID**: `private static final long serialVersionUID = 1L;` — increment when incompatible change
- **Don't serialize sensitive data**: passwords, API keys, private fields should be transient
- **readObject/writeObject custom logic**: override to handle security, validation, version migration; default unsafe
- **Readiness for deserialization**: `readObject()` can construct partially-initialized objects; validate invariants
- **No-arg constructor bypass**: deserialization bypasses constructors; invariants might be violated
- **Test serialization thoroughly**: serialized form is part of API; test round-tripping; don't ship broken serialization
- **Consider sealed classes**: if Serializable, control which subclasses allowed to prevent gadget chain exploits

### 87. Consider Using a Custom Serialized Form
- **Default serialization mirrors internal structure**: if internal structure changes, serialization breaks
- **Custom form decouples API from implementation**: write cleaner, more stable serialized form than default
- **writeObject/readObject override**: define what gets serialized; restructure at deserialization time
- **Example**: internal `Date[]` array serialized as `int` count + individual fields; safer to evolve internally
- **Transient fields help**: mark internal fields `transient`; write only logical data in `writeObject()`
- **Backward compatibility**: custom form lets you support old serialized objects while changing internals
- **Performance benefit**: custom form can be more compact; omit redundant or reconstructible data
- **Cost**: custom serialization adds complexity; need to handle versioning, validation, migration carefully
- **Worth the effort if**: serialization part of API, long-lived, stability critical; not for internal-only serialization

### 88. Write readObject Methods Defensively
- **readObject() is a constructor**: called to reconstruct object from bytes; must treat input as untrusted
- **Validate invariants immediately**: after deserializing, check object state is valid; throw exception if not
- **Copy mutable fields**: defensive copy of mutable deserialized objects — dates, arrays, collections
- **Check null constraints**: if field should never be null, validate after deserialization
- **Range validation**: numeric fields should be within expected bounds; enum fields should be valid enum values
- **Example**: deserialized date range must be start <= end; field array length must match count
- **Fail fast**: invalid state should cause exception during deserialization, not later during use
- **Partial object risk**: readObject can create object in partially-invalid state; don't trust default initialization
- **Override readObjectNoData()**: if class serialized before added field, handle missing fields safely

### 89. For Instance Control, Prefer Enum Types to readResolve
- **readResolve() for instance control**: return replacement object during deserialization; enforce singletons
- **Singleton pattern problem**: deserialization creates new instance, breaking singleton guarantee
- **readResolve workaround**: `return INSTANCE;` to return singleton instead of deserialized copy
- **Enum better solution**: enums enforce singleton by design; serialization guaranteed to preserve instance
- **Enum guarantees**: only one instance per enum constant, no matter how many times deserialized
- **No readResolve needed**: enum automatically handles serialization correctly; instance guaranteed unique
- **Caveat with readResolve**: reflection can still create new instances; enums prevent this at language level
- **Migration path**: if singleton class becomes serialized, convert to enum if possible; cleaner than readResolve
- **Example**: `enum Singleton { INSTANCE; }` safer and clearer than `class Singleton implements Serializable`

### 90. Consider Serialization Proxies Instead of Serialized Instances
- **Serialization proxy pattern**: serialize lightweight proxy instead of full object; deserialize to original class
- **Proxy advantage**: decouples serialized form from implementation; safer than custom readObject
- **Pattern**: inner `SerializationProxy` class with `writeReplace()`, proxy implements `Serializable`
- **Deserialize via readResolve()**: proxy's `readResolve()` validates and reconstructs original object
- **Invariant protection**: proxy can enforce invariants during reconstruction; impossible to violate state
- **No need for readObject()**: proxy pattern eliminates dangerous `readObject()` entirely
- **Cost**: requires proxy class; slightly more verbose than custom `readObject()` but safer
- **Best for complex objects**: when invariants critical, serialization proxy worth the effort
- **Limitation**: serialization proxies don't work well with classes in inheritance hierarchies; use for leaf classes
