package io.hypercell.api;

public interface CellValue {
    enum Type { STRING, NUMBER, BOOLEAN, ERROR, EMPTY, ARRAY }
    
    Type getType();
    Object getValue();
    
    String getStringValue();
    Double getNumberValue();
    Boolean getBooleanValue();
    CellValue[][] getArrayValue();
}
