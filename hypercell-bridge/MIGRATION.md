# Scoop Migration Guide

This guide explains how to migrate Scoop Analytics from the legacy tightly-coupled HyperCell integration to the new bridge-based architecture.

## Overview

The new architecture cleanly separates concerns:

| Layer | Purpose | Dependencies |
|-------|---------|--------------|
| `oss/` | Pure open-source HyperCell engine | None (self-contained) |
| `hypercell-bridge/` | Integration adapters for Scoop | Depends on `oss/` |
| Scoop Application | Business logic with HyperCell | Uses `hypercell-bridge/` |

## Migration at a Glance

```
BEFORE (Tight Coupling)              AFTER (Bridge-Based)
┌─────────────────────┐              ┌─────────────────────┐
│  Scoop Application  │              │  Scoop Application  │
│  ┌───────────────┐  │              │  ┌───────────────┐  │
│  │  MemWorkbook  │  │              │  │ ScoopCallbacks│  │
│  │   (with sc)   │  │    ──────>   │  │   .builder()  │  │
│  └───────────────┘  │              │  └───────────────┘  │
│  workbook.          │              │          ▼          │
│   setScoopContext() │              │  ┌───────────────┐  │
└─────────────────────┘              │  │ScoopIntegration│  │
                                     │  └───────────────┘  │
                                     │          ▼          │
                                     │  ┌───────────────┐  │
                                     │  │ExtendedWorkbook│  │
                                     │  └───────────────┘  │
                                     └─────────────────────┘
```

## Quick Start

### Step 1: Add Dependency

In Scoop's `build.gradle`:

```gradle
dependencies {
    implementation 'com.scoopanalytics.hypercell:hypercell-bridge:0.1.0-SNAPSHOT'
}
```

### Step 2: Create Integration Instance

Replace direct usage of `MemWorkbook` with the bridge integration:

```java
import com.scoopanalytics.hypercell.bridge.scoop.ScoopCallbacks;
import com.scoopanalytics.hypercell.bridge.scoop.ScoopIntegration;
import com.scoopanalytics.hypercell.bridge.ExtendedWorkbook;

public class HyperCellIntegration {
    private final ScoopContext sc;
    private final ScoopIntegration integration;

    public HyperCellIntegration(ScoopContext sc) {
        this.sc = sc;
        this.integration = createIntegration();
    }

    private ScoopIntegration createIntegration() {
        ScoopCallbacks callbacks = ScoopCallbacks.builder()
            .userId(() -> sc.getUserId())
            .tenantId(() -> sc.getOrganizationId())
            .dataSources(() -> convertInputQueries(sc.getInputQueries()))
            .queryRefresher(this::refreshQuerySheet)
            .auditLogger((event, details) ->
                logger.info("[AUDIT] {} - {}", event, details))
            .permissionChecker(perm -> sc.hasPermission(perm))
            .build();

        return new ScoopIntegration(callbacks);
    }

    private boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet) {
        CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(sc, null, workbook);
        var queries = csw.getQueries(false, false);
        for (var iq : queries) {
            if (iq.sheetName.startsWith(sheet.getName())) {
                csw.refreshInputQuery(iq);
            }
        }
        sheet.setQuerySheetUpdated(true);
        return true;
    }

    public void calculate(MemWorkbook memWorkbook) {
        ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
        workbook.setAutoRefreshQueries(true);
        workbook.calculate();
    }
}
```

## Migration Mapping

### Before (Legacy)

```java
// Direct usage with Scoop coupling
MemWorkbook workbook = new MemWorkbook(file, poiWorkbook, true);
workbook.setScoopContext(sc);  // Tight coupling!
workbook.calculate();
```

### After (Bridge-Based)

```java
// Decoupled via bridge callbacks
MemWorkbook workbook = new MemWorkbook(file, poiWorkbook, true);

ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .userId(() -> sc.getUserId())
    .queryRefresher(this::refreshQuerySheet)
    .build();

ScoopIntegration integration = new ScoopIntegration(callbacks);
ExtendedWorkbook extWorkbook = integration.createWorkbook(workbook);
extWorkbook.calculate();
```

## Callback Reference

### Required Callbacks

**None** - all callbacks have sensible defaults.

### Recommended Callbacks

| Callback | Purpose | Default |
|----------|---------|---------|
| `userId()` | User ID for audit | `null` |
| `tenantId()` | Tenant/org ID | `null` |
| `queryRefresher()` | Refresh query sheets | No-op (returns false) |

