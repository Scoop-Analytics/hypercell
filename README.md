# HyperCell
**The High-Performance, In-Memory Java Calculation Graph**

> *Turn any Excel workbook into a scalable, in-memory calculation object.*

HyperCell is a specialized Java library designed to solve a specific, painful enterprise problem: **Running complex Excel business logic in high-scale backend environments.**

It is not just a file parser (like Apache POI). It is a **Headless Spreadsheet Engine** that hydrates Excel models into optimized Directed Acyclic Graphs (DAGs) for sub-millisecond calculation.

## 🚀 The Core Proposition: "Excel as Code"

Business analysts model the world in Excel. Engineers rewrite that logic in Java/Python. This translation layer causes bugs, delays, and drift.

**HyperCell changes the workflow:**
1.  **Model**: Analysts build sophisticated logic in Excel (Financial models, Risk raters, Pricing engines).
2.  **Hydrate**: HyperCell loads the workbook into memory, compiling formulas into an executable dependency graph.
3.  **Execute**: The backend injects inputs, triggers calculation, and reads outputs.
4.  **Scale**: Serialize the compiled model and distribute it across thousands of server nodes.

## 📦 Key Features

*   **In-Memory Performance**: Optimized for calculation speed, not file I/O.
*   **Dependency Graph Resolution**: Intelligent recalculation (only updates what changed).
*   **Full Formula Support**: Implements standard Excel library (`SUM`, `VLOOKUP`, `INDEX/MATCH`, `IF`, `NPV`, `IRR`).
*   **Extensible Registry**: Plugin architecture to register custom Java functions callable from Excel (e.g., `=MY_DB_LOOKUP(A1)`).
*   **Serialization**: Native support for freezing compiled models to binary for distributed caching.
*   **Thread-Safe Execution**: Designed for high-concurrency server environments.

## 🛠 Installation

*(Coming soon - Artifacts will be published to Maven Central)*

```groovy
implementation 'io.hypercell:hypercell-core:0.1.0'
```

## 📚 Documentation

*   [**Vision & Philosophy**](docs/VISION.md): Why HyperCell exists and the problem it solves.
*   [**Use Cases**](docs/USE_CASES.md): Detailed examples of HyperCell in production.
*   [**Architecture**](docs/ARCHITECTURE.md): How the calculation graph works under the hood.

## 🤝 Relationship with Scoop

HyperCell is the open-source calculation kernel extracted from [Scoop Analytics](https://scoopanalytics.com). It powers the "Digital Data Analyst" platform, handling millions of formula evaluations for autonomous data investigation.
