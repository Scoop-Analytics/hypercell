# HyperCell Extraction - Status Report
*Generated: November 20, 2025*

## ✅ WHAT'S WORKING

### HyperCell Project (../hypercell)
- **BUILD STATUS**: ✅ **COMPILES SUCCESSFULLY**
- All 4 modules build cleanly:
  - `hypercell-api` - Core interfaces (Expression, CellValue, Function, FunctionRegistry, EvaluationContext)
  - `hypercell-formula` - ANTLR grammar (HyperCellExpression.g4, HyperCellDate.g4)
  - `hypercell-functions` - Function implementations (BaseFunction, etc.)
  - `hypercell-core` - Core grid and expression engine

### Fixed Today
- **LogicalFunction.java** - Had doubled import paths (`io.hypercell.core.grid.io.hypercell.core.grid.MemCell`)
  - Fixed by removing duplicate package prefixes throughout the file
  - All 12 compilation errors resolved

### Core Infrastructure (Successfully Migrated)
- Grid classes: `MemWorkbook`, `MemSheet`, `MemCell` → `io.hypercell.core.grid`
- Expression engine: `Compile`, `Identifier`, `Range`, `CompileContext` → `io.hypercell.core.expression`
- Base classes: `AbstractExpression`, `BaseFunctionExpression` → `io.hypercell.core.expression`
- Some functions: `LogicalFunction`, `DateTimeFunction`, `BooleanArray` → `io.hypercell.core.expression`
- Grammar: `HyperCellExpression.g4` compiles and generates parser/lexer

---

## ❌ WHAT'S BROKEN

### Scoop Project (/home/bradpeters/dev/scoop)
- **BUILD STATUS**: ❌ **866 COMPILATION ERRORS**
- Root cause: **Partial migration of function library**

### The Problem: Stranded Function Classes

**15 function classes remain in Scoop** at `app/src/main/java/scoop/expression/`:
- `TextualFunction.java` (23 KB)
- `MathFunction.java` (38 KB)
- `LookupFunction.java` (16 KB)
- `FilterFunction.java` (10 KB)
- `StatisticalFunction.java` (2.6 KB)
- `FinancialFunction.java` (2.8 KB)
- `InformationFunction.java` (4.4 KB)
- `ErrorFunction.java` (531 bytes)
- `ScoopFunction.java` (21 KB) - **Proprietary, should stay**
- Plus 6 utility/support files

**Why they're broken:**
1. **Extends `Function`** - This class was removed/moved, no import exists
2. **References `ScoopExpressionParser`** - Now called `HyperCellExpressionParser` in hypercell
3. **References `ScoopExpressionLexer`** - Now called `HyperCellExpressionLexer` in hypercell
4. **Missing `CompileContext` import** - Moved to `io.hypercell.core.expression`
5. **Uses `calculateCellValue()` method** - BaseFunctionExpression uses `evaluate()` instead

### Sample Error Pattern (from TextualFunction.java)
```
error: cannot find symbol
  public class TextualFunction extends Function
                                       ^
  symbol: class Function

error: cannot find symbol
  } else if (type == ScoopExpressionParser.MIDTOKEN)
                     ^
  symbol: variable ScoopExpressionParser
```

---

## 🎯 DECISION REQUIRED: Path Forward

There are **3 possible approaches** to complete this extraction:

### Option 1: Complete the Migration (Recommended)
**Move all standard Excel functions to hypercell, keep only Scoop-proprietary functions in Scoop**

**Standard functions → hypercell** (Excel-compatible, no Scoop dependencies):
- TextualFunction (MID, LEFT, RIGHT, CONCAT, TEXT, etc.)
- MathFunction (SUM, AVERAGE, ROUND, ABS, etc.)
- LookupFunction (VLOOKUP, INDEX, MATCH, etc.)
- StatisticalFunction (AVERAGE, COUNT, STDEV, etc.)
- FinancialFunction (NPV, IRR, PMT, etc.)
- InformationFunction (ISERROR, ISBLANK, TYPE, etc.)
- ErrorFunction (NA, ERROR.TYPE, etc.)
- FilterFunction (FILTER, SORT, UNIQUE - might be Scoop-specific?)

**Scoop-proprietary → stays in Scoop** (Requires ScoopContext, database, ML):
- ScoopFunction (SCOOPLOOKUP, SCOOPPREDICT, SCOOPNEXTCONVERSION, etc.)

**Steps:**
1. For each standard function file:
   - Copy to `hypercell-functions/src/main/java/io/hypercell/functions/`
   - Change package to `io.hypercell.functions`
   - Extend `BaseFunctionExpression` instead of `Function`
   - Add `FunctionRegistry registry` parameter to constructor
   - Replace `ScoopExpressionParser` → `HyperCellExpressionParser`
   - Replace `ScoopExpressionLexer` → `HyperCellExpressionLexer`
   - Import `io.hypercell.core.expression.CompileContext`
2. Register functions in `StandardLibrary` (hypercell-functions)
3. Delete the files from Scoop's expression package
4. Update ScoopFunction to use hypercell base classes
5. Test Scoop compilation

**Effort:** 2-4 hours (systematic refactoring, ~8 files)

---

### Option 2: Bridge Pattern (Quick Fix)
**Keep function files in Scoop, create adapter layer**

**Steps:**
1. Create `scoop.expression.Function` as a bridge class:
   ```java
   package scoop.expression;
   import io.hypercell.core.expression.BaseFunctionExpression;
   import io.hypercell.core.expression.CompileContext;
   import io.hypercell.api.FunctionRegistry;

   public abstract class Function extends BaseFunctionExpression {
       public Function(ParseTree tree, CompileContext cc) {
           super(tree, cc, null); // No registry for legacy functions
       }
   }
   ```
