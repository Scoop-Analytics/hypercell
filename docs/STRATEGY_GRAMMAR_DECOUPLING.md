# Strategy: Decoupling Scoop from the ANTLR Grammar

**Status:** DRAFT
**Goal:** Remove hardcoded `SCOOP*` tokens and rules from `HyperCellExpression.g4` to make the core grammar generic, while preserving the ability for Scoop (and other consumers) to define custom syntax or strict function validation.

---

## The Problem
Currently, `HyperCellExpression.g4` contains specific rules for proprietary functions:
```antlr
scoop:
    SCOOPNEXTCONVERSION '(' expression ',' expression ',' (expression ',' expression)+ ')' | ...
```
This effectively hardcodes the business logic's signature into the kernel's compiler. If we remove these lines, the proprietary formulas will fail to parse unless we provide an alternative path.

---

## Option 1: The "UDF" Approach (Generic Identifiers)
**Recommendation: High**

Treat all proprietary functions as standard "User Defined Functions" using the existing generic function rule.

### The Mechanism
1.  **Grammar Change:** Remove all `SCOOP*` tokens from the Lexer and all `scoop` rules from the Parser.
2.  **Fallback:** The existing rule `genericFunction : IDENTIFIER '(' (expression (',' expression)*)? ')'` will automatically catch `SCOOPNEXTCONVERSION(A, B, C)`.
    *   `SCOOPNEXTCONVERSION` becomes a simple `IDENTIFIER`.
    *   The arguments parse as a standard comma-separated list.
3.  **Runtime Validation:** The specific argument structure (e.g., "must have pairs of arguments") moves from **Compile Time** (Grammar) to **Runtime** (Java Function Implementation).
    *   The registered `ScoopNextConversionFunction` class checks its arguments when `evaluate()` is called and throws a `#VALUE!` error if the structure is invalid.

### Pros
*   **Zero Coupling:** The grammar knows nothing about Scoop.
*   **Simplicity:** No complex factory patterns or grammar inheritance.
*   **Excel-Aligned:** This mimics how Excel handles UDFs/Add-ins (validation happens at execution).

### Cons
*   **Loss of Strictness:** We lose the ability to catch syntax errors (like missing a pair in the conversion list) at parse time. The user won't know until they run the calculation.

---

## Option 2: Parser Injection (Grammar Inheritance)
**Recommendation: Medium**

Allow the consumer (Scoop) to provide its own "Extended Grammar" that inherits from HyperCell's base grammar.

### The Mechanism
1.  **Grammar Inheritance:** Scoop maintains `ScoopExpression.g4` which does `import HyperCellExpression.g4`. It adds the specific `SCOOP*` tokens and rules.
2.  **Factory Pattern:**
    *   HyperCell defines a `ParserFactory` interface.
    *   `Compile.java` stops saying `new HyperCellExpressionParser(...)`. Instead, it asks `cc.getParserFactory().createParser(...)`.
3.  **Injection:** When Scoop initializes `MemWorkbook`, it passes a `ScoopParserFactory`.

### Pros
*   **Strict Validation:** Preserves the current behavior where invalid syntax is caught immediately.
*   **Extensibility:** Allows consumers to add entirely new language constructs, not just functions (e.g., new operators).

### Cons
*   **Complexity:** Requires significant refactoring of `Compile.java`, `MemCell.java`, and `MemWorkbook.java` to thread the `ParserFactory` down the stack.
*   **Fragility:** Grammar inheritance in ANTLR can be tricky to maintain across versions.

---

## Option 3: Post-Processing Validator
**Recommendation: Low**

Parse as generic (Option 1), but run a secondary "Linter" pass.

### The Mechanism
1.  Use Option 1 (Generic parsing).
2.  Register a `SyntaxValidator` in the `FunctionRegistry` alongside the function implementation.
3.  After parsing, `Compile.java` looks up the function. If a validator exists, it runs it against the argument list immediately.

### Pros
*   Retains compile-time validation without polluting the grammar.

### Cons
*   Requires building a new "Validator" API and infrastructure. Overkill for this problem.

---

## 🏆 Recommendation: Option 1 (UDF Approach)

We should embrace the **UDF Approach**. It is the most robust and maintainable solution for a calculation kernel.

**Reasoning:**
1.  **Separation of Concerns:** The Grammar defines the *language syntax* (Excel formulas), not the *standard library*. `SCOOPNEXTCONVERSION` is part of the library, not the language.
2.  **Robustness:** Moving validation to Java is easier to test and debug than validation in ANTLR rules.
3.  **Speed:** It drastically simplifies the migration. We just delete the lines from the `.g4` file and ensure the Java implementation checks its args.

### Implementation Plan
1.  **Delete** `SCOOP*` rules from `HyperCellExpression.g4`.
2.  **Delete** `ScoopFunction.java` (as planned in the Stubs cleanup).
3.  **Verify** that `SCOOPLOOKUP(...)` tests still pass (because they now route through `genericFunction` -> `FunctionRegistry` -> Stub/Plugin).