### Optional Callbacks

| Callback | Purpose | Default |
|----------|---------|---------|
| `dataSources()` | List of data sources | Empty list |
| `dataSourceRefresher()` | Refresh a data source | No-op |
| `querySheetChecker()` | Check if sheet is query | Uses `sheet.isQuerySheet()` |
| `needsRefreshChecker()` | Check if refresh needed | Standard check |
| `auditLogger()` | Log audit events | No-op |
| `permissionChecker()` | Check permissions | Always returns `true` |
| `referenceResolver()` | Resolve external refs | Returns `null` |

## Key Changes

### 1. No More Direct Context Access

**Before:**
```java
workbook.getScoopContext().getUserId();
```

**After:**
```java
integration.getContext().getUserId();
```

### 2. Query Refresh is Callback-Based

**Before:**
```java
// MemWorkbook internally called Scoop's query refresh
workbook.calculate();  // Implicitly refreshed queries
```

**After:**
```java
// You provide the refresh logic via callback
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .queryRefresher((wb, sheet) -> {
        // Your Scoop-specific refresh logic
        CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(sc, null, wb);
        // ... refresh queries ...
        return true;
    })
    .build();
```

### 3. Evaluation Context is External

**Before:**
```java
// Context was internal to MemWorkbook
public class MemWorkbook {
    private ScoopContext sc;  // Tight coupling
}
```

**After:**
```java
// Context is provided externally
ScoopIntegration integration = new ScoopIntegration(callbacks);
EvaluationContext context = integration.getContext();
```

### 4. Cell Value Access via ExtendedWorkbook

**Before:**
```java
MemCell cell = workbook.getSheet("Sheet1").getCellAt(0, 0);
Object value = cell.getValue();
```

**After:**
```java
// Option 1: Via ExtendedWorkbook (recommended)
Object value = extWorkbook.getCellValue("Sheet1", 0, 0);

// Option 2: Access underlying workbook
MemWorkbook underlying = extWorkbook.getWorkbook();
MemCell cell = underlying.getSheet("Sheet1").getCellAt(0, 0);
```

## Testing the Migration

```java
@Test
void testMigration() {
    // Load workbook
    MemWorkbook memWorkbook = new MemWorkbook(file, poiWorkbook, true);

    // Create integration with test callbacks
    ScoopCallbacks callbacks = ScoopCallbacks.builder()
        .userId(() -> "testUser")
        .tenantId(() -> "testOrg")
        .build();

    ScoopIntegration integration = new ScoopIntegration(callbacks);
    ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);

    // Calculate
    workbook.calculate();

    // Verify results
    Object value = workbook.getCellValue("Sheet1", 0, 0);
    assertEquals(expectedValue, value);
}
```

## Troubleshooting

### Query sheets not refreshing

Ensure you've provided a `queryRefresher` callback and enabled auto-refresh:

```java
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .queryRefresher((wb, sheet) -> {
        // Your refresh logic here
        return true;  // Return true on success
    })
    .build();

ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
workbook.setAutoRefreshQueries(true);  // Don't forget this!
workbook.calculate();
```

### User/tenant context is null

Provide the callbacks:

```java
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .userId(() -> sc.getUserId())
    .tenantId(() -> sc.getOrganizationId())
    .build();
```

### Formula results are wrong

Ensure the workbook is properly loaded with formulas compiled:

```java
// The third parameter (true) enables formula loading
MemWorkbook workbook = new MemWorkbook(file, poiWorkbook, true);
workbook.calculate();  // Compiles and evaluates
```

### Permission checks always pass

Provide a permission checker:

```java
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .permissionChecker(perm -> sc.hasPermission(perm))
    .build();
```

### External references not resolving

Provide a reference resolver:

```java
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .referenceResolver((name, context) -> {
        // Look up external reference by name
        return externalDataStore.get(name);
    })
    .build();
```

## Validation

After migration, verify using cross-validation tests:

```bash
./gradlew :hypercell-bridge:test
```

All 8 integration tests should pass:
- Basic integration workflow
- Query sheet refresh
- Permission checking
- Data source access
- External reference resolution
- Cell value operations
- Default callbacks
- Multi-sheet workbooks

## Support

For issues, see:
- Bridge tests: `hypercell-bridge/src/test/java/io/hypercell/bridge/scoop/ScoopIntegrationTest.java`
- README: [hypercell-bridge/README.md](README.md)

---

*Part of the [HyperCell](../) project by [Scoop Analytics](https://scoopanalytics.com).*
