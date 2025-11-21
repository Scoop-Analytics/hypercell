/**
 * Information functions for Excel compatibility (ISNUMBER, ISTEXT, ISBLANK, ISERROR, etc.)
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.api.CellValue;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.FormattingUtils;
import io.hypercell.core.grid.MemCell;
import io.hypercell.formula.HyperCellExpressionLexer;
import io.hypercell.formula.HyperCellExpressionParser;

/**
 * @author bradpeters
 */
public class InformationFunction extends BaseFunctionExpression
{
    public InformationFunction(ParseTree tree, CompileContext cc, FunctionRegistry registry)
    {
        super(tree, cc, registry);
        if (type == HyperCellExpressionLexer.TABLETOKEN)
        {
            cc.setInformationalOnly(true);
        }
    }

    @Override
    public CellValue evaluate()
    {
        if (type == HyperCellExpressionParser.ISNUMBERTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc == null || mc.getNumberValue() == null || mc.getErrorValue() != null)
                return new MemCell(0);
            return new MemCell(mc.getNumberValue() != null ? 1 : 0);
        } else if (type == HyperCellExpressionParser.ISTEXTTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc == null || mc.getStringValue() == null || mc.getErrorValue() != null)
                return new MemCell(0);
            return new MemCell(mc.getStringValue() != null ? 1 : 0);
        } else if (type == HyperCellExpressionParser.ISNONTEXTTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc == null || mc.getStringValue() == null || mc.getErrorValue() != null)
                return new MemCell(1);
            return new MemCell(mc.getStringValue() != null ? 0 : 1);
        } else if (type == HyperCellExpressionParser.ISNATOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc != null && mc.getErrorValue() != null && mc.getErrorValue() == FormulaError.NA)
                return new MemCell(1);
            return new MemCell(0);
        } else if (type == HyperCellExpressionParser.ISERRTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc.getErrorValue() != null && mc.getErrorValue() != FormulaError.NA)
                return new MemCell(1);
            return new MemCell(0);
        } else if (type == HyperCellExpressionParser.ISERRORTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc.getErrorValue() != null)
                return new MemCell(1);
            return new MemCell(0);
        } else if (type == HyperCellExpressionParser.ISBLANKTOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc == null || (mc.getStringValue() == null && mc.getNumberValue() == null))
            {
                return new MemCell(1);
            }
            return new MemCell(0);
        } else if (type == HyperCellExpressionParser.ISDATETOKEN)
        {
            MemCell mc = (MemCell)expressions.getFirst().evaluate();
            if (mc == null)
            {
                return new MemCell(0);
            }
            if (mc.getCellContext() != null && mc.getCellContext().isDate())
            {
                return new MemCell(1);
            }
            if (mc.getFormatString() == null)
            {
                if (mc.getStringValue() != null)
                {
                    DateAnalyzer dateAnalyzer = new DateAnalyzer(mc.getStringValue());
                    return new MemCell(dateAnalyzer.isAValidDate() ? 1 : 0);
                }
                return new MemCell(0);
            }
            if (FormattingUtils.isExcelDateFormat(mc.getFormatString()))
            {
                return new MemCell(1);
            }
            return new MemCell(0);
        }
        return null;
    }
}
