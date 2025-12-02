package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

public class ErrorFunction extends ScoopExpression
{
    FormulaError formulaError;

    public ErrorFunction(FormulaError formulaError)
    {
        this.formulaError = formulaError;
    }

    @Override
    public MemCell calculateCellValue()
    {
        return new MemCell(formulaError);
    }

    @Override
    public String getExcelFormula()
    {
        return formulaError.getDisplay();
    }

}
