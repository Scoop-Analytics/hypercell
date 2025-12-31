# HyperCell (Open Source)

**A high-performance, in-memory Excel calculation engine for Java.**

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

HyperCell evaluates Excel formulas with full compatibility, supporting 200+ functions including financial, statistical, logical, text, date/time, and lookup operations. Cross-validated against Excel with **82,881 formulas at 100% accuracy**.

## Features

- **Full Excel Formula Compatibility** - Supports complex formulas with nested functions, array operations, and cell references
- **High Performance** - In-memory DAG-based calculation with dependency tracking
- **Spill Arrays** - Dynamic array formulas that automatically expand results
- **Excel I/O** - Load and save Excel workbooks via Apache POI integration
- **Extensible** - Plugin architecture for custom functions
- **Zero Proprietary Dependencies** - Pure open-source, self-contained engine

## Quick Start

### Gradle

```gradle
dependencies {
    implementation 'com.scoopanalytics.hypercell:hypercell-core:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.scoopanalytics.hypercell</groupId>
    <artifactId>hypercell-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Basic Usage

```java
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import com.scoopanalytics.hypercell.core.grid.MemSheet;
import com.scoopanalytics.hypercell.core.grid.MemCell;

// Create a workbook programmatically
MemWorkbook workbook = new MemWorkbook();
MemSheet sheet = workbook.createSheet("Sheet1");

// Set values using MemCell
sheet.setCellAt(0, 0, new MemCell(100));  // A1 = 100
sheet.setCellAt(0, 1, new MemCell(200));  // B1 = 200

// Set a formula
MemCell formulaCell = new MemCell();
formulaCell.setFormula("=A1+B1");
sheet.setCellAt(0, 2, formulaCell);  // C1 = =A1+B1

// Calculate
workbook.calculate();

// Get result
Number result = sheet.getCellAt(0, 2).getNumberValue();  // 300
```

### Loading Excel Files

```java
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;

// Load Excel file via Apache POI
XSSFWorkbook poiWorkbook = new XSSFWorkbook(new FileInputStream("input.xlsx"));
MemWorkbook workbook = new MemWorkbook("input.xlsx", poiWorkbook, true);

// Calculate all formulas
workbook.calculate();

// Access results
MemSheet sheet = workbook.getSheet("Sheet1");
MemCell cell = sheet.getCellAt(0, 0);
System.out.println(cell.getValue());
```

### Working with Different Value Types

```java
// Numeric values
MemCell numCell = new MemCell(42.5);
sheet.setCellAt(0, 0, numCell);

// String values
MemCell strCell = new MemCell();
strCell.setStringValue("Hello World");
strCell.setCellType(MemCellType.String);
sheet.setCellAt(0, 1, strCell);

// Boolean values
MemCell boolCell = new MemCell(true);
sheet.setCellAt(0, 2, boolCell);

// Date values (Excel serial date format)
MemCell dateCell = new MemCell();
dateCell.setNumberValue(45000);  // Excel serial date
dateCell.setCellType(MemCellType.Date);
sheet.setCellAt(0, 3, dateCell);
```

## Supported Functions

### Math & Trigonometry
`SUM`, `SUMIF`, `SUMIFS`, `SUMPRODUCT`, `AVERAGE`, `COUNT`, `COUNTA`, `COUNTIF`, `COUNTIFS`, `MIN`, `MAX`, `ABS`, `ROUND`, `ROUNDUP`, `ROUNDDOWN`, `FLOOR`, `CEILING`, `MOD`, `POWER`, `SQRT`, `EXP`, `LN`, `LOG`, `LOG10`, `SIN`, `COS`, `TAN`, `ASIN`, `ACOS`, `ATAN`, `PI`, `RAND`, `RANDBETWEEN`

### Statistical
`AVERAGE`, `AVERAGEIF`, `AVERAGEIFS`, `MEDIAN`, `MODE`, `STDEV`, `STDEV.S`, `STDEV.P`, `VAR`, `VAR.S`, `VAR.P`, `LARGE`, `SMALL`, `RANK`, `PERCENTILE`, `QUARTILE`, `CORREL`, `FORECAST`

### Logical
`IF`, `IFS`, `IFERROR`, `IFNA`, `AND`, `OR`, `NOT`, `XOR`, `TRUE`, `FALSE`, `SWITCH`

### Text
`CONCATENATE`, `CONCAT`, `TEXTJOIN`, `LEFT`, `RIGHT`, `MID`, `LEN`, `FIND`, `SEARCH`, `REPLACE`, `SUBSTITUTE`, `TRIM`, `LOWER`, `UPPER`, `PROPER`, `TEXT`, `VALUE`, `TEXTBEFORE`, `TEXTAFTER`, `REPT`, `CHAR`, `CODE`

### Lookup & Reference
`VLOOKUP`, `HLOOKUP`, `INDEX`, `MATCH`, `XLOOKUP`, `OFFSET`, `INDIRECT`, `ROW`, `COLUMN`, `ROWS`, `COLUMNS`, `CHOOSE`, `ADDRESS`, `HYPERLINK`

### Date & Time
`DATE`, `TIME`, `NOW`, `TODAY`, `YEAR`, `MONTH`, `DAY`, `HOUR`, `MINUTE`, `SECOND`, `WEEKDAY`, `WEEKNUM`, `EOMONTH`, `EDATE`, `DATEDIF`, `DATEVALUE`, `TIMEVALUE`, `NETWORKDAYS`, `WORKDAY`, `DAYS`, `DAYS360`

### Financial
`PMT`, `PV`, `FV`, `RATE`, `NPER`, `NPV`, `IRR`, `XNPV`, `XIRR`, `SLN`, `DB`, `DDB`

### Information
`ISBLANK`, `ISERROR`, `ISNA`, `ISNUMBER`, `ISTEXT`, `ISLOGICAL`, `ISREF`, `ISERR`, `ISEVEN`, `ISODD`, `TYPE`, `NA`, `ERROR.TYPE`

### Array Functions
`FILTER`, `SORT`, `SORTBY`, `UNIQUE`, `SEQUENCE`, `TRANSPOSE`

## Module Structure

```
oss/
├── hypercell-api/        # Public interfaces
│   └── EvaluationContext, DataSource, Expression, Function
│
├── hypercell-formula/    # ANTLR4 Excel grammar
│   └── ExcelFormula.g4 → Parser/Lexer
│
├── hypercell-core/       # Main calculation engine
│   ├── grid/             # MemWorkbook, MemSheet, MemCell
│   ├── expression/       # Formula compilation & evaluation
│   └── types/            # Value types, errors
│
└── hypercell-functions/  # Function implementations (reserved)
```

## Building from Source

```bash
# From oss/ directory
./gradlew clean build

