/**
 *
 */
package io.hypercell.core.expression;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.api.HyperCellException;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 *
 */
public class SheetNumber extends HyperCellExpression
{
    private boolean invalid = false;
    private Integer intValue;
    private Long longValue;
    private Double doubleValue;

    public SheetNumber(ParseTree tree) throws HyperCellException
    {
        String text = tree.getChild(0).getText();
        try
        {
            if (text.contains("E"))
            {
                doubleValue = Double.parseDouble(text);
            } else
            {
                NumberFormat nf = NumberFormat.getInstance();
                java.lang.Number n = nf.parse(text);
                if (n instanceof Integer)
                {
                    intValue = (Integer) n;
                } else if (n instanceof Long)
                {
                    longValue = (Long) n;
                } else if (n instanceof Double)
                {
                    doubleValue = (Double) n;
                }
            }
        } catch (ParseException e)
        {
            invalid = true;
            throw new HyperCellException("Unable to parse number: " + text);
        }
    }

    public SheetNumber(int value)
    {
        this.intValue = value;
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (intValue != null)
            return new MemCell(intValue);
        if (longValue != null)
            return new MemCell(longValue);
        if (doubleValue != null)
            return new MemCell(doubleValue);
        return new MemCell(FormulaError.NA);
    }

    @Override
    public String getMetricFormula()
    {
        if (invalid)
            return "";
        if (intValue != null)
            return intValue.toString();
        if (longValue != null)
            return longValue.toString();
        if (doubleValue != null)
            return doubleValue.toString();
        return "";
    }

    @Override
    public String getExcelFormula()
    {
        if (invalid)
            return "@#N/A";
        if (intValue != null)
            return intValue.toString();
        if (longValue != null)
            return longValue.toString();
        if (doubleValue != null)
        {
            return String.format("%e", doubleValue);
        }
        return "";
    }
}
