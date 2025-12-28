# Changelog

All notable changes to HyperCell will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-01-01

### Added
- Initial open-source release of HyperCell calculation engine
- Full Excel formula parsing via ANTLR4 grammar
- 200+ Excel function implementations:
  - Math: SUM, SUMIF, SUMIFS, AVERAGE, COUNT, MIN, MAX, ROUND, etc.
  - Statistical: STDEV, VAR, MEDIAN, MODE, PERCENTILE, etc.
  - Logical: IF, IFS, AND, OR, NOT, IFERROR, SWITCH, etc.
  - Text: CONCAT, LEFT, RIGHT, MID, FIND, REPLACE, TEXT, etc.
  - Lookup: VLOOKUP, HLOOKUP, INDEX, MATCH, XLOOKUP, etc.
  - Date/Time: DATE, NOW, TODAY, EOMONTH, DATEDIF, etc.
  - Financial: PMT, PV, FV, NPV, IRR, etc.
  - Information: ISBLANK, ISERROR, ISNUMBER, TYPE, etc.
  - Array: FILTER, SORT, UNIQUE, SEQUENCE
- In-memory workbook representation (MemWorkbook, MemSheet, MemCell)
- Excel file I/O via Apache POI integration
- Spill array support for dynamic array formulas
- Parallel formula calculation
- Extensible function registry for custom functions
- EvaluationContext interface for external data integration

### Architecture
- **hypercell-api**: Public interfaces and contracts
- **hypercell-formula**: ANTLR4 grammar and generated parsers
- **hypercell-core**: Main calculation engine
- **hypercell-functions**: Function implementations (reserved for future use)

### Technical Details
- Java 21 required
- Gradle 8.5 build system
- Apache License 2.0

---

*HyperCell is extracted from the Scoop Analytics platform and released as open source.*
