package com.scoopanalytics.hypercell.api;

import java.util.List;

/**
 * Interface to abstract the evaluation context for HyperCell calculations.
 * Implementations provide access to external data sources, cross-sheet references,
 * and other environment-specific capabilities.
 */
public interface EvaluationContext {

    /**
     * Resolves a reference to a cell or range in a different sheet or workbook.
     *
     * @param sheetName The name of the sheet.
     * @param row       The row index.
     * @param col       The column index.
     * @return The value at the specified location, or null if not found.
     */
    Object resolveReference(String sheetName, int row, int col);

    /**
     * Retrieves the list of available data sources.
     *
     * @return A list of DataSource objects.
     */
    List<DataSource> getDataSources();

    /**
     * Refreshes the specified data source.
     *
     * @param dataSource The data source to refresh.
     */
    void refreshDataSource(DataSource dataSource);
}