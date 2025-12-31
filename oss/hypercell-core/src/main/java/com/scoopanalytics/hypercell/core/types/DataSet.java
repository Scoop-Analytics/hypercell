/**
 * DataSet interface for HyperCell.
 */
package com.scoopanalytics.hypercell.core.types;

/**
 * Interface for accessing tabular data.
 */
public interface DataSet
{
    /**
     * Get a value at the specified row and column.
     *
     * @param row the row index
     * @param column the column index
     * @return the value at the specified position
     */
    Object getValue(int row, int column);

    /**
     * Get a double value at the specified row and column.
     *
     * @param row the row index
     * @param column the column index
     * @return the double value at the specified position
     */
    double getDoubleValue(int row, int column);
}
