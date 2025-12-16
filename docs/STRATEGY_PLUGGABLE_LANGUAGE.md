# Strategy: Pluggable Language Architecture (Option 2)

**Status:** IMPLEMENTED (December 2, 2025)
**Goal:** Enable consumers (like Scoop) to extend the HyperCell language definition (`.g4`) while reusing the core grid infrastructure. This supports strict syntax validation and custom language constructs.

---

## 1. The Architecture

We will move from a **Hardcoded Pipeline** to a **Pluggable Pipeline**.

### Current (Hardcoded)
`MemWorkbook` -> `new Compile(...)` -> `new HyperCellExpressionParser(...)` -> `Hardcoded AST Traversal`

### Future (Pluggable)
`MemWorkbook` -> `LanguagePipeline` Interface -> `Injected Implementation`

The `LanguagePipeline` encapsulates the grammar and the logic to translate that grammar into HyperCell Expressions.

---

## 2. New Core Interfaces

We introduce these interfaces in `hypercell-api` or `hypercell-core`:

### `LanguagePipeline`
The entry point for compiling a formula string.
```java
public interface LanguagePipeline {
    // Converts a raw formula string into an executable Expression
    Expression compile(String formula, EvaluationContext context);
}
```

### `ParserFactory` (Optional but recommended)
Abstracts the creation of the ANTLR parser.
```java
public interface ParserFactory {
    ParseTree parse(String formula); // Returns the generic ANTLR ParseTree
}
```

---

## 3. The Default Implementation (HyperCell Standard)

HyperCell provides the "Standard Excel" implementation out of the box.

1.  **Grammar:** `HyperCellExpression.g4` (The standard we have now).
2.  **Compiler:** `StandardCompiler.java` (Refactored from current `Compile.java`).
    *   It is tightly coupled to `HyperCellExpressionParser` classes.
    *   It handles `SUM`, `IF`, `VLOOKUP`.
3.  **Pipeline:** `StandardLanguagePipeline` wires them together.

---

## 4. The Developer Experience (How Scoop uses it)

To add proprietary syntax (e.g., `SCOOPLOOKUP` with strict argument pairs), the developer does the following:

### Step A: Define Custom Grammar
Create `ScoopExpression.g4` which imports `HyperCellExpression.g4`.
```antlr
grammar ScoopExpression;
import HyperCellExpression;

// Custom Rule with strict syntax
scoopLookup : SCOOPLOOKUP '(' val ',' dataset ')' ;

// Override expression to include custom rule
expression : ... | scoopLookup | ... ;
```

### Step B: Implement Custom Compiler
Because `ScoopExpressionParser` generates unique AST classes (e.g., `ScoopExpressionParser.ScoopLookupContext`), the developer must write a compiler to handle them.

To avoid rewriting the *entire* compiler, we recommend a **Delegation Pattern**:

```java
public class ScoopCompiler implements LanguagePipeline {
    private final StandardCompiler baseCompiler;

    public Expression compile(String formula, EvaluationContext ctx) {
        // 1. Parse with Custom Parser
        ScoopExpressionParser parser = createScoopParser(formula);
        ParseTree tree = parser.start();

        // 2. Traverse
        return visit(tree, ctx);
    }

    private Expression visit(ParseTree tree, EvaluationContext ctx) {
        // Handle Custom Nodes
        if (tree instanceof ScoopExpressionParser.ScoopLookupContext) {
            return new ScoopLookupFunction(tree, ctx);
        }
        
        // Delegate Standard Nodes to Base Compiler?
        // *Challenge:* Base Compiler expects HyperCellParser nodes, not ScoopParser nodes.
        // *Solution:* The Custom Compiler must handle the "Structure" of standard nodes 
        // or we use Option 1 (Generic) for the standard parts.
        
        // PRACTICAL REALITY CHECK: 
        // ANTLR inheritance is messy for code reuse. 
        // It is often cleaner for ScoopCompiler to be a full fork or 
        // for the Scoop grammar to be the *Primary* grammar used by the app.
    }
}
```

### **Wait, there is a cleaner way for Step B:**
If `ScoopExpression.g4` is the **Superset**, Scoop can simply **Copy & Extend** the `Compile.java` logic. Since `Compile.java` is just a giant switch statement, "Extending" it essentially means copying it and adding the new `case` statements for the new AST nodes. This is acceptable for a "Heavy" extension like a full product platform.

---

## 5. Migration Plan (to Option 2)

1.  **Refactor `Compile.java`:** Extract the parsing logic (ANTLR setup) from the AST traversal logic.
2.  **Create `LanguagePipeline` Interface:** Make `MemWorkbook` accept this interface in its constructor.
3.  **Default Wiring:** `MemWorkbook` defaults to `StandardLanguagePipeline`.
4.  **Scoop Implementation:**
    *   In the `scoop` package (legacy), create `ScoopLanguagePipeline`.
    *   It uses the `ScoopExpressionParser` (already generated).
    *   It uses a `ScoopCompiler` (clone of `Compile.java` adapted to `ScoopExpressionParser` classes).
5.  **Inject:** When `MemWorkbook` is created in the test/app, pass `new ScoopLanguagePipeline()`.

## Pros & Cons of Option 2

**Pros:**
*   **Strict Contracts:** The grammar enforces `SCOOPNEXTCONVERSION` takes exactly X arguments.
*   **Clean Core:** `hypercell-core` grammar has zero Scoop tokens.
*   **Power:** Developers can fundamentally alter the language (e.g., add SQL syntax support).

**Cons:**
*   **Code Duplication:** The `ScoopCompiler` will look 90% identical to `StandardCompiler`, just referencing different (but identical-looking) AST classes.
*   **Maintenance:** Updates to the core logic (e.g., fixing `SUM`) need to be ported to `ScoopCompiler`.

**Mitigation for Duplication:**
We can use **Java Generics / Interfaces** for the Compiler if we abstract the Parse Tree nodes, but that is significant engineering effort. For now, **Forking the Compiler** (Copy/Paste/Extend) is the pragmatic way to achieve strict Grammar Inheritance.
