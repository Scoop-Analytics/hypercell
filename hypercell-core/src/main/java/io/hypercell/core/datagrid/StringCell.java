/**
 *
 */
package io.hypercell.core.datagrid;

/**
 * @author bradpeters
 *
 */
public class StringCell extends DataCell
{
    private String cellValue;

    public StringCell()
    {
    }

    public StringCell(String cellValue)
    {
        this.cellValue = cellValue;
    }

    @Override
    public String getValue()
    {
        return cellValue;
    }

    public void setValue(String cellValue)
    {
        this.cellValue = cellValue;
    }

    @Override
    public DataCell duplicate()
    {
        return new StringCell(cellValue);
    }

    @Override
    public String toString()
    {
        if (cellValue == null)
            return "[null]";
        return cellValue;
    }

    @Override
    public String getStringValue()
    {
        return cellValue;
    }

    @Override
    public boolean isBlank()
    {
        return cellValue == null || cellValue.trim().isEmpty();
    }
}
