# Plan: Refactoring Legacy Scaffolding

**Status:** DRAFT
**Goal:** Systematically eliminate the hollow `scoop.*` stub classes by refactoring the legacy function code to use HyperCell's abstract interfaces (`EvaluationContext`, `FunctionRegistry`).

---

## 🧠 Context & Philosophy

We currently have ~30 stub files (e.g., `scoop.user.User`, `scoop.metadata.ScoopMetadata`) that exist solely to make the legacy `scoop.expression.*` code compile. They return `null` or empty lists.

This refactoring is **high risk** because it involves modifying the "Black Box" legacy code we just successfully isolated. Therefore, we must proceed with extreme caution, modifying one dependency chain at a time and validating against the test suite at every step.

**The Pattern:**
1.  **Identify** a specific Scoop dependency (e.g., `ScoopContext`).
2.  **Analyze** how it is used in the legacy code (e.g., "It's only used to look up a user ID").
3.  **Abstract** that capability into the `com.scoopanalytics.hypercell` API (e.g., add `getUserId()` to `EvaluationContext`).
4.  **Inject** the abstraction into the legacy code.
5.  **Delete** the specific Scoop dependency import and stub.

---

## 📋 Execution Phase 1: The `ScoopContext` God Object

`ScoopContext` is the root of the dependency tree. Removing it requires untangling everything it touches.

### Step 1.1: Analyze `CompileContext` Usage
**Target:** `scoop.expression.CompileContext`
**Current State:** Holds a reference to `ScoopContext`.
**Plan:**
1.  Modify `scoop.expression.CompileContext` to hold `com.scoopanalytics.hypercell.api.EvaluationContext` instead of `ScoopContext`.
2.  Update `com.scoopanalytics.hypercell.core.expression.Compile` (the bridge) to pass the `EvaluationContext` (which is `MemSheet`) when creating `CompileContext`.
3.  **Compile & Fix:** This will break every function that calls `cc.getSc()`. We will fix them one by one in subsequent steps.

### Step 1.2: Refactor `MathFunction` & `TextualFunction`
These functions likely use `ScoopContext` for simple things like locale or configuration.
1.  **Analyze:** Check usages of `cc.getSc()` in `MathFunction.java`.
2.  **Refactor:** Replace specific calls (e.g., getting a config value) with a call to `EvaluationContext.getConfiguration(key)`.
3.  **Verify:** Run tests.

---

## 📋 Execution Phase 2: Metadata & Database Stubs

These stubs (`ScoopMetadata`, `SavedModel`, `ReportInbox`) are used primarily by `ScoopFunction.java`, which contains proprietary logic we likely cannot support in the open-source kernel.

### Step 2.1: Isolate `ScoopFunction.java`
**Target:** `scoop.expression.ScoopFunction`
**Analysis:** This file contains 90% of the complex dependencies (DB connections, S3 buckets, ML models).
**Plan:**
1.  **Assess Value:** Does `ScoopFunction` provide *any* value in HyperCell without the real Scoop backend?
2.  **Decision:**
    *   *Option A (Purge):* Delete `ScoopFunction.java` entirely. It is proprietary business logic, not general-purpose spreadsheet logic.
    *   *Option B (Interface):* Define a `ProprietaryFunctionHandler` interface in `hypercell-api` and delegate all `ScoopFunction` logic to it.
**Recommendation:** **Option A**. This allows us to delete ~20 stub files immediately (`scoop.ai.*`, `scoop.connector.*`, `scoop.ingest.*`, etc.).

### Step 2.2: Prune the Tree
If Step 2.1 (Delete `ScoopFunction`) is executed:
1.  **Delete** `scoop.expression.ScoopFunction.java`.
2.  **Delete** stubs that are *only* referenced by it:
    *   `scoop.ai.*`
    *   `scoop.connector.*`
    *   `scoop.ingest.*`
    *   `scoop.processanalysis.*`
    *   `scoop.reportseriestable.*`
    *   `scoop.user.*`
    *   `scoop.workspace.*`
    *   `scoop.metadata.*`
3.  **Verify:** Compile and ensure no other functions (like `LookupFunction`) were quietly using these.

---

## 📋 Execution Phase 3: Worksheet & Grid Stubs

These stubs (`CalculatedSourceWorkbook`, `CustomFieldWorkbook`, `InputQuery`) interact with data loading.

### Step 3.1: Refactor `CalculatedSourceWorkbook` Usage
**Target:** `scoop.expression.MathFunction` (references `CalculatedSourceWorkbook`).
**Plan:**
1.  **Analyze:** It uses this to "refresh input queries".
2.  **Abstract:** Move this logic to `EvaluationContext.refreshData(String sheetName)`.
3.  **Refactor:** Update `MathFunction` to call the interface.
4.  **Delete:** `scoop.worksheet.CalculatedSourceWorkbook` and `scoop.worksheet.InputQuery`.

---

## 📋 Execution Phase 4: Utility & Core Stubs

Cleaning up the final, structural stubs.

### Step 4.1: Consolidate Exceptions
**Target:** `scoop.ScoopException`
**Plan:**
1.  Replace all usages of `ScoopException` with `com.scoopanalytics.hypercell.api.exception.HyperCellException` (or standard Java exceptions).
2.  **Delete:** `scoop.ScoopException.java`.

### Step 4.2: Consolidate Grid Structures
**Target:** `scoop.datatable.*`, `scoop.datagrid.*`
**Plan:**
1.  These are likely used by `ExcelDataGrid` (if copied) or `FormatCache`.
2.  If `ExcelDataGrid` is not used by the core engine (it's usually for loading, which `MemWorkbook` now handles), verify if it can be deleted.
3.  If `CellFormat` is used by `TextualFunction` (for `TEXT` function), move it to `com.scoopanalytics.hypercell.core.format` and refactor `TextualFunction` to use the moved class.

---

## 🏁 Success Criteria

1.  **Zero `scoop.*` source files** in `hypercell-core` (except perhaps the functions themselves if we decide to keep the package name for "Legacy" reasons, but they should depend on `com.scoopanalytics.hypercell` interfaces).
2.  **Dependency Injection:** The legacy functions accept `EvaluationContext` and `FunctionRegistry`.
3.  **Tests Pass:** 100% of `CrossValidationTest` passes throughout the process.
