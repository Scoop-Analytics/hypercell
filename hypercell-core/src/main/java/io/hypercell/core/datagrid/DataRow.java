/**
 *
 */
package io.hypercell.core.datagrid;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bradpeters
 */
public class DataRow
{
    public DataRow()
    {

    }

    protected List<DataCell> cells = new ArrayList<DataCell>();

    public void addCell(DataCell dc)
    {
        cells.add(dc);
    }

    public DataCell getCellAt(int columnIndex)
    {
        if (cells.size() <= columnIndex)
            return null;
        return cells.get(columnIndex);
    }

    public int getNumCells()
    {
        if (cells == null)
            return 0;
        return cells.size();
    }

    public void setCellAt(int columnIndex, DataCell cell)
    {
        cells.set(columnIndex, cell);
    }

    public boolean isBlank()
    {
        for (int j = 0; j < getNumCells(); j++)
        {
            DataCell dc = getCellAt(j);
            if (dc != null && !dc.isBlank())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (DataCell cell : cells)
        {
            if (count++ > 0)
                sb.append("|");
            sb.append(cell.toString());
        }
        return sb.toString();
    }

    public void removeCellAt(int index)
    {
        cells.remove(index);
    }

    public List<DataCell> getCells()
    {
        return cells;
    }

}
