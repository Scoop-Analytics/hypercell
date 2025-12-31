/**
 * DataSetValue for HyperCell metric evaluation.
 */
package com.scoopanalytics.hypercell.core.types;

/**
 * Holds a reference to a value within a DataSet.
 */
public class DataSetValue
{
    public DataSet dataSet;
    public int row;
    public int column;
    public double value;

    public DataSetValue(DataSet dataSet, int row, int column)
    {
        this.dataSet = dataSet;
        this.row = row;
        this.column = column;
    }
}
