/**
 *
 */
package io.hypercell.core.expression;

import scoop.worksheet.MemCellCalculationCache;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 */
public abstract class ScoopExpression
{
    public MemCellCalculationCache memCellCalculationCache = null;

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
