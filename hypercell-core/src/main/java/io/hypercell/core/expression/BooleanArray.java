package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import io.hypercell.api.RangeAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BooleanArray extends ScoopExpression
{
    private String operator;
    private Range range;
    private ScoopExpression exp;
    private ScoopExpression left;
    private ScoopExpression right;

    public BooleanArray(CompileContext cc, ParseTree tree)
    {
        if (tree instanceof ScoopExpressionParser.COMPAREARRAYContext)
        {
            range = new Range(cc.getSheet(), tree.getChild(0));
            operator = tree.getChild(1).getText();
            Compile c = new Compile(tree.getChild(2), cc);
            exp = c.getExpression();
        } else if (tree instanceof ScoopExpressionParser.BOOLEANARRAYOPContext)
        {
            operator = tree.getChild(1).getText();
            Compile c = new Compile(tree.getChild(0), cc);
            left = c.getExpression();
            c = new Compile(tree.getChild(2), cc);
            right = c.getExpression();
        } else if (tree instanceof ScoopExpressionParser.GROUPARRAYContext)
        {
            Compile c = new Compile(tree.getChild(1), cc);
            exp = c.getExpression();
        } else if (tree instanceof ScoopExpressionParser.NOTARRAYContext)
        {
        }
    }

    public List<Range> getRanges()
    {
        if (range != null)
            return List.of(range);
        if (operator != null && range == null)
        {
            List<Range> result = new ArrayList<>();
            result.addAll(((BooleanArray) left).getRanges());
            result.addAll(((BooleanArray) right).getRanges());
            return result;
        } else if (exp instanceof BooleanArray)
        {
            return ((BooleanArray) exp).getRanges();
        }
        return new ArrayList<>();
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (operator != null && range == null)
        {
            if (left == null || right == null)
                return null;
            MemCell leftCell = left.calculateCellValue();
            MemCell rightCell = right.calculateCellValue();
            if (leftCell == null || rightCell == null)
                return null;
            if (leftCell.getErrorValue() != null)
                return new MemCell(leftCell.getErrorValue());
            if (rightCell.getErrorValue() != null)
                return new MemCell(rightCell.getErrorValue());
            MemCell[][] leftResultArray = leftCell.getArray();
            MemCell[][] rightResultArray = rightCell.getArray();
            MemCell[][] result = new MemCell[Math.min(leftResultArray.length, rightResultArray.length)][];
            IntStream.range(0, Math.min(leftResultArray.length, rightResultArray.length)).parallel()
                     .forEach(
                             row ->
                             {
                                 result[row] = new MemCell[Math.min(leftResultArray[row].length,
                                         rightResultArray[row].length)];
                                 for (int col = 0; col < leftResultArray[row].length && col < rightResultArray[row].length; col++)
                                 {
                                     switch (operator)
                                     {
                                         case "*":
                                         case ",":
                                             result[row][col] = new MemCell(
                                                     (leftResultArray[row][col] != null && leftResultArray[row][col].getDoubleValue() != null && leftResultArray[row][col].getDoubleValue() > 0) &&
                                                     (rightResultArray[row][col] != null && rightResultArray[row][col].getDoubleValue() != null && rightResultArray[row][col].getDoubleValue() > 0));
                                             break;
                                         case "+":
                                             result[row][col] = new MemCell(
                                                     (leftResultArray[row][col] != null && leftResultArray[row][col].getDoubleValue() != null && leftResultArray[row][col].getDoubleValue() > 0) ||
                                                     (rightResultArray[row][col] != null && rightResultArray[row][col].getDoubleValue() != null && rightResultArray[row][col].getDoubleValue() > 0));
                                             break;
                                     }
                                 }
                             }
                     );
            return new MemCell(result);
        } else if (range != null)
        {
            RangeAddress rangeAddress = range.getRangeAddress();
            int startRow = rangeAddress.getFirstRowNumber(range.getSheet().getWorkbook());
            int endRow = rangeAddress.getLastRowNumber(range.getSheet().getWorkbook());
            if (endRow < 0)
            {
                endRow = range.getSheet().getNumRows();
            }
            int startCol = rangeAddress.getFirstColumnNumber(range.getSheet().getWorkbook());
            int endCol = rangeAddress.getLastColumnNumber(range.getSheet().getWorkbook());
            // Boolean arrays can only be width 1
            if (endCol - startCol > 1)
                return new MemCell(FormulaError.VALUE);
            MemCell value = exp.calculateCellValue();
            MemCell[][] array = new MemCell[endRow - startRow + 1][endCol - startCol + 1];
            IntStream.range(startRow, endRow + 1).parallel().forEach(row ->
                    {
                        array[row - startRow] = new MemCell[endCol - startCol + 1];
                        for (int col = startCol; col <= endCol; col++)
                        {
                            array[row - startRow][col - startCol] = range.getSheet().getCellAt(row, col);
                            if (array[row - startRow][col - startCol] == null)
                                continue;
                            array[row - startRow][col - startCol].calculate();
                            if (array[row - startRow][col - startCol] != null)
                            {
                                if (operator.equals("="))
                                {
                                    array[row - startRow][col - startCol] = new MemCell(
                                            value.equals(array[row - startRow][col - startCol]));
                                }
                            }
                        }

                    }
            );
            return new MemCell(array);
        } else
        {
            if (exp == null)
                return null;
            return exp.calculateCellValue();
        }
    }

    public boolean validateRanges(Range range, List<Range> ranges)
    {
        if (range.isTableArray())
        {
            for (var r : ranges)
            {
                if (!r.isTableArray())
                    return false;
            }
        } else
        {
            for (var r : ranges)
            {
                if (Math.abs(r.getEndAddress().getRow() - r.getStartAddress().getRow()) != Math.abs(
                        range.getEndAddress().getRow() - range.getStartAddress().getRow()))
                {
                    return false;
                }
            }
        }
        for (var r : ranges)
        {
            if (Math.abs(r.getEndAddress().getColumn() - r.getStartAddress().getColumn()) != 0)
                return false;
        }
        return true;
    }

}
