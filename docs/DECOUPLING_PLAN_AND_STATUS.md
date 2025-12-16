# HyperCell Decoupling Strategy & Status
*Last Updated: December 16, 2025*

## 🎯 Objective
To transform `hypercell-core` from a "Scoop-aware" library into a hermetic, generic calculation engine. This is achieved by inverting dependencies: instead of HyperCell importing `scoop.*` classes, it defines interfaces that Scoop must implement.

---

## 🏗️ Architectural Changes

### 1. The Core Contract: `EvaluationContext`
We have introduced `io.hypercell.api.EvaluationContext` to replace the "God Object" `scoop.ScoopContext`.

**Old World (Coupled):**
```java
// HyperCell code directly depended on Scoop
public void calculate(ScoopContext sc) {
    String data = sc.getDataFromDB(...); 
}
```

**New World (Decoupled):**
```java
// HyperCell defines what it needs
public interface EvaluationContext {
    Object resolveReference(String sheet, int row, int col);
    void refreshDataSource(DataSource ds);
}

// Scoop implements the contract
public class ScoopBridge implements EvaluationContext { ... }
```

### 2. Data Loading Abstraction
We replaced the legacy `scoop.worksheet.InputQuery` (a stub) with a clean record `io.hypercell.api.DataSource`.
*   **Old:** `CalculatedSourceWorkbook` (stub) was instantiated to "refresh" data.
*   **New:** `EvaluationContext.refreshDataSource(DataSource ds)` is called to trigger external data loading.

### 3. Utility Consolidation
We centralized Excel format validation logic.
*   **Deleted:** `io.hypercell.core.datagrid.ExcelDataGrid` (Legacy code that depended on ScoopContext).
*   **Deleted:** `io.hypercell.core.grid.FormattingUtils` (Redundant).
*   **Created:** `io.hypercell.core.util.FormattingUtils` (The single source of truth, 100% dependency-free).

---

## ✅ Status Report

| Component | Status | Details |
| :--- | :--- | :--- |
| **MemWorkbook** | ✅ **Done** | Constructor and `calculate()` now use `EvaluationContext`. |
| **MemCell** | ✅ **Done** | Removed unused `ScoopContext` from `compileFormula()`. |
| **ExcelDataGrid** | ✅ **Gone** | Deleted legacy class. |
| **FormattingUtils** | ✅ **Done** | Consolidated logic into `io.hypercell.core.util`. |
| **MathFunction** | ⚠️ **Pending** | Still contains `populateIfNecessary` which calls legacy stubs. |
| **Scoop Package** | ⚠️ **Pending** | `hypercell-core/src/main/java/scoop/` still exists. |

---

## 📋 Integration Guide for Scoop Team

**Warning:** Do not attempt to upgrade Scoop to use this version of HyperCell until the following steps are prepared.

### 1. Implement `EvaluationContext`
In the Scoop codebase (`scoop/app/src/main/java/scoop/hypercell/bridge/`), create an adapter:

```java
public class ScoopEvaluationContext implements io.hypercell.api.EvaluationContext {
    private final ScoopContext sc;

    public ScoopEvaluationContext(ScoopContext sc) {
        this.sc = sc;
    }

    @Override
    public Object resolveReference(String sheet, int row, int col) {
        // Delegate to MemSheet lookup logic
        return ...;
    }

    @Override
    public void refreshDataSource(DataSource ds) {
        // Map DataSource.sheetName to InputQuery
        // Call CalculatedSourceWorkbook.refreshInputQuery(...)
    }
}
```

### 2. Update Workbook Creation
When initializing a calculation, pass the bridge instead of the context:

```java
// Old
MemWorkbook wb = new MemWorkbook(scoopContext, name, poiWorkbook, true);

// New
EvaluationContext bridge = new ScoopEvaluationContext(scoopContext);
MemWorkbook wb = new MemWorkbook(bridge, name, poiWorkbook, true);
```

### 3. Register Custom Functions
HyperCell no longer "knows" about `SCOOPLOOKUP` or `SCOOP_AI`. You must register them explicitly:

```java
FunctionRegistry registry = wb.getRegistry();
registry.register("SCOOPLOOKUP", new ScoopLookupFunctionHandler());
```
