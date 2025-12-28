/**
 *
 */
package io.hypercell.core.grid;

import org.antlr.v4.runtime.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hypercell.api.CellAddress;
import io.hypercell.core.expression.Compile;
import io.hypercell.core.expression.Identifier;
import io.hypercell.core.expression.Range;
import io.hypercell.core.expression.SpillArea;
import io.hypercell.core.expression.ThrowingErrorListener;
import io.hypercell.formula.HyperCellExpressionParser.ExpressionContext;
import io.hypercell.core.util.DateFormatUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author bradpeters
 */
public class MemCell implements io.hypercell.api.CellValue { 
    @Override public Type getType() {
        if (cellType == MemCellType.String) return Type.STRING;
        if (cellType == MemCellType.Number) return Type.NUMBER;
        return Type.EMPTY; // simplify
    }
    @Override public io.hypercell.api.CellValue[][] getArrayValue() { return array; }
    @Override public Boolean getBooleanValue() { return numberValue != null && numberValue.doubleValue() != 0; }

    private static final Logger logger = LoggerFactory.getLogger(MemCell.class);
    private static long count;
    private static final int MAX_RECURSION_DEPTH = 100;
    private static final ThreadLocal<Integer> recursionDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Set<MemCell>> calculationStack = ThreadLocal.withInitial(() -> new java.util.HashSet<>());
    private MemCellType cellType;
    private String stringValue;
    private String formulaValue;
    private Number numberValue;
    private transient Compile compile;
    private FormulaError errorValue;
    private boolean calculated = false;
    private boolean booleanColumn = false;
    private MemCellContext cellContext;
    private transient MemSheet sheet;
    private transient MemCell[][] array;
    private transient MemCell inferredFromCell;
    private transient List<MemCell> dependencies;

    public MemCell()
    {

    }

    public MemCell(Object value)
    {
        if (value instanceof String) {
            this.cellType = MemCellType.String;
            this.stringValue = (String) value;
        } else if (value instanceof Number) {
            this.cellType = MemCellType.Number;
            this.numberValue = (Number) value;
        } else if (value instanceof Boolean) {
            this.cellType = MemCellType.Number;
            this.booleanColumn = true;
            this.numberValue = ((Boolean) value) ? 1 : 0;
        }
    }

    public MemCell(String value)
    {
        this.cellType = MemCellType.String;
        this.stringValue = value;
    }

    public MemCell(Number value)
    {
        this.cellType = MemCellType.Number;
        this.numberValue = value;
    }

    public MemCell(boolean value)
    {
        this.cellType = MemCellType.Number;
        this.booleanColumn = true;
        this.numberValue = value ? 1 : 0;
    }

    public MemCell(MemCell mc)
    {
        this.cellType = mc.cellType;
        this.errorValue = mc.errorValue;
        this.stringValue = mc.stringValue;
        this.numberValue = mc.numberValue;
        this.formulaValue = mc.formulaValue;
        this.compile = mc.compile;
        this.cellContext = mc.cellContext;
        this.calculated = mc.calculated;
        this.booleanColumn = mc.booleanColumn;
    }

    public MemCell(FormulaError errorValue)
    {
        this.errorValue = errorValue;
    }

    public MemCell(MemCell[][] array)
    {
        this.array = array;
    }

    public MemCellType getCellType()
    {
        return cellType;
    }

    public void setCellType(MemCellType cellType)
    {
        this.cellType = cellType;
        if (cellType == MemCellType.Formula)
        {
            formulaValue = stringValue;
        }
    }

