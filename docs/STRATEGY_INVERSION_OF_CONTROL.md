# Strategy: Inversion of Control for Scoop Isolation

**Status:** DRAFT
**Goal:** Remove `ScoopContext` and associated stubs from `hypercell` by inverting the dependency relationship. HyperCell will define interfaces; Scoop (the consumer) will implement them.

---

## 1. The Core Problem

Currently, `hypercell` depends on `scoop.*` classes (even though they are stubs). This is backward. The Calculation Engine (`hypercell`) should not know about the Application Context (`ScoopContext`, Users, Database, S3).

**Current State (Coupled):**
`MathFunction` --> `ScoopContext` --> `CalculatedSourceWorkbook` --> `Database`

**Desired State (Decoupled):**
`MathFunction` --> `DataRefreshInterface` (Interface in HyperCell)
`Scoop App` implements `DataRefreshInterface` --> `ScoopContext` --> `Database`

---

## 2. The Architecture: Plugins & Registries

We will isolate the "Impure" (Context-dependent) logic into two categories:

### A. Proprietary Functions (The "Plugin" Pattern)
Functions like `SCOOPLOOKUP`, `SCOOPAPPLYMODEL`, `SCOOPNEXTCONVERSION` are purely proprietary business logic. They do not belong in the kernel.

*   **Strategy:** Treat them as **User-Defined Functions (UDFs)**.
*   **HyperCell Responsibility:**
    *   Retain the grammar tokens (`SCOOPLOOKUP`, etc.) to support parsing.
    *   In `Compile.java`, instead of hardcoding `new ScoopFunction(...)`, perform a lookup: `registry.getFunction("SCOOPLOOKUP")`.
*   **Scoop App Responsibility:**
    *   Implement the logic currently in `ScoopFunction.java` as a class implementing `com.scoopanalytics.hypercell.api.Function`.
    *   Register this function at startup: `registry.register("SCOOPLOOKUP", new ScoopLookupImplementation(myScoopContext));`
*   **Result:** `ScoopFunction.java` and its ~20 dependent stubs (`scoop.ai.*`, `scoop.metadata.*`, etc.) can be **deleted** from HyperCell.

### B. Side Effects (The "Context" Pattern)
Some standard functions (`SUMIF`, `AVERAGEIF`) have "side effects" in Scoop, specifically triggering a data refresh (`populateIfNecessary`).

*   **Strategy:** Abstract the side effect into an interface.
*   **HyperCell Responsibility:**
    *   Define `com.scoopanalytics.hypercell.api.DataRefresher` interface:
        ```java
        public interface DataRefresher {
            void ensureDataLoaded(String sheetName);
        }
        ```
    *   Add `DataRefresher` to `CompileContext` (and `EvaluationContext`).
    *   Update `MathFunction` to call `cc.getDataRefresher().ensureDataLoaded(...)` instead of `new CalculatedSourceWorkbook(...)`.
*   **Scoop App Responsibility:**
    *   Implement `DataRefresher` using the real `CalculatedSourceWorkbook`.
    *   Pass this implementation when initializing `MemWorkbook`.
*   **Result:** `CalculatedSourceWorkbook.java` and `InputQuery.java` stubs can be **deleted** from HyperCell.

---

## 3. Execution Plan

### Step 1: Define Interfaces (HyperCell API)
1.  Create `com.scoopanalytics.hypercell.api.DataRefresher` (or similar name).
2.  Ensure `FunctionRegistry` is robust enough to handle the complex arguments required by Scoop functions.

### Step 2: Refactor `Compile.java` & `CompileContext`
1.  Remove `ScoopContext` field from `CompileContext`. Replace with `DataRefresher`.
2.  Update `Compile.java` to route `SCOOP*` tokens to `registry.getFunction(...)`.

### Step 3: Update Legacy Functions
1.  **MathFunction:** Replace `CalculatedSourceWorkbook` usage with `DataRefresher` call.
2.  **ScoopFunction:** **DELETE IT.** (Since the logic will live in the consumer app).

### Step 4: Delete Stubs
Once `ScoopFunction` and `CalculatedSourceWorkbook` references are gone, the following packages become unused and can be deleted:
*   `scoop.ai.*`
*   `scoop.connector.*`
*   `scoop.ingest.*`
*   `scoop.metadata.*`
*   `scoop.processanalysis.*`
*   `scoop.reportseriestable.*`
*   `scoop.user.*`
*   `scoop.workspace.*`
*   `scoop.worksheet.CalculatedSourceWorkbook`
*   `scoop.worksheet.InputQuery`

### Step 5: Final Cleanup
1.  **Exceptions:** Define `HyperCellException`. Update `SheetNumber` etc. to throw this instead of `ScoopException`. Delete `ScoopException`.
2.  **Grid Utils:** Move `ExcelDataGrid` (if strictly loading logic) logic to `MemWorkbook` or delete if unused.

---

## 4. The End State

*   **HyperCell:** A pure calculation engine. Contains `SUM`, `IF`, `VLOOKUP`. Zero knowledge of databases, S3, or Users.
*   **Scoop:** A dependency of HyperCell. It injects its specific "Superpowers" (ML, DB Querying) via the Registry and Context interfaces.
