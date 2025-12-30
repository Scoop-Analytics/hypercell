# Changelog

All notable changes to HyperCell will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- `VALUE()` function now correctly handles currency symbols ($, £, €, ¥, ₹)
  - `VALUE("$1,000")` now returns `1000` instead of `0`
  - Currency symbols are stripped before numeric parsing

### Changed
- Documentation API examples updated to match actual implementation
- Clarified "100% compatibility" claim to note 12 skipped formulas (0.014%)

### Removed
- Legacy zombie directories (470 files, 35,291 lines of dead code)

## [0.1.0] - 2025-01-01

### Added
- Initial open-source release of HyperCell calculation engine
- Full Excel formula parsing via ANTLR4 grammar
- 200+ Excel function implementations:
  - **Math**: SUM, SUMIF, SUMIFS, SUMPRODUCT, AVERAGE, COUNT, MIN, MAX, ROUND, etc.
  - **Statistical**: STDEV, STDEV.S, STDEV.P, VAR, MEDIAN, MODE, PERCENTILE, etc.
  - **Logical**: IF, IFS, AND, OR, NOT, IFERROR, SWITCH, etc.
  - **Text**: CONCAT, LEFT, RIGHT, MID, FIND, REPLACE, TEXT, VALUE, etc.
  - **Lookup**: VLOOKUP, HLOOKUP, INDEX, MATCH, XLOOKUP, OFFSET, etc.
  - **Date/Time**: DATE, NOW, TODAY, EOMONTH, DATEDIF, NETWORKDAYS, etc.
  - **Financial**: PMT, PV, FV, NPV, IRR, XNPV, XIRR, etc.
  - **Information**: ISBLANK, ISERROR, ISNUMBER, TYPE, etc.
  - **Array**: FILTER, SORT, SORTBY, UNIQUE, SEQUENCE, TRANSPOSE
- In-memory workbook representation (MemWorkbook, MemSheet, MemCell)
- Excel file I/O via Apache POI integration
- Spill array support for dynamic array formulas
- DAG-based formula calculation with dependency tracking
- Extensible function registry for custom functions
- EvaluationContext interface for external data integration

### Validation
- Cross-validated against Microsoft Excel
- **82,881 formulas** tested across 9 workbooks
- **100% compatibility** (0 mismatches)

### Architecture
- **hypercell-api**: Public interfaces (EvaluationContext, DataSource, Expression, Function)
- **hypercell-formula**: ANTLR4 grammar and generated parsers
- **hypercell-core**: Main calculation engine (MemWorkbook, MemSheet, MemCell)
- **hypercell-functions**: Function implementations (reserved for future modularization)

### Technical Details
- Java 21 required
- Gradle 8.5 build system
- Apache License 2.0
- Zero proprietary dependencies

---

*HyperCell is extracted from the Scoop Analytics platform and released as open source.*
