# Stub Cleanup Status
*Tracking the removal of the `scoop.*` package from `hypercell-core`*

## Remaining Tasks

### 1. Refactor `MathFunction.java`
**Location:** `hypercell-core/src/main/java/io/hypercell/core/expression/MathFunction.java`
**Action:**
*   Remove `populateIfNecessary(MemCell mc)` method.
*   Replace usage of `CalculatedSourceWorkbook` with `cc.getEvaluationContext().refreshDataSource(...)`.
*   Note: This requires `CompileContext` to hold the `EvaluationContext`.

### 2. Update `CompileContext`
**Location:** `hypercell-core/src/main/java/io/hypercell/core/expression/CompileContext.java`
**Action:**
*   Add `EvaluationContext` field.
*   Update constructor to accept it.

### 3. The Purge
Once `MathFunction` is clean, the following directories can be deleted:
*   `hypercell-core/src/main/java/scoop/` (Recursive delete)
*   **Target Files:** `ScoopContext`, `ScoopMetadata`, `Worksheet`, `CalculatedSourceWorkbook`, `InputQuery`.

### 4. Grammar Cleanup
**Location:** `hypercell-formula/src/main/antlr/io/hypercell/formula/HyperCellExpression.g4`
**Action:**
*   Remove `SCOOP` token definitions.
*   Remove `SCOOP` specific parser rules.