# Run tests
./gradlew test

# Run cross-validation tests
./gradlew :hypercell-core:test --tests "*CrossValidationTest*"
```

### Requirements

- Java 21 or higher
- Gradle 8.5 or higher

## Validation

Cross-validated against Microsoft Excel:

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

**Note**: 12 formulas (0.014%) were skipped due to unsupported functions or parse errors.
All 82,881 validated formulas produce identical results to Excel.

## Architecture

### Core Components

| Component | Description |
|-----------|-------------|
| **MemWorkbook** | In-memory workbook with sheet collection and calculation engine |
| **MemSheet** | Worksheet with sparse cell grid and named ranges |
| **MemCell** | Cell containing value, formula, type, and computed result |
| **Compile** | ANTLR4-based formula parser that builds expression trees |
| **HyperCellExpression** | Base class for executable expression nodes |

### Formula Evaluation Pipeline

1. **Parsing** - ANTLR4 parses formula string into parse tree
2. **Compilation** - `Compile` transforms parse tree into expression DAG
3. **Dependency Resolution** - Cell references and ranges are resolved
4. **Topological Sort** - Cells ordered by dependencies for calculation
5. **Evaluation** - Expression DAG is evaluated with caching
6. **Spill Handling** - Array results expand into adjacent cells

### Custom Functions

```java
import com.scoopanalytics.hypercell.api.FunctionRegistry;

// Get the function registry and register a custom function
FunctionRegistry registry = workbook.getRegistry();
registry.register("DOUBLE", (args, context) -> {
    double value = args.get(0).getDoubleValue();
    return new MemCell(value * 2);
});

// Use in formula
formulaCell.setFormula("=DOUBLE(A1)");
```

### Custom Evaluation Context

Implement `EvaluationContext` to provide external data:

```java
import com.scoopanalytics.hypercell.api.EvaluationContext;
import com.scoopanalytics.hypercell.api.DataSource;

public class MyContext implements EvaluationContext {
    @Override
    public List<DataSource> getDataSources() {
        // Return available external data sources
        return Collections.emptyList();
    }
}

// Calculate with custom context
MyContext myContext = new MyContext();
workbook.calculate(myContext);
```

## Performance

HyperCell is optimized for high-performance calculation scenarios:

- **DAG-Based Calculation** - Directed Acyclic Graph ensures optimal evaluation order
- **Dependency Tracking** - Only recalculates cells affected by changes
- **Value Caching** - Computed values cached until dependencies change
- **Sparse Storage** - Efficient memory for large worksheets with gaps
- **Lazy Compilation** - Formulas compiled on first calculation

## Enterprise Integration

For enterprise features like query refresh, audit logging, and multi-tenant support, contact [Scoop Analytics](https://scoopanalytics.com).

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Run tests: `./gradlew clean build`
4. Submit a pull request

## Links

- **Parent Project**: [HyperCell](../)
- **Issues**: [GitHub Issues](https://github.com/Scoop-Analytics/hypercell/issues)
- **Scoop Analytics**: [scoopanalytics.com](https://scoopanalytics.com)

---

*HyperCell is developed and maintained by [Scoop Analytics](https://scoopanalytics.com).*
