# HyperCell Bridge Module

This module provides the integration layer between the open-source HyperCell calculation engine and enterprise platforms like Scoop Analytics.

## Purpose

The bridge module:

1. **Defines extension interfaces** that enterprise systems implement
2. **Provides abstract base classes** with sensible defaults
3. **Contains Scoop-specific adapters** ready for use in Scoop Analytics

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Scoop Analytics                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │             Scoop Implementation                     │   │
│  │  - ScoopContextAdapter extends ScoopEvaluationContext│   │
│  │  - MyScoopQueryRefreshHandler                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           hypercell-bridge (this module)            │   │
│  │  - EnterpriseEvaluationContext                      │   │
│  │  - QueryRefreshHandler                               │   │
│  │  - ExtendedWorkbook                                  │   │
│  │  - ScoopEvaluationContext (abstract)                │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              oss/hypercell-core (open source)               │
│  - MemWorkbook, MemSheet, MemCell                          │
│  - Formula compilation and evaluation                       │
│  - EvaluationContext interface                              │
└─────────────────────────────────────────────────────────────┘
```

## Key Interfaces

### QueryRefreshHandler

Interface for refreshing query-based data sources before calculation:

```java
public interface QueryRefreshHandler {
    boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet);
    boolean isQuerySheet(MemSheet sheet);
    boolean needsRefresh(MemSheet sheet);
}
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

### ExtendedWorkbook

Workbook wrapper that integrates enterprise features:

```java
ExtendedWorkbook workbook = new ExtendedWorkbook(memWorkbook, context);
workbook.setAutoRefreshQueries(true);
workbook.calculate();  // Auto-refreshes queries before calculation
```

## Quick Start (Recommended)

The simplest way to integrate is using callbacks (no subclassing required):

```java
import io.hypercell.bridge.scoop.ScoopCallbacks;
import io.hypercell.bridge.scoop.ScoopIntegration;

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

## Alternative: Subclass-Based Usage

For more control, you can extend the abstract classes:

### Step 1: Implement the Scoop Context Adapter

```java
package scoop.hypercell;

import io.hypercell.bridge.scoop.ScoopEvaluationContext;
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

import io.hypercell.bridge.scoop.ScoopQueryRefreshHandler;
import scoop.ScoopContext;
import scoop.worksheet.CalculatedSourceWorkbook;

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
// In Scoop's calculation code
ScoopContext sc = getScoopContext();
MemWorkbook memWorkbook = loadWorkbook(excelFile);

// Create the bridge
ScoopContextAdapter context = new ScoopContextAdapter(sc);
ExtendedWorkbook workbook = new ExtendedWorkbook(memWorkbook, context);

// Calculate with auto-refresh
workbook.calculate();

// Get results
Object value = workbook.getCellValue("Sheet1", 0, 0);
```

## Dependencies

```gradle
dependencies {
    api 'io.hypercell:hypercell-core:0.1.0'
    api 'io.hypercell:hypercell-api:0.1.0'
}
```

## Not for Open Source Distribution

This module is **not** part of the open-source HyperCell distribution. It contains:

- Enterprise-specific interfaces
- Scoop Analytics integration code
- Features that require proprietary dependencies

The pure open-source version is in the `oss/` directory.
