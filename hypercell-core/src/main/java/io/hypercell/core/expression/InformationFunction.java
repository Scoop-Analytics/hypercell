/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.core.util.FormattingUtils;
import io.hypercell.core.dateparser.DateAnalyzer;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 */
public class InformationFunction extends Function
{
    public InformationFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
        if (type == HyperCellExpressionLexer.TABLETOKEN)
        {
            cc.setInformationalOnly(true);
        }
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
        if (type == HyperCellExpressionParser.ISNUMBERTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || mc.getNumberValue() == null || mc.getErrorValue() != null)
                return getReturn(new MemCell(0));
            return getReturn(new MemCell(mc.getNumberValue() != null ? 1 : 0));
        } else if (type == HyperCellExpressionParser.ISTEXTTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || mc.getStringValue() == null || mc.getErrorValue() != null)
                return getReturn(new MemCell(0));
            return getReturn(new MemCell(mc.getStringValue() != null ? 1 : 0));
        } else if (type == HyperCellExpressionParser.ISNONTEXTTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || mc.getStringValue() == null || mc.getErrorValue() != null)
                return getReturn(new MemCell(1));
            return getReturn(new MemCell(mc.getStringValue() != null ? 0 : 1));
        } else if (type == HyperCellExpressionParser.ISNATOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc != null && mc.getErrorValue() != null && mc.getErrorValue() == FormulaError.NA)
                return getReturn(new MemCell(1));
            return getReturn(new MemCell(0));
        } else if (type == HyperCellExpressionParser.ISERRTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc.getErrorValue() != null && mc.getErrorValue() != FormulaError.NA)
                return getReturn(new MemCell(1));
            return getReturn(new MemCell(0));
        } else if (type == HyperCellExpressionParser.ISERRORTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc.getErrorValue() != null)
                return getReturn(new MemCell(1));
            return getReturn(new MemCell(0));
        } else if (type == HyperCellExpressionParser.ISBLANKTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || (mc.getStringValue() == null && mc.getNumberValue() == null))
            {
                return getReturn(new MemCell(1));
            }
            return getReturn(new MemCell(0));
        } else if (type == HyperCellExpressionParser.ISDATETOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null)
            {
                return getReturn(new MemCell(0));
            }
            if (mc.getCellContext() != null && mc.getCellContext().isDate())
            {
                return getReturn(new MemCell(1));
            }
            if (mc.getFormatString() == null)
            {
                if (mc.getStringValue() != null)
                {
                    DateAnalyzer dateAnalyzer = new DateAnalyzer(mc.getStringValue());
                    return new MemCell(dateAnalyzer.isAValidDate() ? 1 : 0);
                }
                return getReturn(new MemCell(0));
            }
            if (FormattingUtils.isExcelDateFormat(mc.getFormatString()))
            {
                return getReturn(new MemCell(1));
            }
            return getReturn(new MemCell(0));
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
