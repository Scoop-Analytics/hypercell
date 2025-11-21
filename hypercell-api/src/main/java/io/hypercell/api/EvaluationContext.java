package io.hypercell.api;

public interface EvaluationContext {
    CellValue resolveReference(String sheet, int row, int col);
    CellValue resolveIdentifier(String name);
}
