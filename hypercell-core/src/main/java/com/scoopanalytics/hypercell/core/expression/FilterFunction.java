/**
 *
 */
package com.scoopanalytics.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import com.scoopanalytics.hypercell.api.RangeAddress;
import com.scoopanalytics.hypercell.core.grid.FormulaError;
import com.scoopanalytics.hypercell.core.grid.MemCell;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser;

import java.util.*;

/**
 * @author bradpeters
 */
public class FilterFunction extends Function
{
    public FilterFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
    }

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
        if (type == HyperCellExpressionParser.FILTERTOKEN)
        {
            Range range = (Range) expressions.getFirst();
            if (range == null)
                return new MemCell(FormulaError.VALUE);
            if (range.getSheet() == null)
            {
                range.setSheet(cc.getSheet());
            }
            BooleanArray booleanArray = (BooleanArray) expressions.get(1);
            RangeAddress rangeAddress = range.getRangeAddress();
            int startRow, endRow, startCol, endCol;
            if (rangeAddress != null)
            {

                startRow = rangeAddress.getFirstRowNumber(cc.getSheet().getWorkbook());
                endRow = rangeAddress.getLastRowNumber(cc.getSheet().getWorkbook());
                startCol = rangeAddress.getFirstColumnNumber(cc.getSheet().getWorkbook());
                endCol = rangeAddress.getLastColumnNumber(cc.getSheet().getWorkbook());
            } else
            {
                startRow = range.getStartAddress().getRow();
                endRow = range.getEndAddress().getRow();
                startCol = range.getStartAddress().getColumn();
                endCol = range.getEndAddress().getColumn();
            }
            if (endRow < 0)
            {
                endRow = cc.getSheet().getNumRows();
            }
            MemCell booleanResult = booleanArray.calculateCellValue();
            if (booleanResult == null)
            {
                return new MemCell(FormulaError.NA);
            }
            if (booleanResult.getErrorValue() != null)
            {
                return new MemCell(booleanResult.getErrorValue());
            }
            List<MemCell[]> resultRows = new ArrayList<>();
            int count = 0;
            MemCell[][] booleanArrayResult = booleanResult.getArray();
            List<Range> booleanArrayRanges = booleanArray.getRanges();
            // Arrays must be same length
            if (booleanArrayResult == null || (rangeAddress != null && !rangeAddress.start.isNoRow() &&
                    (booleanArrayResult.length != (endRow - startRow))))
                return new MemCell(FormulaError.VALUE);
            if (!booleanArray.validateRanges(range, booleanArrayRanges))
            {
                return new MemCell(FormulaError.VALUE);
            }
            for (int i = startRow; i <= endRow && count < booleanArrayResult.length; i++)
            {
                if (!(booleanArrayResult[count++][0].getDoubleValue() > 0))
                {
                    continue;
                }
                MemCell[] cells = new MemCell[endCol - startCol + 1];
                for (int j = 0; j < cells.length; j++)
                {
                    cells[j] = range.getSheet().getCellAt(i, j + startCol);
                }
                resultRows.add(cells);
            }
            MemCell[][] resultArray = new MemCell[resultRows.size()][];
            resultRows.toArray(resultArray);
            return getReturn(new MemCell(resultArray));
        } else if (type == HyperCellExpressionParser.UNIQUETOKEN)
        {
            if (expressions.getFirst() instanceof Range range)
            {
                List<MemCell[]> resultRows = new ArrayList<>();
                Identifier startAddress = range.getStartAddress();
                Identifier endAddress = range.getEndAddress();
                Set<String> uniqueTokens = new LinkedHashSet<>();
                for (int i = startAddress.getRow(); i < endAddress.getRow(); i++)
                {
                    MemCell cell = range.getSheet().getCellAt(i, startAddress.getColumn());
                    if (cell != null && cell.getStringValue() != null)
                    {
                        uniqueTokens.add(cell.getStringValue());
                    }
                }
                MemCell[][] resultArray = new MemCell[uniqueTokens.size()][];
                int count = 0;
                for (String token : uniqueTokens)
                {
                    resultArray[count] = new MemCell[1];
                    resultArray[count][0] = new MemCell(token);
                    count++;
                }
                return getReturn(new MemCell(resultArray));
            } else
            {
                // Must be an expression which returns an array
                Set<String> uniqueTokens = new LinkedHashSet<>();
                MemCell result = expressions.getFirst().calculateCellValue();
                if (result == null)
                    return new MemCell(FormulaError.VALUE);
                var array = result.getArray();
                if (array == null)
                    return new MemCell(FormulaError.VALUE);
                for (MemCell[] memCells : array)
                {
                    for (MemCell cell : memCells)
                    {
                        if (cell != null && cell.getStringValue() != null)
                        {
                            uniqueTokens.add(cell.getStringValue());
                        }
                    }
                }
                MemCell[][] resultArray = new MemCell[uniqueTokens.size()][];
                int count = 0;
                for (String token : uniqueTokens)
                {
                    resultArray[count] = new MemCell[1];
                    resultArray[count][0] = new MemCell(token);
                    count++;
                }
                return getReturn(new MemCell(resultArray));
            }
        } else if (type == HyperCellExpressionParser.SORTTOKEN)
        {
            int sortIndex = 0;
            boolean ascending = true;
            if (expressions.size() > 1)
            {
                sortIndex = expressions.get(1).calculateCellValue().getNumberValue().intValue() - 1;
            }
            if (expressions.size() > 2)
            {
                ascending = expressions.get(2).calculateCellValue().getNumberValue().doubleValue() > 0;
            }
            if (expressions.getFirst() instanceof Range range)
            {
                List<MemCell[]> resultRows = new ArrayList<>();
                Identifier startAddress = range.getStartAddress();
                Identifier endAddress = range.getEndAddress();
                if (sortIndex > endAddress.getColumn() - startAddress.getColumn())
                    return new MemCell(FormulaError.VALUE);
                TreeMap<String, List<Integer>> rowMap = new TreeMap<>(new CaseInsensitiveComparator());
                for (int i = startAddress.getRow(); i <= endAddress.getRow(); i++)
                {
                    MemCell cell = range.getSheet().getCellAt(i, startAddress.getColumn() + sortIndex);
                    cell.calculate();
                    if (cell != null && cell.getStringValue() != null)
                    {
                        String token = cell.getStringValue();
                        rowMap.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
                    }
                }
                MemCell[][] resultArray = new MemCell[Math.abs(endAddress.getRow() - startAddress.getRow()) + 1][];
                int count = 0;
                int width = endAddress.getColumn() - startAddress.getColumn() + 1;
                for (String token : ascending ? rowMap.keySet() : rowMap.descendingKeySet())
                {
                    for (int row : rowMap.get(token))
                    {
                        resultArray[count] = new MemCell[width];
                        for (int col = 0; col < width; col++)
                        {
                            resultArray[count][col] = range.getSheet().getCellAt(row, startAddress.getColumn() + col);
                        }
                        count++;
                    }
                }
                return getReturn(new MemCell(resultArray));
            } else
            {
                // Must be an expression which returns an array
                Set<String> uniqueTokens = new LinkedHashSet<>();
                MemCell result = expressions.getFirst().calculateCellValue();
                if (result == null)
                    return new MemCell(FormulaError.VALUE);
                var array = result.getArray();
                if (array == null || array.length == 0 || sortIndex > array[0].length - 1)
                    return new MemCell(FormulaError.VALUE);
                for (MemCell[] memCells : array)
                {
                    for (int col = 0; col < memCells.length; col++)
                    {
                        MemCell cell = memCells[col];
                        if (cell != null && cell.getStringValue() != null)
                        {
                            uniqueTokens.add(cell.getStringValue());
                        }
                    }
                }
                MemCell[][] resultArray = new MemCell[uniqueTokens.size()][];
                int count = 0;
                for (String token : uniqueTokens)
                {
                    resultArray[count] = new MemCell[1];
                    resultArray[count][0] = new MemCell(token);
                    count++;
                }
                return getReturn(new MemCell(resultArray));
            }
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

    @Override
    public SpillArea possibleSpillRange()
    {
        if (type == HyperCellExpressionParser.FILTERTOKEN)
        {
            Range range = (Range) expressions.getFirst();
            SpillArea spillArea = new SpillArea();
            spillArea.width = range.getEndAddress().getColumn() - range.getStartAddress().getColumn() + 1;
            spillArea.height = -1;
            return spillArea;
        }
        return null;
    }
}
