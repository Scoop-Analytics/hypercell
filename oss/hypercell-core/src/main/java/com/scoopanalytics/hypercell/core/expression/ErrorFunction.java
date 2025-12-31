package com.scoopanalytics.hypercell.core.expression;

import com.scoopanalytics.hypercell.core.grid.FormulaError;
import com.scoopanalytics.hypercell.core.grid.MemCell;

public class ErrorFunction extends HyperCellExpression
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