    public void calculate()
    {
        count++;
        MemCell oldCell = null;
        synchronized (this)
        {

            if (calculated || compile == null)
                return;
                
            // Check recursion depth
            int currentDepth = recursionDepth.get();
            if (currentDepth >= MAX_RECURSION_DEPTH)
            {
                logger.error("Maximum recursion depth exceeded for formula: {}", formulaValue);
                errorValue = FormulaError.NA;
                calculated = true;
                return;
            }
            
            // Check for circular references
            Set<MemCell> stack = calculationStack.get();
            if (stack.contains(this))
            {
                logger.error("Circular reference detected in formula: {}", formulaValue);
                errorValue = FormulaError.REF;
                calculated = true;
                return;
            }
            
            // Add to calculation stack and increment depth
            stack.add(this);
            recursionDepth.set(currentDepth + 1);
            
            try
            {
                calculated = true;
                if (dependencies != null)
                {
                    for (MemCell mc : dependencies)
                    {
                        mc.calculate();
                    }
                }
                MemCell value = null;
                try
                {
                    if (compile != null && compile.getExpression() != null) {
                        value = compile.getExpression().calculateCellValue();
                    }
                } catch (Exception calcException)
                {
                    logger.error("Error calculating MemCell with formula {}", formulaValue, calcException);
                    value = new MemCell(FormulaError.NA);
                }
                if (sheet != null && sheet.getWorkbook().getChangedCells() != null)
                {
                    oldCell = new MemCell(this);
                }
                stringValue = null;
                numberValue = null;
                if (value == null)
                {
                    return;
                }
                if (value.errorValue != null)
                    errorValue = value.errorValue;
                else if (value.stringValue != null)
                    stringValue = value.stringValue;
                else if (value.numberValue != null)
                    numberValue = value.numberValue;
                else if (value.array != null)
                    array = value.array;
            }
            finally
            {
                // Clean up recursion tracking
                stack.remove(this);
                recursionDepth.set(currentDepth);
            }
        }
        if (oldCell != null && !equals(oldCell))
        {
            sheet.getWorkbook().getChangedCells().add(this);
        }
        if (array != null && sheet != null && array.length > 0)
        {
            for (int row = 0; row < array.length; row++)
            {
                if (array[row] == null)
                    continue;
                for (int col = 0; col < array[row].length; col++)
                {
                    MemCell targetCell = sheet.getOrCreateCellAt(cellContext.getRow() + row, cellContext.getCol() + col,
                            cellType);
                    if (row != 0 && col != 0)
                    {
                        targetCell.inferredFromCell = this;
                    }
                    targetCell.calculated = true;
                    targetCell.stringValue = array[row][col].stringValue;
                    targetCell.numberValue = array[row][col].numberValue;
                }
            }
        }
    }

    @Override
    public String toString()
    {
        if (errorValue != null)
            return errorValue.toString();
        if (cellType == MemCellType.String)
            return stringValue;
        if (cellType == MemCellType.Number)
        {
            if (numberValue != null)
            {
                return numberValue.toString();
            } else
            {
                return "null";
            }
        }
        if (cellType == MemCellType.Formula)
        {
            if (numberValue != null)
                return numberValue.toString();
            else if (stringValue != null)
                return stringValue;
            return null;
        }
        return "";
    }

    public String describeCell()
    {
        if (cellType == null)
        {
            return "[" + System.identityHashCode(
                    this) + ":no cell type]";
        }
        return "[" + System.identityHashCode(
                this) + ":" + cellType.toString() + ":" + (cellType == MemCellType.Formula ? formulaValue + ":" : "") + this.toString() + "]";
    }

    public void setCompile(Compile compile)
    {
        this.compile = compile;
    }

    public boolean hasStringValue()
    {
        return stringValue != null && numberValue == null;
    }

    public boolean hasNumberValue()
    {
        return numberValue != null && stringValue == null;
    }

    public String getStringValue()
    {
        if (cellType == MemCellType.Formula && !calculated)
        {
            calculate();
        }
        if (errorValue != null)
        {
            return errorValue.getDisplay();
        }
        if (stringValue == null && numberValue != null)
        {
            NumberFormat format = NumberFormat.getInstance();
            format.setMinimumFractionDigits(0);
            format.setGroupingUsed(false);
            return format.format(numberValue);
        }
        return stringValue;
    }

    public String getFormula()
    {
        return formulaValue;
    }

    public Number getNumberValue()
    {
        if (cellType == MemCellType.Formula && !calculated)
        {
            calculate();
        }
        if (numberValue == null && stringValue != null)
        {
            try
            {
                return Double.valueOf(stringValue);
            } catch (Exception e)
            {
                return null;
            }
        }
        return numberValue == null ? 0 : numberValue;
    }

    public Double getDoubleValue()
    {
        Number n = getNumberValue();
        if (n != null)
            return n.doubleValue();
        return null;
    }

    public Object getValue()
    {
        if (cellType == MemCellType.Formula)
        {
            if (!calculated)
            {
                calculate();
            }
            return stringValue != null ? stringValue : numberValue;
        }
        if (cellType == MemCellType.String)
            return stringValue;
        if (cellType == MemCellType.Number)
            return numberValue;
        return null;
    }

    public boolean isCalculated()
    {
        return calculated;
    }

    public void setCalculated(boolean newcalc)
    {
        this.calculated = newcalc;
    }
    
    /**
     * Reset thread-local recursion tracking variables.
     * Should be called after a complete calculation cycle to prevent memory leaks.
     */
    public static void resetRecursionTracking()
    {
        recursionDepth.remove();
        calculationStack.remove();
    }

