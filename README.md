# HyperCell

**The High-Performance, In-Memory Excel Calculation Engine for Java**

[![Build Status](https://github.com/Scoop-Analytics/hypercell/actions/workflows/ci.yml/badge.svg)](https://github.com/Scoop-Analytics/hypercell/actions)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

> **Status**: Ready for v0.1.0 release. Validated with 82,881 formulas at 100% Excel compatibility.
> See [AUDIT_STATUS.md](AUDIT_STATUS.md) for audit details.

HyperCell transforms Excel workbooks into high-performance, in-memory calculation graphs. It's not just a file parser - it's a **headless spreadsheet engine** that compiles Excel formulas into optimized Directed Acyclic Graphs (DAGs) for sub-millisecond calculation.

## Why HyperCell?

Business analysts model the world in Excel. Engineers rewrite that logic in Java/Python. This translation causes bugs, delays, and drift.

**HyperCell changes the workflow:**

1. **Model** - Analysts build sophisticated logic in Excel (financial models, risk raters, pricing engines)
2. **Hydrate** - HyperCell loads the workbook, compiling formulas into an executable dependency graph
3. **Execute** - Your backend injects inputs, triggers calculation, and reads outputs
4. **Scale** - Serialize the compiled model and distribute across thousands of server nodes

## Project Structure

```
hypercell/
├── oss/                    # Open-source distribution (zero proprietary deps)
│   ├── hypercell-api/      # Public interfaces
│   ├── hypercell-formula/  # ANTLR4 Excel grammar
│   ├── hypercell-core/     # Calculation engine
│   └── hypercell-functions/# 200+ Excel functions
│
├── hypercell-bridge/       # Enterprise integration layer
│   └── scoop/              # Scoop Analytics adapters
│
└── docs/                   # Architecture documentation
```

| Directory | Purpose | Dependencies |
|-----------|---------|--------------|
| `oss/` | Pure open-source engine | None (self-contained) |
| `hypercell-bridge/` | Enterprise integration | Depends on `oss/` |

## Quick Start

### Open Source Usage

```bash
cd oss/
./gradlew clean build
```

```java
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import com.scoopanalytics.hypercell.core.grid.MemSheet;
import com.scoopanalytics.hypercell.core.grid.MemCell;

// Load Excel file
MemWorkbook workbook = new MemWorkbook("model.xlsx", poiWorkbook, true);

// Calculate all formulas
workbook.calculate();

// Read results
MemSheet sheet = workbook.getSheet("Results");
double value = sheet.getCellAt(0, 0).getNumberValue().doubleValue();
```

### Enterprise Integration (Scoop)

```java
import com.scoopanalytics.hypercell.bridge.scoop.ScoopCallbacks;
import com.scoopanalytics.hypercell.bridge.scoop.ScoopIntegration;
import com.scoopanalytics.hypercell.bridge.ExtendedWorkbook;

// Configure callbacks (no subclassing required)
ScoopCallbacks callbacks = ScoopCallbacks.builder()
    .userId(() -> context.getUserId())
    .tenantId(() -> context.getOrganizationId())
    .queryRefresher((workbook, sheet) -> refreshFromDatabase(workbook, sheet))
    .auditLogger((event, details) -> logger.info("[AUDIT] {}: {}", event, details))
    .build();

// Create integration
ScoopIntegration integration = new ScoopIntegration(callbacks);
ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);

// Calculate with auto-refresh
workbook.setAutoRefreshQueries(true);
workbook.calculate();
```

## Key Features

| Feature | Description |
|---------|-------------|
| **200+ Excel Functions** | SUM, VLOOKUP, INDEX/MATCH, IF, NPV, IRR, and more |
| **In-Memory Performance** | Optimized DAG for sub-millisecond calculation |
| **Dependency Resolution** | Intelligent recalculation - only updates what changed |
| **Spill Arrays** | Dynamic array formulas that auto-expand |
| **Thread-Safe** | Designed for high-concurrency server environments |
| **Extensible** | Plugin architecture for custom functions |
| **Serializable** | Freeze compiled models for distributed caching |

## Validation

Cross-validated against Excel with **82,881 formulas** across 9 workbooks:

```
═══════════════════════════════════════════════════════════
         HYPERCELL CROSS-VALIDATION SUMMARY
═══════════════════════════════════════════════════════════
Workbooks tested:     9
Total sheets:         64
Formulas validated:   82881
Formulas skipped:     12 (unsupported functions)
Mismatches found:     0

✅ SUCCESS: 100% of validated formulas match Excel
═══════════════════════════════════════════════════════════
```

*12 formulas (0.014%) skipped due to unsupported functions.*

## Installation

JARs are published to [GitHub Packages](https://github.com/Scoop-Analytics/hypercell/packages).

### Gradle

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/Scoop-Analytics/hypercell")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Open source core
    implementation 'com.scoopanalytics.hypercell:hypercell-core:0.1.0'

    // Enterprise bridge (optional)
    implementation 'com.scoopanalytics.hypercell:hypercell-bridge:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Scoop-Analytics/hypercell</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.scoopanalytics.hypercell</groupId>
    <artifactId>hypercell-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

Configure authentication in `~/.m2/settings.xml`:
```xml
<server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
</server>
```

## Documentation

| Document | Description |
|----------|-------------|
| [OSS README](oss/README.md) | Open-source usage and API reference |
| [Bridge README](hypercell-bridge/README.md) | Enterprise integration guide |
| [Migration Guide](hypercell-bridge/MIGRATION.md) | Migrating from legacy Scoop integration |
| [Architecture](docs/ARCHITECTURE.md) | How the calculation graph works |
| [Vision](docs/VISION.md) | Why HyperCell exists |

## Requirements

- Java 21 or higher
- Gradle 8.5 or higher (for building)

## Building

```bash
# Build everything
./gradlew clean build

# Run all tests
./gradlew test

# Build only OSS
cd oss && ./gradlew clean build

# Run cross-validation
./gradlew :oss:hypercell-core:test --tests "*CrossValidationTest*"
```

## Relationship with Scoop

HyperCell is the open-source calculation kernel extracted from [Scoop Analytics](https://scoopanalytics.com). It powers the "Digital Data Analyst" platform, handling millions of formula evaluations for autonomous data investigation.

The extraction ensures:
- **Clean separation** - `oss/` has zero Scoop references
- **Easy integration** - `hypercell-bridge/` provides ready-to-use adapters
- **Full compatibility** - 100% calculation parity with the original engine

## License

Apache License 2.0 - See [LICENSE](oss/LICENSE) for details.

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Run tests: `./gradlew clean build`
4. Submit a pull request

---

*Developed by [Scoop Analytics](https://scoopanalytics.com)*
