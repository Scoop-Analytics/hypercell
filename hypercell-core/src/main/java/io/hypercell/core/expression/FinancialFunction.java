/**
 * Financial functions for Excel compatibility (IRR, NPV, etc.)
 */
package io.hypercell.core.expression;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.poi.ss.formula.functions.Irr;

import io.hypercell.api.CellAddress;
import io.hypercell.api.CellValue;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.formula.HyperCellExpressionLexer;

/**
 * @author bradpeters
 */
public class FinancialFunction extends BaseFunctionExpression
{
    public FinancialFunction(ParseTree tree, CompileContext cc, FunctionRegistry registry)
    {
        super(tree, cc, registry);
    }

    @Override
    public CellValue evaluate()
    {
        if (type == HyperCellExpressionLexer.IRRTOKEN)
        {
            double[] values = getValues((Range) expressions.get(0));
            if (expressions.size() > 1)
            {
                Number n = ((MemCell)expressions.get(1).evaluate()).getNumberValue();
                if (n == null)
                {
                    return new MemCell(Irr.irr(values));
                }
                return new MemCell(Irr.irr(values, n.doubleValue()));
            } else
            {
                return new MemCell(Irr.irr(values));
            }
        } else if (type == HyperCellExpressionLexer.NPVTOKEN)
        {
            double[] values = getValues((Range) expressions.get(1));
            MemCell mc = (MemCell)expressions.get(0).evaluate();
            if (mc == null)
            {
                return new MemCell(FormulaError.VALUE);
            }
            Number n = mc.getNumberValue();
            if (n == null)
            {
                return new MemCell(FormulaError.VALUE);
            }
            double irr = n.doubleValue();
            double result = 0;
            for (int i = 0; i < values.length; i++)
            {
                result += values[i] / Math.pow(1 + irr, i + 1);
            }
            return new MemCell(result);
        }
        return null;
    }

    private double[] getValues(Range r)
    {
        List<CellAddress> addresses = r.getAddresses();
        double[] values = new double[addresses.size()];
        for (int i = 0; i < values.length; i++)
        {
            CellAddress a = addresses.get(i);
            java.lang.Number n = cc.getSheet().getCellAt(a.row, a.column).getNumberValue();
            if (n != null)
            {
                values[i] = n.doubleValue();
            }
        }
        return values;
    }
}
