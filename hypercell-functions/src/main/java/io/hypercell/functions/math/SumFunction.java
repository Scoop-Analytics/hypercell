package io.hypercell.functions.math;

import io.hypercell.api.*;
import io.hypercell.functions.BaseFunction;
import java.util.List;

public class SumFunction extends BaseFunction {
    @Override
    public CellValue execute(List<Expression> args, EvaluationContext context) {
        double sum = 0;
        for (Expression arg : args) {
            CellValue val = arg.evaluate();
            if (val == null) continue;
            
            if (val.getType() == CellValue.Type.NUMBER) {
                sum += val.getNumberValue();
            } else if (val.getType() == CellValue.Type.ARRAY) {
                 CellValue[][] array = val.getArrayValue();
                 if (array != null) {
                     for (CellValue[] row : array) {
                         for (CellValue cell : row) {
                             if (cell != null && cell.getType() == CellValue.Type.NUMBER) {
                                 sum += cell.getNumberValue();
                             }
                         }
                     }
                 }
            }
        }
        return createNumber(sum);
    }
}
