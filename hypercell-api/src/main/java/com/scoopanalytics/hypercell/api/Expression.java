package com.scoopanalytics.hypercell.api;

public interface Expression {
    CellValue evaluate();
    String getExcelFormula();
    String getMetricFormula();
    default Object possibleSpillRange() { return null; }
}
