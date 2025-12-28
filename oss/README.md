# HyperCell

**A high-performance, in-memory Excel calculation engine for Java.**

HyperCell evaluates Excel formulas with full compatibility, supporting 200+ functions including financial, statistical, logical, text, date/time, and lookup operations.

## Features

- **Full Excel Formula Compatibility** - Supports complex formulas with nested functions, array operations, and cell references
- **High Performance** - In-memory calculation with parallel processing support
- **Spill Arrays** - Dynamic array formulas that automatically expand results
- **Excel I/O** - Load and save Excel workbooks via Apache POI integration
- **Extensible** - Plugin architecture for custom functions via `FunctionRegistry`
- **Zero External Dependencies** - Core engine has no proprietary dependencies

## Quick Start

### Gradle

```gradle
dependencies {
    implementation 'io.hypercell:hypercell-core:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.hypercell</groupId>
    <artifactId>hypercell-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Basic Usage

```java
import io.hypercell.core.grid.MemWorkbook;
import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.MemCell;

// Create a workbook
MemWorkbook workbook = new MemWorkbook("MyWorkbook");
MemSheet sheet = workbook.addSheet("Sheet1");

// Set values
sheet.setCellValue(0, 0, 100);  // A1 = 100
sheet.setCellValue(0, 1, 200);  // B1 = 200

// Set a formula
MemCell cell = sheet.getCellAt(0, 2);
cell.setFormula("=A1+B1");

// Calculate
workbook.calculate();

// Get result
Number result = cell.getNumberValue();  // 300
```

### Loading Excel Files

```java
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;

// Load Excel file
XSSFWorkbook poiWorkbook = new XSSFWorkbook(new FileInputStream("input.xlsx"));
MemWorkbook workbook = new MemWorkbook(null, "MyWorkbook", poiWorkbook, false);

// Calculate all formulas
workbook.calculate();

// Access results
MemSheet sheet = workbook.getSheet("Sheet1");
MemCell cell = sheet.getCellAt(0, 0);
System.out.println(cell.getValue());
```

## Supported Functions

### Math & Trigonometry
`SUM`, `SUMIF`, `SUMIFS`, `SUMPRODUCT`, `AVERAGE`, `COUNT`, `COUNTA`, `COUNTIF`, `COUNTIFS`, `MIN`, `MAX`, `ABS`, `ROUND`, `ROUNDUP`, `ROUNDDOWN`, `FLOOR`, `CEILING`, `MOD`, `POWER`, `SQRT`, `EXP`, `LN`, `LOG`, `LOG10`, `SIN`, `COS`, `TAN`, `ASIN`, `ACOS`, `ATAN`, `PI`, `RAND`, `RANDBETWEEN`

### Statistical
`AVERAGE`, `AVERAGEIF`, `AVERAGEIFS`, `MEDIAN`, `MODE`, `STDEV`, `VAR`, `LARGE`, `SMALL`, `RANK`, `PERCENTILE`, `QUARTILE`, `CORREL`, `FORECAST`

### Logical
`IF`, `IFS`, `IFERROR`, `IFNA`, `AND`, `OR`, `NOT`, `XOR`, `TRUE`, `FALSE`, `SWITCH`

### Text
`CONCATENATE`, `CONCAT`, `TEXTJOIN`, `LEFT`, `RIGHT`, `MID`, `LEN`, `FIND`, `SEARCH`, `REPLACE`, `SUBSTITUTE`, `TRIM`, `LOWER`, `UPPER`, `PROPER`, `TEXT`, `VALUE`, `TEXTBEFORE`, `TEXTAFTER`

### Lookup & Reference
`VLOOKUP`, `HLOOKUP`, `INDEX`, `MATCH`, `XLOOKUP`, `OFFSET`, `INDIRECT`, `ROW`, `COLUMN`, `ROWS`, `COLUMNS`, `CHOOSE`

### Date & Time
`DATE`, `TIME`, `NOW`, `TODAY`, `YEAR`, `MONTH`, `DAY`, `HOUR`, `MINUTE`, `SECOND`, `WEEKDAY`, `WEEKNUM`, `EOMONTH`, `EDATE`, `DATEDIF`, `DATEVALUE`, `TIMEVALUE`, `NETWORKDAYS`, `WORKDAY`

### Financial
`PMT`, `PV`, `FV`, `RATE`, `NPER`, `NPV`, `IRR`, `XNPV`, `XIRR`

### Information
`ISBLANK`, `ISERROR`, `ISNA`, `ISNUMBER`, `ISTEXT`, `ISLOGICAL`, `ISREF`, `ISERR`, `ISDATE`, `TYPE`, `NA`, `ERROR.TYPE`

### Array Functions
`FILTER`, `SORT`, `UNIQUE`, `SEQUENCE`

## Module Structure

```
hypercell/
├── hypercell-api/        # Public interfaces (EvaluationContext, CellValue, etc.)
├── hypercell-formula/    # ANTLR4 grammar for Excel formula parsing
├── hypercell-core/       # Main calculation engine
└── hypercell-functions/  # Function implementations
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/scoopanalytics/hypercell.git
cd hypercell

# Build
./gradlew clean build

# Run tests
./gradlew test
```

### Requirements

- Java 21 or higher
- Gradle 8.5 or higher

## Architecture

### Core Components

- **MemWorkbook** - In-memory representation of an Excel workbook
- **MemSheet** - Individual worksheet with cell grid
- **MemCell** - Cell containing value, formula, and formatting
- **Compile** - Formula parser and expression tree builder
- **HyperCellExpression** - Base class for executable expression nodes

### Formula Evaluation Pipeline

1. **Parsing** - ANTLR4 parses formula string into parse tree
2. **Compilation** - `Compile` transforms parse tree into expression tree
3. **Dependency Resolution** - Cell references and ranges are resolved
4. **Calculation** - Expression tree is evaluated recursively
5. **Spill Handling** - Array results expand into adjacent cells

## Extending HyperCell

### Custom Functions

```java
// Register a custom function
FunctionRegistry registry = workbook.getRegistry();
registry.register("MYFUNC", (args, context) -> {
    double value = args.get(0).getDoubleValue();
    return new MemCell(value * 2);
});

// Use in formula
cell.setFormula("=MYFUNC(A1)");
```

### Custom Evaluation Context

Implement `EvaluationContext` to provide external data sources:

```java
public class MyContext implements EvaluationContext {
    @Override
    public Object resolveReference(String sheet, int row, int col) {
        // Return value from external source
    }

    @Override
    public List<DataSource> getDataSources() {
        // Return available data sources
    }
}
```

## Performance

HyperCell is designed for high-performance scenarios:

- **Parallel Calculation** - Multi-threaded formula evaluation
- **Lazy Evaluation** - Cells calculated only when needed
- **Caching** - Computed values cached to avoid recalculation
- **Efficient Memory** - Sparse array storage for large sheets

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## Support

- **Issues**: [GitHub Issues](https://github.com/scoopanalytics/hypercell/issues)
- **Documentation**: [Wiki](https://github.com/scoopanalytics/hypercell/wiki)

---

*HyperCell is developed and maintained by [Scoop Analytics](https://scoopanalytics.com).*
