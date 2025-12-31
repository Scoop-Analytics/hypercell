/**
 * Extended workbook with enterprise features.
 */
package com.scoopanalytics.hypercell.bridge;

import com.scoopanalytics.hypercell.core.grid.MemCell;
import com.scoopanalytics.hypercell.core.grid.MemCellType;
import com.scoopanalytics.hypercell.core.grid.MemSheet;
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended workbook that integrates enterprise features via the bridge.
 *
 * <p>This class wraps a {@link MemWorkbook} and adds:
 * <ul>
 *   <li>Automatic query refresh before calculation</li>
 *   <li>Enterprise evaluation context support</li>
 *   <li>Audit logging integration</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class ExtendedWorkbook
{
    private static final Logger logger = LoggerFactory.getLogger(ExtendedWorkbook.class);

    private final MemWorkbook workbook;
    private EnterpriseEvaluationContext context;
    private boolean autoRefreshQueries = true;

    /**
     * Create an extended workbook wrapper.
     *
     * @param workbook the underlying workbook
     */
    public ExtendedWorkbook(MemWorkbook workbook)
    {
        this.workbook = workbook;
    }

    /**
     * Create an extended workbook with enterprise context.
     *
     * @param workbook the underlying workbook
     * @param context the enterprise evaluation context
     */
    public ExtendedWorkbook(MemWorkbook workbook, EnterpriseEvaluationContext context)
    {
        this.workbook = workbook;
        this.context = context;
    }

    /**
     * Calculate all formulas, refreshing queries if needed.
     */
    public void calculate()
    {
        if (autoRefreshQueries && context != null)
        {
            refreshQueriesIfNeeded();
        }

        if (context != null)
        {
            context.logAuditEvent("CALCULATE", "Starting workbook calculation: " + workbook.getName());
        }

        workbook.calculate();

        if (context != null)
        {
            context.logAuditEvent("CALCULATE_COMPLETE", "Completed workbook calculation: " + workbook.getName());
        }
    }

    /**
     * Refresh all query sheets that need updating.
     */
    public void refreshQueriesIfNeeded()
    {
        if (context == null || context.getQueryRefreshHandler() == null)
        {
            return;
        }

        QueryRefreshHandler handler = context.getQueryRefreshHandler();

        for (MemSheet sheet : workbook.getSheets())
        {
            if (handler.isQuerySheet(sheet) && handler.needsRefresh(sheet))
            {
                logger.info("Refreshing query sheet: {}", sheet.getName());

                if (context != null)
                {
                    context.logAuditEvent("QUERY_REFRESH", "Refreshing: " + sheet.getName());
                }

                boolean success = handler.refreshQuerySheet(workbook, sheet);

                if (!success)
                {
                    logger.warn("Failed to refresh query sheet: {}", sheet.getName());
                }
            }
        }
    }

    /**
     * Get a cell value, refreshing if needed.
     *
     * @param sheetName the sheet name
     * @param row the row index
     * @param col the column index
     * @return the cell value
     */
    public Object getCellValue(String sheetName, int row, int col)
    {
        MemSheet sheet = workbook.getSheet(sheetName);
        if (sheet == null)
        {
            return null;
        }

        MemCell cell = sheet.getCellAt(row, col);
        if (cell == null)
        {
            return null;
        }

        return cell.getValue();
    }

    /**
     * Set a cell value and optionally recalculate.
     *
     * @param sheetName the sheet name
     * @param row the row index
     * @param col the column index
     * @param value the value to set
     * @param recalculate whether to recalculate after setting
     */
    public void setCellValue(String sheetName, int row, int col, Object value, boolean recalculate)
    {
        MemSheet sheet = workbook.getSheet(sheetName);
        if (sheet == null)
        {
            logger.warn("Sheet not found: {}", sheetName);
            return;
        }

        MemCell cell = sheet.getCellAt(row, col);
        if (cell == null)
        {
            cell = new MemCell();
            sheet.setCellAt(row, col, cell);
        }
        if (value instanceof Number)
        {
            cell.setNumberValue((Number) value);
            cell.setCellType(MemCellType.Number);
        }
        else if (value instanceof String)
        {
            cell.setStringValue((String) value);
            cell.setCellType(MemCellType.String);
        }
        else if (value != null)
        {
            cell.setStringValue(value.toString());
            cell.setCellType(MemCellType.String);
        }

        if (recalculate)
        {
            calculate();
        }
    }

    // Getters and setters

    public MemWorkbook getWorkbook()
    {
        return workbook;
    }

    public EnterpriseEvaluationContext getContext()
    {
        return context;
    }

    public void setContext(EnterpriseEvaluationContext context)
    {
        this.context = context;
    }

    public boolean isAutoRefreshQueries()
    {
        return autoRefreshQueries;
    }

    public void setAutoRefreshQueries(boolean autoRefreshQueries)
    {
        this.autoRefreshQueries = autoRefreshQueries;
    }
}