    public boolean equals(MemCell other)
    {
        return equals(other, false);
    }

    public boolean equals(MemCell other, boolean caseInsensitive)
    {
        if (cellType != other.cellType)
        {
            if (cellType == MemCellType.Formula || other.cellType == MemCellType.Formula)
            {
                if (!((stringValue != null && other.stringValue != null)
                        || (numberValue != null && other.numberValue != null)))
                {
                    return false;
                }
            } else
            {
                return false;
            }
        }
        if (errorValue != null || other.errorValue != null)
            return false;
        if (cellType == MemCellType.String || (cellType == MemCellType.Formula && stringValue != null))
        {
            if (stringValue == null && other.stringValue == null)
                return true;
            if (stringValue == null || other.stringValue == null)
                return false;
            if (caseInsensitive)
            {
                return stringValue.equalsIgnoreCase(other.stringValue);
            } else
            {
                return stringValue.equals(other.stringValue);
            }
        }
        if (cellType == MemCellType.Number || (cellType == MemCellType.Formula && numberValue != null))
        {
            if (numberValue == null && other.numberValue == null)
                return true;
            if (numberValue == null || other.numberValue == null)
                return false;
            return numberValue.doubleValue() == other.numberValue.doubleValue();
        }
        return false;
    }

    public FormulaError getCompareError(MemCell other)
    {
        if (errorValue != null)
            return errorValue;
        if (other.errorValue != null)
            return other.errorValue;
        return null;
    }

    public int compareTo(MemCell other)
    {
        return compareTo(other, false);
    }

    public int compareTo(MemCell other, boolean caseInsensitive)
    {
        if (cellType != other.cellType)
        {
            if (cellType != MemCellType.Formula && other.cellType != MemCellType.Formula)
            {
                return toString().compareTo(other.toString());
            }
        }
        if (errorValue != null || other.errorValue != null)
            return 0;
        if (cellType == MemCellType.String || (cellType == MemCellType.Formula && stringValue != null))
        {
            if (stringValue == null && other.stringValue == null)
                return 0;
            if (stringValue == null || other.stringValue == null)
                return stringValue != null ? 1 : -1;
            if (caseInsensitive)
            {
                return stringValue.toLowerCase().compareTo(other.stringValue.toLowerCase());
            } else
            {
                return stringValue.compareTo(other.stringValue);
            }
        }
        if (cellType == MemCellType.Number || (cellType == MemCellType.Formula && numberValue != null))
        {
            if (numberValue == null && other.numberValue == null)
                return 0;
            if (numberValue == null || other.numberValue == null)
                return numberValue != null ? 1 : -1;
            return Double.compare(numberValue.doubleValue(), other.numberValue.doubleValue());
        }
        return 0;
    }

    public FormulaError getErrorValue()
    {
        return errorValue;
    }

    public void clearError()
    {
        this.errorValue = null;
    }

    public void setNumberValue(Number numberValue)
    {
        this.numberValue = numberValue;
    }

    public void setStringValue(String s)
    {
        this.stringValue = s;
    }

    public void setFormula(String formulaValue)
    {
        this.formulaValue = formulaValue;
        this.cellType = MemCellType.Formula;
    }

    public MemCellContext getCellContext()
    {
        return cellContext;
    }

    public MemCellContext getOrSetCellContext(int row, int col)
    {
        if (cellContext == null)
        {
            cellContext = new MemCellContext(row, col);
        }
        return cellContext;
    }

    public void setCellContext(MemCellContext cellContext)
    {
        this.cellContext = cellContext;
    }

    public void setCellContext(Cell wsCell)
    {
        this.cellContext = new MemCellContext(wsCell, sheet.getWorkbook());
    }

    public void setCellContext(XSSFCellStyle style, int row, int col)
    {
        this.cellContext = new MemCellContext(style, sheet.getWorkbook(), row, col);
    }

    public String getFormatString()
    {
        if (cellContext != null && cellContext.getStyle() != null)
        {
            return cellContext.getStyle().getFormatString();
        }
        return null;
    }

