package io.hypercell.functions;

import io.hypercell.api.CellValue;
import io.hypercell.api.Expression;
import io.hypercell.api.Function;
import io.hypercell.api.EvaluationContext;
import java.util.List;

public abstract class BaseFunction implements Function {

    protected Double getNumericValue(CellValue cv) {
        if (cv == null) return null;
        return cv.getNumberValue();
    }

    protected CellValue createNumber(double value) {
        return new CellValue() {
            @Override public Type getType() { return Type.NUMBER; }
            @Override public Object getValue() { return value; }
            @Override public String getStringValue() { return Double.toString(value); }
            @Override public Double getNumberValue() { return value; }
            @Override public Boolean getBooleanValue() { return value != 0; }
            @Override public CellValue[][] getArrayValue() { return null; }
        };
    }
    
    protected CellValue createError(String error) {
        return new CellValue() {
            @Override public Type getType() { return Type.ERROR; }
            @Override public Object getValue() { return error; }
            @Override public String getStringValue() { return error; }
            @Override public Double getNumberValue() { return null; }
            @Override public Boolean getBooleanValue() { return null; }
            @Override public CellValue[][] getArrayValue() { return null; }
        };
    }
}
