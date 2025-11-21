package io.hypercell.core.expression;
import java.util.stream.IntStream;

import io.hypercell.core.grid.MemCell;

import io.hypercell.core.grid.FormulaError;

import org.antlr.v4.runtime.tree.ParseTree;
import io.hypercell.api.RangeAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

public class BooleanArray extends AbstractExpression
{
    private String operator;
    private Range range;
    private io.hypercell.api.Expression exp;
    private io.hypercell.api.Expression left;
    private io.hypercell.api.Expression right;

    public BooleanArray(CompileContext cc, ParseTree tree, io.hypercell.api.FunctionRegistry registry)
    {
        if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.COMPAREARRAYContext)
        {
            range = new Range(cc.getSheet(), tree.getChild(0));
            operator = tree.getChild(1).getText();
            Compile c = new Compile(tree.getChild(2), cc, registry);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.BOOLEANARRAYOPContext)
        {
            operator = tree.getChild(1).getText();
            Compile c = new Compile(tree.getChild(0), cc, registry);
            left = c.getExpression();
            c = new Compile(tree.getChild(2), cc, registry);
            right = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.GROUPARRAYContext)
        {
            Compile c = new Compile(tree.getChild(1), cc, registry);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.NOTARRAYContext)
        {
            int a = 1;
        }
    }

    @Override
    public io.hypercell.api.CellValue evaluate()
    {
        if (operator != null && range == null)
        {
            
            io.hypercell.api.CellValue[][] leftResultArray = left.evaluate().getArrayValue();
            io.hypercell.api.CellValue[][] rightResultArray = right.evaluate().getArrayValue();
            if (leftResultArray == null || rightResultArray == null) return null;

            int rows = Math.min(leftResultArray.length, rightResultArray.length);
            io.hypercell.core.grid.MemCell[][] result = new io.hypercell.core.grid.MemCell[rows][];
            
            IntStream.range(0, rows).parallel().forEach(row -> {
                int cols = Math.min(leftResultArray[row].length, rightResultArray[row].length);
                result[row] = new io.hypercell.core.grid.MemCell[cols];
                for (int col = 0; col < cols; col++) {
                    io.hypercell.api.CellValue l = leftResultArray[row][col];
                    io.hypercell.api.CellValue r = rightResultArray[row][col];
                    boolean lVal = l != null && l.getNumberValue() != null && l.getNumberValue().doubleValue() > 0;
                    boolean rVal = r != null && r.getNumberValue() != null && r.getNumberValue().doubleValue() > 0;
                    
                    switch (operator) {
                        case "*":
                        case ",":
                            result[row][col] = new MemCell(lVal && rVal);
                            break;
                        case "+":
                            result[row][col] = new MemCell(lVal || rVal);
                            break;
                    }
                }
            });
            return new MemCell(result);

        } else if (range != null)
        {
            return new MemCell(FormulaError.NOT_IMPLEMENTED);
        } else
        {
            return (io.hypercell.core.grid.MemCell)exp.evaluate();
        }
    }
}
