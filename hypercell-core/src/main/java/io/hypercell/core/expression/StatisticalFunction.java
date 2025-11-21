/**
 * Statistical functions for Excel compatibility (NORMDIST, NORMSDIST, etc.)
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.math3.distribution.NormalDistribution;

import io.hypercell.api.CellValue;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.formula.HyperCellExpressionParser;

/**
 * @author bradpeters
 */
public class StatisticalFunction extends BaseFunctionExpression
{
    public StatisticalFunction(ParseTree tree, CompileContext cc, FunctionRegistry registry)
    {
        super(tree, cc, registry);
    }

    @Override
    public CellValue evaluate()
    {
        if (type == HyperCellExpressionParser.NORMDISTTOKEN)
        {
            MemCell xmc = (MemCell)expressions.getFirst().evaluate();
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
                MemCell mean = (MemCell)expressions.get(1).evaluate();
                mn = mean.getNumberValue();
                MemCell stddev = (MemCell)expressions.get(2).evaluate();
                stdn = stddev.getNumberValue();
                MemCell cumulative = (MemCell)expressions.get(3).evaluate();
                cn = cumulative.getNumberValue();
                if (xn == null || mn == null || stdn == null || cn == null)
                    return new MemCell(FormulaError.VALUE);
            }
            NormalDistribution nd = new NormalDistribution(mn.doubleValue(), stdn.doubleValue());
            if (cn.doubleValue() > 0)
            {
                return new MemCell(nd.cumulativeProbability(xn.doubleValue()));
            } else
            {
                return new MemCell(nd.density(xn.doubleValue()));
            }
        } else if (type == HyperCellExpressionParser.NORMSDISTTOKEN)
        {
            MemCell xmc = (MemCell)expressions.get(0).evaluate();
            Number xn = xmc.getNumberValue();
            NormalDistribution nd = new NormalDistribution(0, 1);
            return new MemCell(nd.cumulativeProbability(xn.doubleValue()));
        }
        return null;
    }
}