    public List<CellAddress> getReferences()
    {
        if (cellType != MemCellType.Formula || compile == null)
        {
            return null;
        }
        List<CellAddress> result = new ArrayList<>();
        List<Identifier> idlist = compile.getIdentifiers();
        for (Identifier id : idlist)
        {
            boolean found = false;
            CellAddress idAddress = id.getAddress();
            for (CellAddress ca : result)
            {
                if (ca == null)
                    continue;
                if (ca.equals(idAddress))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
                result.add(idAddress);
        }
        return result;
    }

    public void compileFormula(MemSheet memSheet)
    {
        String formula = formulaValue;
        if (formula == null)
            return;
        if (formula.startsWith("_xlfn."))
        {
            formula = formula.substring(6);
            if (formula.startsWith("_xlws."))
            {
                formula = formula.substring(6);
            }
        }
        CharStream input = CharStreams.fromString(formula);
        io.hypercell.formula.HyperCellExpressionLexer lex = new io.hypercell.formula.HyperCellExpressionLexer(input);
        lex.removeErrorListeners();
        lex.addErrorListener(io.hypercell.core.expression.ThrowingErrorListener.INSTANCE);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        io.hypercell.formula.HyperCellExpressionParser parser = new io.hypercell.formula.HyperCellExpressionParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(io.hypercell.core.expression.ThrowingErrorListener.INSTANCE);
        ExpressionContext expressionContext = null;
        try
        {
            expressionContext = parser.expression();
        } catch (Exception e)
        {
            if (memSheet == null || memSheet.incrementAndGetNumParseErrors() < 5)
            {
                String parseError = "Unable to parse expression:" + input;
                if (memSheet != null)
                {
                    memSheet.addParseError(parseError);
                }
                logger.error(parseError);
            }
            return;
        }
        compile = new Compile(expressionContext, new io.hypercell.core.expression.CompileContext(memSheet));
        if (!compile.isInformationalOnly())
        {
            for (Identifier id : compile.getIdentifiers())
            {
                id.setSheet(memSheet);
            }

            // Check if expression compiled successfully before accessing it
            io.hypercell.core.expression.HyperCellExpression expr = compile.getExpression();
            if (expr != null)
            {
                SpillArea spillArea = (SpillArea) expr.possibleSpillRange();
                if (spillArea != null)
                {
                    memSheet.getSpillAreaCache().put(this, spillArea);
                }
            }
            else
            {
                // Expression compilation failed - log as parse error
                if (memSheet == null || memSheet.incrementAndGetNumParseErrors() < 5)
                {
                    String parseError = "Unable to compile expression:" + formula;
                    if (memSheet != null)
                    {
                        memSheet.addParseError(parseError);
                    }
                    logger.error(parseError);
                }
            }
        }
    }

    public List<Range> getRanges()
    {
        return compile == null ? null : compile.getRanges();
    }

    public List<Identifier> getIdentifiers()
    {
        return compile == null ? null : compile.getIdentifiers();
    }

    public void getRecursiveIdentifiers(Set<Identifier> identierSet)
    {
        var idList = getIdentifiers();
        if (idList != null)
        {
            identierSet.addAll(idList);
            for (var id : idList)
            {
                var cell = sheet.getCellAt(id.getAddress());
                if (cell.getCellType() == MemCellType.Formula)
                {
                    cell.getRecursiveIdentifiers(identierSet);
                }
            }
        }
    }

    public boolean isInformationalOnly()
    {
        return compile != null && compile.isInformationalOnly();
    }

    public MemSheet getSheet()
    {
        return sheet;
    }

    public void setSheet(MemSheet sheet)
    {
        this.sheet = sheet;
    }

    public boolean isBooleanColumn()
    {
        return booleanColumn;
    }

    public void setBooleanColumn(boolean booleanColumn)
    {
        this.booleanColumn = booleanColumn;
    }

    public void clearCell()
    {
        numberValue = null;
        stringValue = null;
        formulaValue = null;
        compile = null;
        errorValue = null;
    }

    public boolean isEmptyValue()
    {
        if (cellType == MemCellType.Number)
        {
            return numberValue == null;
        } else if (cellType == MemCellType.Formula)
        {
            return formulaValue == null;
        }
        return stringValue == null;
    }

    public boolean isExcelDate()
    {
        if (cellContext != null && cellContext.getStyle() != null)
        {
            var style = cellContext.getStyle();
            var fmtString = style.getFormatString();
            if (fmtString != null)
            {
                return DateFormatUtils.isExcelDateFormat(fmtString);
            }
        }
        return false;
    }

    public Compile getCompile()
    {
        return compile;
    }

    public MemCell[][] getArray()
    {
        return array;
    }

    public List<MemCell> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies(List<MemCell> dependencies)
    {
        this.dependencies = dependencies;
    }

    public void clearInferred()
    {
        array = null;
        if (inferredFromCell != null)
        {
            stringValue = null;
            formulaValue = null;
            numberValue = null;
        }
    }
}
