package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import io.hypercell.api.RangeAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ExpressionAray extends HyperCellExpression
{
    private final List<HyperCellExpression> expressions = new ArrayList<>();

    public ExpressionAray(CompileContext cc, ParseTree tree)
    {
        for (int i = 1; i < tree.getChildCount(); i += 2)
        {
            var child = tree.getChild(i);
            Compile c = new Compile(child, cc);
            HyperCellExpression exp = c.getExpression();
            expressions.add(exp);
        }
    }

    @Override
    public MemCell calculateCellValue()
    {
        // Return 1 dimensional array
        MemCell[][] resultArray = new MemCell[1][expressions.size()];
        for (int i = 0; i < expressions.size(); i++)
        {
            resultArray[0][i] = expressions.get(i).calculateCellValue();
        }
        return new MemCell(resultArray);

    }
}