2. Create type aliases for parser references:
   ```java
   package scoop.expression;
   import io.hypercell.formula.HyperCellExpressionParser;
   import io.hypercell.formula.HyperCellExpressionLexer;

   public class ScoopExpressionParser extends HyperCellExpressionParser { }
   public class ScoopExpressionLexer extends HyperCellExpressionLexer { }
   ```
3. Add missing imports to each function file
4. Test Scoop compilation

**Pros:** Fast, minimal changes
**Cons:** Technical debt, functions still coupled to Scoop, hypercell not self-contained

**Effort:** 1-2 hours

---

### Option 3: Revert the Migration (Not Recommended)
**Undo the extraction, keep everything in Scoop**

**Why not:**
- Previous AI explicitly said: "Do NOT revert: The structure is correct"
- HyperCell builds successfully - the hard work is done
- The API design is sound
- Strategic value of open-sourcing the calculation engine

---

## 📋 RECOMMENDED ACTION PLAN

**Proceed with Option 1: Complete the Migration**

### Phase 1: Classify Functions (15 minutes)
- [x] Identify standard Excel functions (move to hypercell)
- [x] Identify Scoop-proprietary functions (keep in Scoop, adapt)

### Phase 2: Migrate Standard Functions (2-3 hours)
For each standard function (TextualFunction, MathFunction, etc.):
1. Copy file to `hypercell-functions/src/main/java/io/hypercell/functions/`
2. Refactor:
   - Package: `package io.hypercell.functions;`
   - Base class: `extends BaseFunctionExpression`
   - Constructor: Add `FunctionRegistry registry` parameter
   - Imports: Fix all scoop.* → io.hypercell.*
   - Parser refs: ScoopExpression* → HyperCellExpression*
3. Test build: `cd ../hypercell && ./gradlew :hypercell-functions:compileJava`
4. Delete from Scoop

### Phase 3: Adapt Scoop-Proprietary Functions (1 hour)
For ScoopFunction and any others that need ScoopContext:
1. Create `scoop.expression.ScoopBaseFunctionExpression` that bridges to hypercell
2. Update ScoopFunction to extend the bridge class
3. Keep Scoop-specific logic intact

### Phase 4: Verify Integration (30 minutes)
1. Build hypercell: `cd ../hypercell && ./gradlew build`
2. Build Scoop: `cd /home/bradpeters/dev/scoop && ./gradlew :app:compileJava`
3. Run Scoop tests: `./gradlew test --tests ChatRegressionTestSuite`

### Phase 5: Documentation (30 minutes)
1. Update `HYPERCELL_EXTRACTION_PLAN.md` - Mark Phase 4 complete
2. Update hypercell README with function list
3. Document ScoopProprietaryFunctions registry

---

## 🔧 TECHNICAL REFERENCE

### Key Class Mappings (Old → New)
| Old (Scoop) | New (HyperCell) |
|-------------|-----------------|
| `scoop.expression.Function` | `io.hypercell.core.expression.BaseFunctionExpression` |
| `scoop.expression.Compile` | `io.hypercell.core.expression.Compile` |
| `scoop.expression.CompileContext` | `io.hypercell.core.expression.CompileContext` |
| `scoop.expression.ScoopExpressionParser` | `io.hypercell.formula.HyperCellExpressionParser` |
| `scoop.expression.ScoopExpressionLexer` | `io.hypercell.formula.HyperCellExpressionLexer` |
| `scoop.worksheet.memsheet.MemCell` | `io.hypercell.core.grid.MemCell` |
| `scoop.worksheet.memsheet.MemSheet` | `io.hypercell.core.grid.MemSheet` |
| `scoop.worksheet.memsheet.MemWorkbook` | `io.hypercell.core.grid.MemWorkbook` |

### Constructor Pattern Change
**Old (Scoop):**
```java
public class TextualFunction extends Function {
    public TextualFunction(ParseTree tree, CompileContext cc) {
        super(tree, cc);
    }
}
```

**New (HyperCell):**
```java
public class TextualFunction extends BaseFunctionExpression {
    public TextualFunction(ParseTree tree, CompileContext cc, FunctionRegistry registry) {
        super(tree, cc, registry);
    }
}
```

### Import Template for Migrated Functions
```java
package io.hypercell.functions;

import org.antlr.v4.runtime.tree.ParseTree;
import io.hypercell.api.CellValue;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.core.expression.BaseFunctionExpression;
import io.hypercell.core.expression.CompileContext;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;
```

---

## 🚦 CURRENT STATUS SUMMARY

| Component | Status | Next Action |
|-----------|--------|-------------|
| HyperCell build | ✅ Working | Continue migration |
| Scoop build | ❌ Broken (866 errors) | Fix function classes |
| Core infrastructure | ✅ Complete | None needed |
| Standard functions | ⚠️ Partially migrated | Complete migration |
| Proprietary functions | ⚠️ Not adapted | Create bridge layer |
| Tests | ❌ Can't run (Scoop broken) | Fix after migration |

**Estimated time to complete:** 4-5 hours of focused work

---

## 💬 NOTES FOR NEXT AI

1. **The architecture is correct** - Don't revert, push forward
2. **LogicalFunction.java is fixed** - Use as reference for other functions
3. **BaseFunctionExpression is the pattern** - All functions should extend it
4. **ScoopFunction is special** - It's proprietary, needs bridge to hypercell
5. **FilterFunction might be proprietary** - Check if it uses Scoop-specific data structures
6. **Follow the constructor pattern** - Add `FunctionRegistry registry` parameter
7. **Test incrementally** - Build hypercell after each function migration
8. **The hard part is done** - This is just systematic refactoring now

**Good luck! The finish line is in sight.** 🎯
