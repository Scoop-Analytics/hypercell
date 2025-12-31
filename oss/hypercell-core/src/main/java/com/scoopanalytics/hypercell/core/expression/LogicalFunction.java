/**
 *
 */
package com.scoopanalytics.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;

import com.scoopanalytics.hypercell.core.grid.FormulaError;
import com.scoopanalytics.hypercell.core.grid.MemCell;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionLexer;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser;

/**
 * @author bradpeters
 */
public class LogicalFunction extends Function
{
    private SpillArea spillArea;

    public LogicalFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
        if (type == HyperCellExpressionParser.IFERRORTOKEN)
        {
            spillArea = expressions.getFirst().possibleSpillRange();
            if (spillArea != null && expressions.size() > 1)
            {
                spillArea = SpillArea.getLargestSpillArea(spillArea, expressions.get(1).possibleSpillRange());
            }
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
        MemCell memCellResult = null;
        if (type == HyperCellExpressionParser.IFTOKEN)
        {
            if (expressions.isEmpty())
                return new MemCell(FormulaError.NA);
            MemCell result = expressions.getFirst().calculateCellValue();
            if (result == null)
                return new MemCell(FormulaError.NA);
            Number n = result.getNumberValue();
            if (n == null)
                return new MemCell(FormulaError.VALUE);
            double val = n.doubleValue();
            if (val > 0)
                memCellResult = expressions.get(1).calculateCellValue();
            else
            {
                if (expressions.size() == 3)
                {
                    memCellResult = expressions.get(2).calculateCellValue();
                }
            }
        } else if (type == HyperCellExpressionParser.IFSTOKEN)
        {
            if (expressions.size() % 2 != 0)
            {
                memCellResult = new MemCell(FormulaError.NA);
            } else
            {
                for (int condition = 0; condition < expressions.size() / 2; condition++)
                {
                    MemCell result = expressions.get(condition * 2).calculateCellValue();
                    if (result == null)
                        return new MemCell(FormulaError.NA);
                    Number n = result.getNumberValue();
                    if (n == null)
                        memCellResult = new MemCell(FormulaError.VALUE);
                    double val = n == null ? 0 : n.doubleValue();
                    if (val > 0)
                    {
                        memCellResult = expressions.get(condition * 2 + 1).calculateCellValue();
                        break;
                    }
                }
            }
        } else if (type == HyperCellExpressionParser.IFERRORTOKEN)
        {
            MemCell result = expressions.get(0).calculateCellValue();
            if (expressions.size()  < 2 || expressions.get(1) == null)
                return new MemCell(FormulaError.NA);
            MemCell errorResult = expressions.get(1).calculateCellValue();
            if (result == null)
            {
                memCellResult = errorResult;
            } else if (result.getErrorValue() != null)
            {
                memCellResult = errorResult;
            } else
            {
                memCellResult = result;
            }
        } else if (type == HyperCellExpressionParser.IFNATOKEN)
        {
            MemCell result = expressions.get(0).calculateCellValue();
            MemCell naResult = expressions.get(1).calculateCellValue();
            if (result.getErrorValue() == FormulaError.NA)
            {
                memCellResult = naResult;
            } else
            {
                memCellResult = result;
            }
        } else if (type == HyperCellExpressionParser.TRUETOKEN)
        {
            memCellResult = new MemCell(1);
        } else if (type == HyperCellExpressionParser.FALSETOKEN)
        {
            memCellResult = new MemCell(0);
        } else if (type == HyperCellExpressionParser.EQTOKEN)
        {
            MemCell exp0 = expressions.getFirst().calculateCellValue();
            MemCell exp1 = expressions.getFirst().calculateCellValue();
            if (exp0 == null && exp1 == null)
                memCellResult = new MemCell(1);
            else if (exp0 == null || exp1 == null)
                memCellResult = new MemCell(0);
            else memCellResult = new MemCell(exp0.equals(exp1) ? 1 : 0);
        } else if (type == HyperCellExpressionLexer.ANDTOKEN || type == HyperCellExpressionLexer.ORTOKEN
                || type == HyperCellExpressionLexer.XORTOKEN)
        {
            boolean bresult = false;
            boolean first = true;
            for (HyperCellExpression exp : expressions)
            {
                MemCell result = exp.calculateCellValue();
                if (result == null)
                    return new MemCell(FormulaError.NA);
                Number n = result.getNumberValue();
                if (n == null)
                    return new MemCell(FormulaError.VALUE);
                double val = n.doubleValue();
                boolean newVal = val > 0;
                if (first)
                {
                    bresult = newVal;
                    first = false;
                } else
                {
                    if (type == HyperCellExpressionLexer.ANDTOKEN)
                        bresult = bresult && newVal;
                    else if (type == HyperCellExpressionLexer.ORTOKEN)
                        bresult = bresult || newVal;
                    else if (type == HyperCellExpressionLexer.XORTOKEN)
                        bresult = bresult ^ newVal;
                }
                if (type == HyperCellExpressionLexer.ANDTOKEN && !bresult)
                    break;
                if (type == HyperCellExpressionLexer.ORTOKEN && bresult)
                    break;
            }
            memCellResult = new MemCell(bresult ? 1 : 0);
        } else if (type == HyperCellExpressionLexer.NOTTOKEN)
        {
            MemCell result = expressions.getFirst().calculateCellValue();
            if (result == null)
                return new MemCell(FormulaError.NA);
            Number n = result.getNumberValue();
            if (n == null)
                return new MemCell(FormulaError.VALUE);
            double val = n.doubleValue();
            memCellResult = new MemCell(val > 0 ? 0 : 1);
        }
        if (memCellCalculationCache != null)
        {
            memCellCalculationCache.cacheValue(memCellResult);
        }
        return memCellResult;
    }

    @Override
    public SpillArea possibleSpillRange()
    {
        return spillArea;
    }
}
