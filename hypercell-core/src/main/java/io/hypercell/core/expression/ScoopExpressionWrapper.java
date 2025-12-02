package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import io.hypercell.api.Expression;
import io.hypercell.api.CellValue;
import io.hypercell.core.grid.MemCell;

public class ScoopExpressionWrapper extends ScoopExpression {
    private final Expression delegate;

    public ScoopExpressionWrapper(Expression delegate) {
        this.delegate = delegate;
    }

    @Override
    public MemCell calculateCellValue() {
        return (MemCell) delegate.evaluate();
    }

    @Override
    public String getMetricFormula() {
        return delegate.getMetricFormula();
    }

    @Override
    public String getExcelFormula() {
        return delegate.getExcelFormula();
    }
}
