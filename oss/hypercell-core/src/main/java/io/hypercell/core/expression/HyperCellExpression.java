/**
 * Base class for all expression types in HyperCell.
 */
package io.hypercell.core.expression;

import io.hypercell.core.grid.MemCell;

/**
 * Abstract base class for compiled expressions that can be evaluated.
 */
public abstract class HyperCellExpression
{
    public abstract MemCell calculateCellValue();

    public String getMetricFormula()
    {
        return "";
    }

    public String getExcelFormula()
    {
        return "";
    }

    public Number getNumberValue()
    {
        MemCell mc = calculateCellValue();
        if (mc == null)
            return null;
        return mc.getNumberValue();
    }

    public int getIntValue()
    {
        Number n = getNumberValue();
        if (n == null)
            return 0;
        return n.intValue();
    }

    public double getDoubleValue()
    {
        Number n = getNumberValue();
        if (n == null)
            return 0;
        return n.doubleValue();
    }

    public SpillArea possibleSpillRange()
    {
        return null;
    }
}
