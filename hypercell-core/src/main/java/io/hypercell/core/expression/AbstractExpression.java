package io.hypercell.core.expression;

import io.hypercell.api.Expression;
import io.hypercell.api.CellValue;

public abstract class AbstractExpression implements Expression {
    
    public abstract CellValue evaluate();

    public String getExcelFormula() {
        return "";
    }
}
