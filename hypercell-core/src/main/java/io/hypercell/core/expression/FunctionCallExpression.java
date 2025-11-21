package io.hypercell.core.expression;

import io.hypercell.api.Expression;
import io.hypercell.api.CellValue;
import io.hypercell.api.Function;
import io.hypercell.api.EvaluationContext;
import java.util.List;

public class FunctionCallExpression extends AbstractExpression {
    private Function function;
    private List<Expression> args;
    private EvaluationContext context;

    public FunctionCallExpression(Function function, List<Expression> args, EvaluationContext context) {
        this.function = function;
        this.args = args;
        this.context = context;
    }

    @Override
    public CellValue evaluate() {
        if (function == null) return null; // Or Error
        return function.execute(args, context);
    }
}
