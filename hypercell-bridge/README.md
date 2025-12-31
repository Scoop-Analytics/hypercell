# HyperCell Bridge Module

**Integration layer between the open-source HyperCell calculation engine and enterprise platforms.**

This module provides adapters and extension points for integrating HyperCell with enterprise systems like Scoop Analytics, without requiring any changes to the core OSS engine.

## Purpose

The bridge module:

1. **Provides callback-based integration** - No subclassing required
2. **Defines enterprise extension interfaces** - Query refresh, audit logging, permissions
3. **Contains Scoop-specific adapters** - Ready-to-use integration for Scoop Analytics
4. **Maintains clean separation** - The `oss/` core has zero knowledge of enterprise features

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Scoop Analytics                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │             Application Code                         │   │
│  │  - Uses ScoopCallbacks.builder()                    │   │
│  │  - Provides context via lambdas                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           hypercell-bridge (this module)            │   │
│  │  - ScoopCallbacks (builder pattern)                 │   │
│  │  - ScoopIntegration (main entry point)              │   │
│  │  - ExtendedWorkbook (wrapper with enterprise APIs)  │   │
│  │  - EnterpriseEvaluationContext                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              oss/hypercell-core (open source)               │
│  - MemWorkbook, MemSheet, MemCell                          │
│  - Formula compilation and evaluation                       │
│  - EvaluationContext interface                              │
│  - 82,881 formulas validated at 100% Excel compatibility   │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start (Recommended)

The simplest way to integrate is using callbacks (no subclassing required):

```java
import com.scoopanalytics.hypercell.bridge.scoop.ScoopCallbacks;
import com.scoopanalytics.hypercell.bridge.scoop.ScoopIntegration;
import com.scoopanalytics.hypercell.bridge.ExtendedWorkbook;
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;

// Create callbacks with Scoop-specific logic
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .userId(() -> sc.getUserId())
    .tenantId(() -> sc.getOrganizationId())
    .queryRefresher((workbook, sheet) -> {
        CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(sc, null, workbook);
        var queries = csw.getQueries(false, false);
        for (var iq : queries) {
            if (iq.sheetName.startsWith(sheet.getName())) {
                csw.refreshInputQuery(iq);
            }
        }
        sheet.setQuerySheetUpdated(true);
        return true;
    })
    .auditLogger((event, details) -> logger.info("[AUDIT] {} - {}", event, details))
    .permissionChecker(perm -> sc.hasPermission(perm))
    .build();

// Create integration and workbook
ScoopIntegration integration = new ScoopIntegration(callbacks);
ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
workbook.setAutoRefreshQueries(true);
workbook.calculate();

// Get values
Object result = workbook.getCellValue("Sheet1", 0, 0);
```

See [MIGRATION.md](MIGRATION.md) for the complete migration guide.

## Key Interfaces

### ScoopCallbacks

Builder-based callback configuration for Scoop integration:

```java
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .userId(() -> "user123")                    // User ID for audit
    .tenantId(() -> "org456")                   // Tenant/org ID
    .queryRefresher((wb, sheet) -> { ... })     // Query sheet refresh
    .querySheetChecker(sheet -> { ... })        // Check if sheet is query-backed
    .needsRefreshChecker(sheet -> { ... })      // Check if refresh needed
    .auditLogger((event, details) -> { ... })   // Audit event logging
    .permissionChecker(perm -> { ... })         // Permission checks
    .referenceResolver((name, ctx) -> { ... })  // External reference resolver
    .dataSources(() -> { ... })                 // List of data sources
    .dataSourceRefresher(ds -> { ... })         // Refresh a data source
    .build();
```

### ExtendedWorkbook

Workbook wrapper that adds enterprise features:

```java
ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);

// Auto-refresh query sheets before calculation
workbook.setAutoRefreshQueries(true);
workbook.calculate();

// Get cell values
Object value = workbook.getCellValue("Sheet1", 0, 0);

// Cast to specific types as needed
String text = (String) workbook.getCellValue("Results", 5, 2);
Number number = (Number) workbook.getCellValue("Data", 0, 0);

// Set cell values (last param: recalculate after setting)
workbook.setCellValue("Sheet1", 0, 0, 42.5, false);
workbook.setCellValue("Sheet1", 0, 1, "Hello", true);  // recalculate after this

// Access underlying workbook
MemWorkbook underlying = workbook.getWorkbook();
```

