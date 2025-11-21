package io.hypercell.core.expression;

import io.hypercell.api.CellValue;
import io.hypercell.api.Expression;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

/**
 * Represents error values in expressions (e.g., #N/A, #VALUE!, #REF!)
 */
public class ErrorFunction implements Expression
{
    FormulaError formulaError;

    public ErrorFunction(FormulaError formulaError)
    {
        this.formulaError = formulaError;
    }

    @Override
    public CellValue evaluate()
    {
        return new MemCell(formulaError);
    }

    @Override
    public String getExcelFormula()
    {
        return formulaError.getDisplay();
    }

    @Override
    public String getMetricFormula()
    {
        return formulaError.getDisplay();
    }
}
