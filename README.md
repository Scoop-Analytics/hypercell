# HyperCell
**The High-Performance, In-Memory Java Calculation Graph**

> *Turn any Excel workbook into a scalable, in-memory calculation object.*

HyperCell is a specialized Java library designed to solve a specific, painful enterprise problem: **Running complex Excel business logic in high-scale backend environments.**

It is not just a file parser (like Apache POI). It is a **Headless Spreadsheet Engine** that hydrates Excel models into optimized Directed Acyclic Graphs (DAGs) for sub-millisecond calculation.

## Project Structure

This repository contains two levels:

### `oss/` - Open Source Distribution
The `oss/` directory contains the **pure, self-contained open-source version** of HyperCell:
- Zero proprietary dependencies
- Ready for GitHub publication
- Complete Excel formula calculation engine
- See [`oss/README.md`](oss/README.md) for usage documentation

### Parent Level - Scoop Integration
The parent directory contains:
- Integration code for Scoop Analytics platform
- Extended functionality and enterprise features
- Migration documentation and verification scripts
- Development tools and testing infrastructure

## Quick Start (Open Source)

```bash
cd oss/
./gradlew clean build
```

See [`oss/README.md`](oss/README.md) for complete documentation.

## The Core Proposition: "Excel as Code"

Business analysts model the world in Excel. Engineers rewrite that logic in Java/Python. This translation layer causes bugs, delays, and drift.

**HyperCell changes the workflow:**
1.  **Model**: Analysts build sophisticated logic in Excel (Financial models, Risk raters, Pricing engines).
2.  **Hydrate**: HyperCell loads the workbook into memory, compiling formulas into an executable dependency graph.
3.  **Execute**: The backend injects inputs, triggers calculation, and reads outputs.
4.  **Scale**: Serialize the compiled model and distribute it across thousands of server nodes.

## Key Features

*   **In-Memory Performance**: Optimized for calculation speed, not file I/O.
*   **Dependency Graph Resolution**: Intelligent recalculation (only updates what changed).
*   **Full Formula Support**: Implements 200+ Excel functions (`SUM`, `VLOOKUP`, `INDEX/MATCH`, `IF`, `NPV`, `IRR`).
*   **Extensible Registry**: Plugin architecture to register custom Java functions callable from Excel.
*   **Serialization**: Native support for freezing compiled models to binary for distributed caching.
*   **Thread-Safe Execution**: Designed for high-concurrency server environments.

## Installation

```groovy
// Gradle
implementation 'io.hypercell:hypercell-core:0.1.0'
```

```xml
<!-- Maven -->
<dependency>
    <groupId>io.hypercell</groupId>
    <artifactId>hypercell-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Documentation

*   [**Vision & Philosophy**](docs/VISION.md): Why HyperCell exists and the problem it solves.
*   [**Use Cases**](docs/USE_CASES.md): Detailed examples of HyperCell in production.
*   [**Architecture**](docs/ARCHITECTURE.md): How the calculation graph works under the hood.
*   [**OSS README**](oss/README.md): Complete open-source documentation.

## Relationship with Scoop

HyperCell is the open-source calculation kernel extracted from [Scoop Analytics](https://scoopanalytics.com). It powers the "Digital Data Analyst" platform, handling millions of formula evaluations for autonomous data investigation.

For details on the extraction process and verification, see [**Migration Success Report**](MIGRATION_SUCCESS.md).

## License

Apache License 2.0 - See [LICENSE](oss/LICENSE) for details.

---

*Developed by [Scoop Analytics](https://scoopanalytics.com)*
