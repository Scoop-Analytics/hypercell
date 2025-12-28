/**
 *
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.math3.distribution.NormalDistribution;

import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 *
 */
public class StatisticalFunction extends Function
{
    public StatisticalFunction(ParseTree tree, CompileContext cc)
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
        if (type == ScoopExpressionParser.NORMDISTTOKEN)
        {
            MemCell xmc = expressions.getFirst().calculateCellValue();
            Number xn = xmc.getNumberValue();
            Number mn = null;
            Number stdn = null;
            Number cn = null;
            if (expressions.size() == 1)
            {
                mn = 0;
                stdn = 1;
                cn = 0;
            } else
            {
                MemCell mean = expressions.get(1).calculateCellValue();
                mn = mean.getNumberValue();
                MemCell stddev = expressions.get(2).calculateCellValue();
                stdn = stddev.getNumberValue();
                MemCell cumulative = expressions.get(3).calculateCellValue();
                cn = cumulative.getNumberValue();
                if (xn == null || mn == null || stdn == null || cn == null)
                    return getReturn(new MemCell(FormulaError.VALUE));
            }
            NormalDistribution nd = new NormalDistribution(mn.doubleValue(), stdn.doubleValue());
            if (cn.doubleValue() > 0)
            {
                return getReturn(new MemCell(nd.cumulativeProbability(xn.doubleValue())));
            } else
            {
                return getReturn(new MemCell(nd.density(xn.doubleValue())));
            }
        } else if (type == ScoopExpressionParser.NORMSDISTTOKEN)
        {
            MemCell xmc = expressions.get(0).calculateCellValue();
            Number xn = xmc.getNumberValue();
            NormalDistribution nd = new NormalDistribution(0, 1);
            return getReturn(new MemCell(nd.cumulativeProbability(xn.doubleValue())));
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

}
