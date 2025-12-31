package com.scoopanalytics.hypercell.api;

import java.util.List;

public interface Function {
    CellValue execute(List<Expression> args, EvaluationContext context);
}
