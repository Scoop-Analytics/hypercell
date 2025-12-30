package io.hypercell.core.expression;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import io.hypercell.core.grid.MemCell;

public class UnaryOperator extends HyperCellExpression
{
    public HyperCellExpression exp;
    private boolean isMinus;

    public UnaryOperator(ParseTree op, ParseTree tree, CompileContext cc)
    {
        Compile c = new Compile(tree, cc);
        exp = c.getExpression();
        if (op instanceof TerminalNodeImpl)
        {
            Token t = ((TerminalNodeImpl) op).symbol;
            String tokenString = t.getText();
            if (tokenString.equals("-"))
            {
                isMinus = true;
            }
        }
    }

    @Override
    public MemCell calculateCellValue()
    {
        MemCell result = exp.calculateCellValue();
        if (isMinus)
        {
            if (result.getNumberValue() != null)
            {
                return new MemCell(-result.getNumberValue().doubleValue());
            }
        }
        return result;
    }

    @Override
    public String getExcelFormula()
    {
        if (isMinus)
        {
            return "-" + exp.getExcelFormula();
        }
        return "";
    }
}
