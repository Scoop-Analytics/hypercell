package io.hypercell.core.expression;

import io.hypercell.core.grid.MemCell;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import io.hypercell.core.grid.MemCellContext;
import io.hypercell.core.util.FormattingUtils;

public class FunctionUtils {
    public static String getStringValue(MemCell cell, boolean inConcant) {
        if (cell == null) return null;
        if (cell.getStringValue() == null) {
            if (cell.getNumberValue() != null) {
                MemCellContext context = cell.getCellContext();
                if (context != null) {
                    // Simplification: Just toString for now to break circular dependency
                    return cell.getNumberValue().toString();
                } else {
                    return cell.getNumberValue().toString();
                }
            } else {
                return null;
            }
        } else {
            return cell.getStringValue();
        }
    }
}