### EnterpriseEvaluationContext

Extended evaluation context with enterprise features:

```java
public interface EnterpriseEvaluationContext extends EvaluationContext {
    QueryRefreshHandler getQueryRefreshHandler();
    String getUserId();
    String getTenantId();
    void logAuditEvent(String eventType, String details);
    boolean hasPermission(String permission);
}
```

## Callback Reference

### Recommended Callbacks

| Callback | Purpose | Default |
|----------|---------|---------|
| `userId()` | User ID for audit trails | `null` |
| `tenantId()` | Tenant/org ID for multi-tenancy | `null` |
| `queryRefresher()` | Refresh query-backed sheets | No-op (returns false) |

### Optional Callbacks

| Callback | Purpose | Default |
|----------|---------|---------|
| `dataSources()` | List available data sources | Empty list |
| `dataSourceRefresher()` | Refresh a specific data source | No-op |
| `querySheetChecker()` | Check if sheet is query-backed | Uses `sheet.isQuerySheet()` |
| `needsRefreshChecker()` | Check if refresh is needed | Standard check |
| `auditLogger()` | Log audit events | No-op |
| `permissionChecker()` | Check user permissions | Always returns `true` |
| `referenceResolver()` | Resolve external references | Returns `null` |

## Alternative: Subclass-Based Usage

For more control, you can extend the abstract classes:

### Step 1: Implement the Scoop Context Adapter

```java
package scoop.hypercell;

import com.scoopanalytics.hypercell.bridge.scoop.ScoopEvaluationContext;
import scoop.ScoopContext;

public class ScoopContextAdapter extends ScoopEvaluationContext {
    private final ScoopContext sc;

    public ScoopContextAdapter(ScoopContext sc) {
        this.sc = sc;
        setUserId(sc.getUserId());
        setTenantId(sc.getOrganizationId());
        setQueryRefreshHandler(new ScoopQueryHandler(sc));
    }

    @Override
    protected Object getScoopContext() {
        return sc;
    }

    @Override
    public List<DataSource> getDataSources() {
        return sc.getInputQueries().stream()
            .map(this::convertToDataSource)
            .collect(Collectors.toList());
    }
}
```

### Step 2: Implement Query Refresh Handler

```java
package scoop.hypercell;

import com.scoopanalytics.hypercell.bridge.scoop.ScoopQueryRefreshHandler;
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import com.scoopanalytics.hypercell.core.grid.MemSheet;

public class ScoopQueryHandler extends ScoopQueryRefreshHandler {
    private final ScoopContext sc;

    public ScoopQueryHandler(ScoopContext sc) {
        this.sc = sc;
    }

    @Override
    protected Object getScoopContext() {
        return sc;
    }

    @Override
    public boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet) {
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
}
```

### Step 3: Use in Scoop's Calculation Flow

```java
ScoopContext sc = getScoopContext();
MemWorkbook memWorkbook = loadWorkbook(excelFile);

// Create the bridge
ScoopContextAdapter context = new ScoopContextAdapter(sc);
ExtendedWorkbook workbook = new ExtendedWorkbook(memWorkbook, context);

// Calculate with auto-refresh
workbook.setAutoRefreshQueries(true);
workbook.calculate();

// Get results
Object value = workbook.getCellValue("Sheet1", 0, 0);
```

## Building

```bash
# From project root
./gradlew :hypercell-bridge:build

# Run tests
./gradlew :hypercell-bridge:test
```

## Dependencies

```gradle
dependencies {
    api 'com.scoopanalytics.hypercell:hypercell-core:0.1.0'
    api 'com.scoopanalytics.hypercell:hypercell-api:0.1.0'
}
```

## Test Coverage

The bridge module includes 8 integration tests covering:
- Basic integration workflow
- Query sheet refresh
- Permission checking
- Data source access
- External reference resolution
- Cell value get/set operations
- Default callback behavior
- Multi-sheet workbook handling

## Not for Open Source Distribution

This module is **not** part of the open-source HyperCell distribution. It contains:

- Enterprise-specific interfaces and adapters
- Scoop Analytics integration code
- Features that require proprietary dependencies

The pure open-source version is in the `oss/` directory.

---

*Part of the [HyperCell](../) project by [Scoop Analytics](https://scoopanalytics.com).*
