/**
 *
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 */
public class LogicalFunction extends BaseFunctionExpression
{
    public LogicalFunction(ParseTree tree, CompileContext cc, io.hypercell.api.FunctionRegistry registry)
    {
        super(tree, cc, registry);
    }

    @Override
    public io.hypercell.api.CellValue evaluate()
    {
        MemCell memCellResult = null;
        if (type == io.hypercell.formula.HyperCellExpressionParser.IFTOKEN)
        {
            MemCell result = (MemCell)expressions.getFirst().evaluate();
            if (result == null)
                return new MemCell(FormulaError.NA);
            Number n = result.getNumberValue();
            if (n == null)
                return new MemCell(FormulaError.VALUE);
            double val = n.doubleValue();
            if (val > 0)
                memCellResult = (MemCell)expressions.get(1).evaluate();
            else
            {
                if (expressions.size() == 3)
                {
                    memCellResult = (MemCell)expressions.get(2).evaluate();
                }
            }
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.IFSTOKEN)
        {
            if (expressions.size() % 2 != 0)
            {
                memCellResult = new MemCell(FormulaError.NA);
            } else
            {
                for (int condition = 0; condition < expressions.size() / 2; condition++)
                {
                    MemCell result = (MemCell)expressions.get(condition * 2).evaluate();
                    if (result == null)
                        return new MemCell(FormulaError.NA);
                    Number n = result.getNumberValue();
                    if (n == null)
                        memCellResult = new MemCell(FormulaError.VALUE);
                    double val = n.doubleValue();
                    if (val > 0)
                    {
                        memCellResult = (MemCell)expressions.get(condition * 2 + 1).evaluate();
                        break;
                    }
                }
            }
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.IFERRORTOKEN)
        {
            MemCell result = (MemCell)expressions.get(0).evaluate();
            MemCell errorResult = (MemCell)expressions.get(1).evaluate();
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
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.IFNATOKEN)
        {
            MemCell result = (MemCell)expressions.get(0).evaluate();
            MemCell naResult = (MemCell)expressions.get(1).evaluate();
            if (result.getErrorValue() == FormulaError.NA)
            {
                memCellResult = naResult;
            } else
            {
                memCellResult = result;
            }
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.TRUETOKEN)
        {
            memCellResult = new MemCell(1);
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.FALSETOKEN)
        {
            memCellResult = new MemCell(0);
        } else if (type == io.hypercell.formula.HyperCellExpressionParser.EQTOKEN)
        {
            MemCell exp0 = (MemCell)expressions.getFirst().evaluate();
            MemCell exp1 = (MemCell)expressions.getFirst().evaluate();
            if (exp0 == null && exp1 == null)
                memCellResult = new MemCell(1);
            else if (exp0 == null || exp1 == null)
                memCellResult = new MemCell(0);
            else memCellResult = new MemCell(exp0.equals(exp1) ? 1 : 0);
        } else if (type == io.hypercell.formula.HyperCellExpressionLexer.ANDTOKEN || type == io.hypercell.formula.HyperCellExpressionLexer.ORTOKEN
                || type == io.hypercell.formula.HyperCellExpressionLexer.XORTOKEN)
        {
            boolean bresult = false;
            boolean first = true;
            for (io.hypercell.api.Expression exp : expressions)
            {
                MemCell result = (MemCell)exp.evaluate();
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
                    if (type == io.hypercell.formula.HyperCellExpressionLexer.ANDTOKEN)
                        bresult = bresult && newVal;
                    else if (type == io.hypercell.formula.HyperCellExpressionLexer.ORTOKEN)
                        bresult = bresult || newVal;
                    else if (type == io.hypercell.formula.HyperCellExpressionLexer.XORTOKEN)
                        bresult = bresult ^ newVal;
                }
                if (type == io.hypercell.formula.HyperCellExpressionLexer.ANDTOKEN && !bresult)
                    break;
                if (type == io.hypercell.formula.HyperCellExpressionLexer.ORTOKEN && bresult)
                    break;
            }
            memCellResult = new MemCell(bresult ? 1 : 0);
        } else if (type == io.hypercell.formula.HyperCellExpressionLexer.NOTTOKEN)
        {
            MemCell result = (MemCell)expressions.getFirst().evaluate();
            if (result == null)
                return new MemCell(FormulaError.NA);
            Number n = result.getNumberValue();
            if (n == null)
                return new MemCell(FormulaError.VALUE);
            double val = n.doubleValue();
            memCellResult = new MemCell(val > 0 ? 0 : 1);
        }
        return memCellResult;
    }

}
