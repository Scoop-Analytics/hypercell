/**
 *
 */
package io.hypercell.core.datagrid;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

import io.hypercell.core.datatable.CellFormat;
import io.hypercell.core.datatable.columnar.ColumnarDataTable;

/**
 * @author bradpeters
 *
 */
public class NumericCell extends DataCell
{
    private Number value;
    private CellFormat format;

    public NumericCell(Number value)
    {
        this.value = value;
    }

    @Override
    public Number getValue()
    {
        return value;
    }

    public void setValue(Number value)
    {
        this.value = value;
    }

    public CellFormat getFormat()
    {
        return format;
    }

    public void setFormat(CellFormat format)
    {
        this.format = format;
    }

    @Override
    public DataCell duplicate()
    {
        NumericCell newcell = new NumericCell(value);
        newcell.format = format;
        return newcell;
    }

    @Override
    public String toString()
    {
        if (value == null)
            return "[null]";
        return value.toString();
    }

    @Override
    public String getStringValue()
    {
        if (format != null)
        {
            DecimalFormat df = format.getDecimalFormat();
            return Objects.requireNonNullElseGet(df, DecimalFormat::new).format(value);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(ColumnarDataTable.TIMESTAMP_FORMAT);
        return sdf.format(value);
    }

    @Override
    public boolean isBlank()
    {
        return value == null;
    }

}
