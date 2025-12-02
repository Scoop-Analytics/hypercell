/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import org.antlr.v4.runtime.tree.ParseTree;
import org.jsoup.select.Evaluator;
import scoop.ScoopException;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;

import java.util.*;

/**
 * @author bradpeters
 */
public class LookupFunction extends Function
{
    public LookupFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
    }

    private Map<String, Integer> stringIndexMap = null;

    @Override
    public MemCell calculateCellValue()
    {
        if (memCellCalculationCache != null)
        {
            var cacheValue = memCellCalculationCache.getValue();
            if (cacheValue != null)
            {
                return cacheValue;
            }
        }
        if (type == HyperCellExpressionLexer.HLOOKUPTOKEN || type == HyperCellExpressionLexer.VLOOKUPTOKEN)
        {
            boolean hLookup = type == HyperCellExpressionLexer.HLOOKUPTOKEN;
            MemCell lv = expressions.getFirst().calculateCellValue();
            if (lv == null) return new MemCell(FormulaError.NA);
            Range r = (Range) expressions.get(1);
            Identifier start = r.getStartAddress();
            Identifier end = r.getEndAddress();
            int rowStart = start.getRow();
            int colStart = start.getColumn();
            int rowEnd = end.getRow();
            int colEnd = end.getColumn();
            MemCell offsetCell = expressions.get(2).calculateCellValue();
            int offset = (int) offsetCell.getNumberValue().intValue() - 1;
            int startIndex = hLookup ? colStart : rowStart;
            int endIndex = hLookup ? colEnd : rowEnd;
            boolean sortedRange = true;
            if (expressions.size() > 3)
            {
                sortedRange = isSortedRangeLookup(expressions.get(3).calculateCellValue());
            }
            int lastRow = -1;
            int lastCol = -1;
            boolean found = false;
            if (cc.getSheet() != null && cc.getSheet().getWorkbook().isUseIndices())
            {
                if (stringIndexMap == null)
                {
                    stringIndexMap = new HashMap<>();
                    for (int index = startIndex; index <= endIndex; index++)
                    {
                        int sheetRow = 0;
                        int sheetCol = 0;
                        if (!hLookup)
                        {
                            sheetRow = index - rowStart;
                        } else
                        {
                            sheetCol = index - colStart;
                        }
                        MemCell key = start.calculateCellValue(sheetRow, sheetCol);
                        if (key != null)
                        {
                            String value = key.getStringValue();
                            if (value == null)
                            {
                                Number n = key.getNumberValue();
                                if (n != null)
                                {
                                    value = Double.toString(n.doubleValue());
                                }
                            }
                            if (!stringIndexMap.containsKey(value))
                            {
                                // Only put if not there already - keeps first value in the map
                                stringIndexMap.put(value, index);
                            }
                        }
                    }
                }
                String lvValue = lv.getStringValue();
                if (lvValue == null)
                {
                    Number n = lv.getNumberValue();
                    if (n != null)
                    {
                        lvValue = Double.toString(n.doubleValue());
                    }
                }
                Integer index = stringIndexMap.get(lvValue);
                if (index != null)
                {
                    int sheetRow = 0;
                    int sheetCol = 0;
                    int rowOffset = 0;
                    int colOffset = 0;
                    if (!hLookup)
                    {
                        sheetRow = index - rowStart;
                        colOffset = offset;
                    } else
                    {
                        sheetCol = index - colStart;
                        rowOffset = offset;
                    }
                    return getReturn(start.calculateCellValue(sheetRow + rowOffset, sheetCol + colOffset));
                } else if (!sortedRange)
                {
                    return getReturn(new MemCell(FormulaError.NA));
                }
            }
            if (!found)
            {
                for (int index = startIndex; index <= endIndex; index++)
                {
                    int sheetRow = 0;
                    int sheetCol = 0;
                    int rowOffset = 0;
                    int colOffset = 0;
                    if (!hLookup)
                    {
                        sheetRow = index - rowStart;
                        colOffset = offset;
                    } else
                    {
                        sheetCol = index - colStart;
                        rowOffset = offset;
                    }
                    MemCell key = start.calculateCellValue(sheetRow, sheetCol);
                    if (key != null)
                    {
                        int compare = key.compareTo(lv);
                        if (compare == 0)
                        {
                            return start.calculateCellValue(sheetRow + rowOffset, sheetCol + colOffset);
                        }
                        if (sortedRange)
                        {
                            if (compare > 0)
                            {
                                if (lastRow >= 0)
                                {
                                    return start.calculateCellValue(lastRow, lastCol);
                                } else
                                {
                                    return new MemCell(FormulaError.NA);
                                }
                            }
                        }
                    }
                    lastRow = sheetRow + rowOffset;
                    lastCol = sheetCol + colOffset;
                }
            }
            if (sortedRange)
            {
                return getReturn(start.calculateCellValue(lastRow, lastCol));
            }
            return getReturn(new MemCell(FormulaError.NA));
        } else if (type == HyperCellExpressionLexer.XLOOKUPTOKEN)
        {
            MemCell lv = expressions.get(0).calculateCellValue();
            if (lv == null) return new MemCell(FormulaError.NA);
            Range r = (Range) expressions.get(1);
            Identifier start = r.getStartAddress();
            Identifier end = r.getEndAddress();
            int rowStart = start.getRow();
            int colStart = start.getColumn();
            int rowEnd = end.getRow();
            int colEnd = end.getColumn();
            Range targetRange = (Range) expressions.get(2);
            Identifier targetStart = targetRange.getStartAddress();
            MemCell notFoundCell = null;
            if (expressions.size() >= 4)
            {
                notFoundCell = expressions.get(3).calculateCellValue();
            }
            for (int row = 0; row <= rowEnd - rowStart; row++)
            {
                for (int col = 0; col <= colEnd - colStart; col++)
                {
                    MemCell key = start.calculateCellValue(r.getSheet(), row, col);
                    if (key != null)
                    {
                        if (key.equals(lv))
                        {
                            MemCell target = targetStart.calculateCellValue(targetRange.getSheet(), row, col);
                            if (target != null)
                            {
                                return target;
                            } else
                            {
                                if (notFoundCell != null)
                                {
                                    return notFoundCell;
                                } else
                                {
                                    return new MemCell(FormulaError.NA);
                                }
                            }
                        }
                    }
                }
            }
            if (notFoundCell != null)
            {
                return getReturn(notFoundCell);
            } else
            {
                return getReturn(new MemCell(FormulaError.NA));
            }
        } else if (type == HyperCellExpressionParser.CHOOSETOKEN)
        {
            MemCell position = expressions.get(0).calculateCellValue();
            if (position == null || position.getNumberValue() == null) return getReturn(new MemCell(FormulaError.NA));
            int pos = position.getNumberValue().intValue() - 1;
            if (pos >= expressions.size() - 1) return getReturn(new MemCell(FormulaError.NA));
            return getReturn(expressions.get(pos + 1).calculateCellValue());
        } else if (type == HyperCellExpressionParser.SWITCHTOKEN)
        {
            MemCell position = expressions.getFirst().calculateCellValue();
            MemCell defaultValue = null;
            if (expressions.size() % 2 == 0)
            {
                // Default Value
                defaultValue = expressions.getLast().calculateCellValue();
            }
            if (position == null || position.getNumberValue() == null)
            {
                return getReturn(defaultValue != null ? defaultValue : new MemCell(FormulaError.NA));
            }
            MemCell foundValue = null;
            for (int index = 1; index < expressions.size(); index += 2)
            {
                MemCell test = expressions.get(index).calculateCellValue();
                if (test != null && test.equals(position))
                {
                    foundValue = expressions.get(index + 1).calculateCellValue();
                    break;
                }
            }
            if (foundValue == null)
            {
                return getReturn(defaultValue != null ? defaultValue : new MemCell(FormulaError.NA));
            }
            return getReturn(foundValue);
        } else if (type == HyperCellExpressionParser.MATCHTOKEN)
        {
            if (expressions.size() < 2) return getReturn(new MemCell(FormulaError.NA));
            MemCell lv = expressions.get(0).calculateCellValue();
            if (lv == null) return getReturn(new MemCell(FormulaError.NA));
            ScoopExpression exp = expressions.get(1);
            int type = 1;
            if (expressions.size() == 3)
            {
                Number n = expressions.get(2).calculateCellValue().getNumberValue();
                if (n == null) return getReturn(new MemCell(FormulaError.NA));
                type = n.intValue();
            }
            Range r = null;
            MemCell[][] booleanArrayResult = null;
            if (exp instanceof Range)
            {
                r = (Range) exp;
            } else if (exp instanceof Identifier identifier)
            {
                r = new Range(identifier, identifier);
            } else if (exp instanceof BooleanArray booleanArray)
            {
                MemCell booleanResult = booleanArray.calculateCellValue();
                booleanArrayResult = booleanResult.getArray();
                r = new Range(cc.getSheet(), new CellAddress(0, 0),
                        new CellAddress(booleanArrayResult[0].length, booleanArrayResult.length));
            } else if (exp instanceof ExpressionAray expressionAray)
            {
                MemCell arrayResult = expressionAray.calculateCellValue();
                r = new Range(arrayResult.getArray());
            } else
            {
                return null;
            }
            int typeCount = 0;
            Identifier start = r.getStartAddress();
            Identifier end = r.getEndAddress();
            int rowStart = start.getRow();
            int colStart = start.getColumn();
            int rowEnd = end.getRow();
            int colEnd = end.getColumn();
            int starti = 0;
            int endi = 0;
            boolean useRow = false;
            if (colStart == colEnd)
            {
                starti = rowStart;
                endi = rowEnd;
                useRow = true;
            } else if (rowStart == rowEnd)
            {
                starti = colStart;
                endi = colEnd;
            }
            typeCount += endi - starti;
            for (int index = 0; index <= endi - starti; index++)
            {
                MemCell val = null;
                if (booleanArrayResult == null)
                {
                    val = start.calculateCellValue(useRow ? index : 0, useRow ? 0 : index);
                } else
                {
                    val = booleanArrayResult[0][index];
                }
                if (val == null) continue;
                if (type == 1)
                {
                    if (lv.compareTo(val) <= 0)
                    {
                        if (index == 0 && !lv.equals(val))
                        {
                            return getReturn(new MemCell(FormulaError.NA));
                        }
                        return getReturn(new MemCell(index));
                    }
                } else if (type == 0)
                {
                    if (lv.equals(val))
                    {
                        return getReturn(new MemCell(index + 1));
                    }
                } else if (type == -1)
                {
                    int compare = lv.compareTo(val);
                    if (compare > 0 && index == 0)
                    {
                        return getReturn(new MemCell(FormulaError.NA));
                    } else if (compare >= 0)
                    {
                        return getReturn(new MemCell(index));
                    }
                }
            }

            if (type == 1)
            {
                return getReturn(new MemCell(typeCount));
            }
            return getReturn(new MemCell(FormulaError.NA));
        } else if (type == HyperCellExpressionParser.INDEXTOKEN)
        {
            Range r = (Range) expressions.get(0);
            Identifier start = r.getStartAddress();
            MemCell row = expressions.get(1).calculateCellValue();
            if (row == null || (row.getNumberValue() != null && row.getNumberValue().doubleValue() < 0))
                return getReturn(new MemCell(FormulaError.NA));
            MemCell col = null;
            if (expressions.size() == 3)
            {
                col = expressions.get(2).calculateCellValue();
                if (col == null) return getReturn(new MemCell(FormulaError.NA));
            }
            MemCell mc = start.calculateCellValue(Math.max(0, row.getNumberValue().intValue() - 1),
                    col == null ? 0 : col.getNumberValue().intValue() - 1);
            return getReturn(Objects.requireNonNullElseGet(mc, () -> new MemCell(FormulaError.NA)));
        }
        return null;
    }

    private MemCell getReturn(MemCell result)
    {
        if (memCellCalculationCache != null)
        {
            memCellCalculationCache.cacheValue(result);
        }
        return result;
    }

    private boolean isSortedRangeLookup(MemCell value)
    {
        if (value == null) return false;
        java.lang.Number n = value.getNumberValue();
        if (n != null)
        {
            return n.doubleValue() > 0;
        }
        return false;
    }
}
