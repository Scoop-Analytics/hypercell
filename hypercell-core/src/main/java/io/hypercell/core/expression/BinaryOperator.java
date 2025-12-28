/**
 *
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 */
public class BinaryOperator extends ScoopExpression
{
    public String op;
    public ScoopExpression left;
    public ScoopExpression right;

    public BinaryOperator(ParseTree ltree, ParseTree optree, ParseTree rtree, CompileContext cc)
    {
        Token t = ((TerminalNodeImpl) optree.getChild(0)).symbol;
        op = t.getText();
        Compile c = new Compile(ltree, cc);
        left = c.getExpression();
        c = new Compile(rtree, cc);
        right = c.getExpression();
    }

    private boolean isNumeric()
    {
        return "+-*/^%".contains(op);
    }

    @Override
    public MemCell calculateCellValue()
    {
        MemCell leftmc = left.calculateCellValue();
        MemCell rightmc = right.calculateCellValue();
        if (leftmc != null && leftmc.getErrorValue() != null)
        {
            return leftmc;
        }
        if (rightmc != null && rightmc.getErrorValue() != null)
        {
            return rightmc;
        }
        if (isNumeric())
        {
            Number leftn = leftmc == null ? 0 : leftmc.getNumberValue();
            Number rightn = rightmc == null ? 0 : rightmc.getNumberValue();
            if (leftn == null)
            {
                if (leftmc.getStringValue() != null)
                    return new MemCell(FormulaError.VALUE);
                leftn = 0;
            }
            if (rightn == null)
            {
                if (rightmc.getStringValue() != null)
                    return new MemCell(FormulaError.VALUE);
                rightn = 0;
            }
            switch (op)
            {
                case "+":
                    return new MemCell(leftn.doubleValue() + rightn.doubleValue());
                case "-":
                    return new MemCell(leftn.doubleValue() - rightn.doubleValue());
                case "*":
                    return new MemCell(leftn.doubleValue() * rightn.doubleValue());
                case "/":
                    if (rightn.doubleValue() == 0)
                        return new MemCell(FormulaError.DIV0);
                    return new MemCell(leftn.doubleValue() / rightn.doubleValue());
                case "%":
                    return new MemCell(leftn.doubleValue() % rightn.doubleValue());
                case "^":
                    return new MemCell(Math.pow(leftn.doubleValue(), rightn.doubleValue()));
            }
        } else if (op.equals("&"))
        {
            String s1 = Function.getStringValue(leftmc, true);
            String s2 = Function.getStringValue(rightmc, true);
            return new MemCell(s1 + s2);
        }
        if (leftmc == null)
            leftmc = new MemCell(0);
        if (rightmc == null)
            rightmc = new MemCell(0);
        int comp = leftmc.compareTo(rightmc);
        return switch (op)
        {
            case "=" -> new MemCell(comp == 0 ? 1 : 0);
            case ">" -> new MemCell(comp > 0 ? 1 : 0);
            case "<" -> new MemCell(comp < 0 ? 1 : 0);
            case ">=" -> new MemCell(comp >= 0 ? 1 : 0);
            case "<=" -> new MemCell(comp <= 0 ? 1 : 0);
            case "<>" -> new MemCell(comp != 0 ? 1 : 0);
            default -> null;
        };
    }

    @Override
    public String getExcelFormula()
    {
        return switch (op)
        {
            case "+" -> left.getExcelFormula() + "+" + right.getExcelFormula();
            case "-" -> left.getExcelFormula() + "-" + right.getExcelFormula();
            case "*" -> left.getExcelFormula() + "*" + right.getExcelFormula();
            case "/" -> left.getExcelFormula() + "/" + right.getExcelFormula();
            case "%" -> left.getExcelFormula() + "%" + right.getExcelFormula();
            case "=" -> left.getExcelFormula() + "=" + right.getExcelFormula();
            case ">" -> left.getExcelFormula() + ">" + right.getExcelFormula();
            case "<" -> left.getExcelFormula() + "<" + right.getExcelFormula();
            case ">=" -> left.getExcelFormula() + ">=" + right.getExcelFormula();
            case "<=" -> left.getExcelFormula() + "<=" + right.getExcelFormula();
            case "<>" -> left.getExcelFormula() + "<>" + right.getExcelFormula();
            default -> super.getExcelFormula();
        };
    }

    @Override
    public String getMetricFormula()
    {
        return switch (op)
        {
            case "+" -> left.getMetricFormula() + "+" + right.getMetricFormula();
            case "-" -> left.getMetricFormula() + "-" + right.getMetricFormula();
            case "*" -> left.getMetricFormula() + "*" + right.getMetricFormula();
            case "/" -> left.getMetricFormula() + "/" + right.getMetricFormula();
            case "%" -> left.getMetricFormula() + "%" + right.getMetricFormula();
            case "=" -> left.getMetricFormula() + "=" + right.getMetricFormula();
            case ">" -> left.getMetricFormula() + ">" + right.getMetricFormula();
            case "<" -> left.getMetricFormula() + "<" + right.getMetricFormula();
            case ">=" -> left.getMetricFormula() + ">=" + right.getMetricFormula();
            case "<=" -> left.getMetricFormula() + "<=" + right.getMetricFormula();
            case "<>" -> left.getMetricFormula() + "<>" + right.getMetricFormula();
            default -> super.getMetricFormula();
        };
    }
}
