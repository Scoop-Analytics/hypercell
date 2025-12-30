/**
 * Handler for refreshing query-based data sources.
 */
package io.hypercell.bridge;

import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.MemWorkbook;

/**
 * Interface for handling query refresh operations.
 *
 * <p>Enterprise platforms implement this interface to refresh data from
 * external sources (databases, APIs, etc.) before formula calculation.
 *
 * <p>This is the extension point that replaces the Scoop-specific
 * CalculatedSourceWorkbook functionality in the OSS version.
 *
 * @since 0.1.0
 */
public interface QueryRefreshHandler
{
    /**
     * Refresh data for a query sheet.
     *
     * @param workbook the workbook containing the query sheet
     * @param sheet the query sheet to refresh
     * @return true if refresh was successful
     */
    boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet);

    /**
     * Check if a sheet is a query sheet that needs refresh.
     *
     * @param sheet the sheet to check
     * @return true if this is a query sheet
     */
    boolean isQuerySheet(MemSheet sheet);

    /**
     * Check if a sheet needs to be refreshed before calculation.
     *
     * @param sheet the sheet to check
     * @return true if refresh is needed
     */
    boolean needsRefresh(MemSheet sheet);
}
