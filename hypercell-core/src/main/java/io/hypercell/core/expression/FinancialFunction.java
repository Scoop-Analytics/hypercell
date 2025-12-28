/**
 *
 */
package io.hypercell.core.expression;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.poi.ss.formula.functions.Irr;

import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 *
 */
public class FinancialFunction extends Function
{
    public FinancialFunction(ParseTree tree, CompileContext cc)
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
        if (type == ScoopExpressionLexer.IRRTOKEN)
        {
            double[] values = getValues((Range) expressions.get(0));
            if (expressions.size() > 1)
            {
                Number n = expressions.get(1).calculateCellValue().getNumberValue();
                if (n == null)
                {
                    return getReturn(new MemCell(Irr.irr(values)));
                }
                return getReturn(new MemCell(Irr.irr(values, n.doubleValue())));
            } else
            {
                return getReturn(new MemCell(Irr.irr(values)));
            }
        } else if (type == ScoopExpressionLexer.NPVTOKEN)
        {
            double[] values = getValues((Range) expressions.get(1));
            MemCell mc = expressions.get(0).calculateCellValue();
            if (mc == null)
            {
                return getReturn(new MemCell(FormulaError.VALUE));
            }
            Number n = mc.getNumberValue();
            if (n == null)
            {
                return getReturn(new MemCell(FormulaError.VALUE));
            }
            double irr = n.doubleValue();
            double result = 0;
            for (int i = 0; i < values.length; i++)
            {
                result += values[i] / Math.pow(1 + irr, i + 1);
            }
            return getReturn(new MemCell(result));
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

    private MemCell getReturn(MemCell result)
    {
        if (memCellCalculationCache != null)
        {
            memCellCalculationCache.cacheValue(result);
        }
        return result;
    }

}
