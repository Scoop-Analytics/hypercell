/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import scoop.ScoopException;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * @author bradpeters
 *
 */
public class SheetConstant extends ScoopExpression
{
    private boolean isNA = false;

    public SheetConstant(ParseTree tree)
    {
        TerminalNodeImpl t = (TerminalNodeImpl) tree.getChild(0).getChild(0);
        String text = t.getText();
        if (text.equalsIgnoreCase("NA") || text.equals("@NA"))
        {
            isNA = true;
        }
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (isNA)
        {
            return new MemCell(FormulaError.NA);
        }
        return null;
    }

    @Override
    public String getMetricFormula()
    {
        if (isNA)
        {
            return "NA()";
        }
        return "";
    }

    @Override
    public String getExcelFormula()
    {
        if (isNA)
        {
            return "NA()";
        }
        return "";
    }
}
