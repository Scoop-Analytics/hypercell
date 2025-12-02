/**
 *
 */
package io.hypercell.core.datagrid;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.hypercell.core.datatable.CellFormat;
import io.hypercell.core.datatable.columnar.ColumnarDataTable;

/**
 * @author bradpeters
 */
public class DateCell extends DataCell
{
    private Date value;
    private CellFormat format;

    public DateCell()
    {

    }

    public DateCell(Date value)
    {
        this.value = value;
    }

    public Date getDateValue()
    {
        return value;
    }

    public void setValue(Date value)
    {
        this.value = value;
    }

    @Override
    public DataCell duplicate()
    {
        DateCell newcell = new DateCell(value);
        newcell.format = format;
        return newcell;
    }

    @Override
    public String toString()
    {
        if (value == null)
            return "[null]";
        else
            return value.toString();
    }

    @Override
    public String getStringValue()
    {
        if (format != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(format.getFormat());
            return sdf.format(value);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(ColumnarDataTable.TIMESTAMP_FORMAT);
        return sdf.format(value);
    }

    @Override
    public Date getValue()
    {
        return value;
    }

    @Override
    public boolean isBlank()
    {
        return value == null;
    }

}
