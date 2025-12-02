/**
 *
 */
package io.hypercell.core.datagrid;

import scoop.ingest.IngestedContent;
import scoop.reportinstance.ReportInstance;
import scoop.reportinstance.TablePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author bradpeters
 */
public class DataGrid
{
    // The parent report instance for this table
    private ReportInstance reportInstance;
    // Table path
    private TablePath tablePath;
    // The rows of data for this grid
    protected List<DataRow> rows = new ArrayList<DataRow>();
    // Inferred date (if present)
    protected Date inferredDate;
    private boolean timeout;
    // Whether additional date parsing needs to be done
    private boolean parseDates = true;

    public DataGrid()
    {

    }

    public DataGrid(TablePath tablePath)
    {
        this.tablePath = tablePath;
    }

    public DataGrid(TablePath tablePath, ReportInstance ri)
    {
        this.tablePath = tablePath;
        this.reportInstance = ri;
    }

    private Integer numCellCache = null;

    /**
     * Get the total number of cells in this table (rowsxcolumns)
     */
    public int getNumCells()
    {
        if (numCellCache != null)
        {
            return numCellCache;
        }
        int count = 0;
        for (DataRow dr : rows)
        {
            count += dr.getNumCells();
        }
        numCellCache = count;
        return count;
    }

    public ReportInstance getReportInstance()
    {
        return reportInstance;
    }

    public void setReportInstance(ReportInstance reportInstance)
    {
        this.reportInstance = reportInstance;
    }

    public TablePath getTablePath()
    {
        return tablePath;
    }

    public int getNumColumns()
    {
        if (numCellCache != null)
        {
            return numCellCache;
        }
        int numColumns = 0;
        for (DataRow dr : rows)
        {
            if (dr == null)
                continue;
            if (dr.getNumCells() > numColumns)
            {
                numColumns = dr.getNumCells();
            }
        }
        numCellCache = numColumns;
        return numColumns;
    }

    public int getNumRows()
    {
        return rows.size();
    }

    public List<DataRow> getRows()
    {
        return rows;
    }

    /**
     * Get cell value properly formatted
     */
    public String getStringAt(int row, int col)
    {
        return rows.get(row).cells.get(col).getStringValue();
    }

    /**
     * Get raw cell value
     */
    public Object getCellValueAt(int row, int col)
    {
        DataRow dr = rows.get(row);
        if (dr.getNumCells() <= col) return null;
        if (dr.cells.get(col) == null) return null;
        return dr.cells.get(col).getValue();
    }

    /**
     * Get cell
     */
    public DataCell getCellAt(int row, int col)
    {
        DataRow dr = rows.get(row);
        if (dr.getNumCells() <= col) return null;
        return dr.cells.get(col);
    }

    /**
     * Do any post processing of a data table (intended to be overridden)
     */
    public void postProcess(IngestedContent content)
    {
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (DataRow dr : rows)
        {
            if (dr == null)
                continue;
            for (int col = 0; col < dr.getNumCells(); col++)
            {
                if (col > 0)
                {
                    sb.append(',');
                }
                if (dr.getCellAt(col) != null)
                {
                    sb.append(dr.getCellAt(col).toString());
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public void writeToFile(File outputFile) throws FileNotFoundException
    {
        PrintWriter writer = new PrintWriter(outputFile);
        for (DataRow dr : rows)
        {
            for (int col = 0; col < dr.getNumCells(); col++)
            {
                if (col > 0)
                {
                    writer.print('\t');
                }
                DataCell dc = dr.getCellAt(col);
                if (dc == null || dc instanceof EmptyCell || dc.getStringValue() == null)
                    continue;
                writer.print('"' + dr.getCellAt(col).getStringValue().replace("\"", "\"\"") + '"');
            }
            writer.println();
        }
        writer.close();
    }

    /*
     * Remove any blank rows or columns around the core data
     */
    protected void trim()
    {
        /*
         * Remove any leading blank rows
         */
        int firstNonblankRow = rows.size();
        for (int i = 0; i < rows.size(); i++)
        {
            DataRow dr = rows.get(i);
            if (!dr.isBlank())
            {
                firstNonblankRow = i;
                break;
            }
        }
        if (firstNonblankRow > 0)
        {
            rows.subList(0, firstNonblankRow).clear();
        }
        /*
         * Remove any trailing blank rows
         */
        for (int i = rows.size() - 1; i >= 0; i--)
        {
            DataRow dr = rows.get(i);
            if (dr.isBlank())
            {
                rows.remove(i);
            } else
            {
                break;
            }
        }
        /*
         * Remove any blank columns
         */
        int numColumns = 0;
        for (DataRow dr : rows)
        {
            if (dr.getNumCells() > numColumns) numColumns = dr.getNumCells();
        }
        int[] nonBlank = new int[numColumns];
        for (int j = 0; j < numColumns; j++)
        {
            nonBlank[j] = 0;
        }
        for (DataRow dr : rows)
        {
            for (int j = 0; j < dr.getNumCells(); j++)
            {
                DataCell dc = dr.getCellAt(j);
                if (dc != null && !dc.isBlank())
                {
                    nonBlank[j]++;
                }
            }
        }
        boolean hasBlankColumn = false;
        boolean[] columnsToRemove = new boolean[nonBlank.length];
        for (int i = 0; i < nonBlank.length; i++)
        {
            if (nonBlank[i] == 0)
            {
                hasBlankColumn = true;
                columnsToRemove[i] = true;
            }
        }
        if (hasBlankColumn)
        {
            removeColumns(columnsToRemove);
        }
    }

    public void removeColumns(boolean[] columnsToRemove)
    {
        List<DataRow> newRows = new ArrayList<DataRow>();
        for (DataRow dr : rows)
        {
            DataRow newDr = new DataRow();
            for (int j = 0; j < dr.getNumCells(); j++)
            {
                if (!columnsToRemove[j])
                {
                    newDr.addCell(dr.getCellAt(j));
                }
            }
            newRows.add(newDr);
        }
        rows = newRows;
    }

    /*
     * Remove any double spacing that may be present. Detect a pattern of alternating rows and spaces.
     * The algorithm will only remove one contiguous block, trying to isolate it from header and footer rows
     */

    public void pruneDoubleSpacing()
    {
        int header = 0;
        int footer = getNumRows() - 1;
        // Need at least 10 rows to find a pattern
        if (footer - header < 10)
            return;
        var rows = getRows();
        while (footer > header)
        {
            while (header < footer)
            {
                if (isDoubleSpace(rows, header, footer))
                {
                    boolean noOffset = rows.get(footer).isBlank();
                    for (int i = footer; i >= header + (noOffset ? 0 : 1); i -= 2)
                    {
                        rows.remove(i - (noOffset ? 0 : 1));
                    }
                    return;
                }
                header++;
            }
            footer--;
        }
    }

    private boolean isDoubleSpace(List<DataRow> rows, int start, int end)
    {
        int dataOffset = rows.get(start).isBlank() ? 1 : 0;
        int blankOffset = rows.get(start).isBlank() ? 0 : 1;
        for (int i = start; i <= end; i += 2)
        {
            if (i + dataOffset < rows.size() && rows.get(i + dataOffset).isBlank())
                return false;
            if (i + blankOffset < rows.size() && !rows.get(i + blankOffset).isBlank())
                return false;
        }
        return true;
    }

    /**
     * Split this datagrid into separate grids based on blank rows
     */
    public List<DataGrid> splitGrids()
    {
        List<List<DataRow>> rowLists = new ArrayList<>();
        List<DataRow> curList = new ArrayList<>();
        rowLists.add(curList);
        for (DataRow dr : rows)
        {
            if (dr.isBlank())
            {
                if (!curList.isEmpty())
                {
                    curList = new ArrayList<>();
                    rowLists.add(curList);
                }
            } else
            {
                curList.add(dr);
            }
        }
        List<DataGrid> result = new ArrayList<>();
        if (rowLists.size() == 1)
        {
            result.add(this);
            return result;
        }
        for (List<DataRow> list : rowLists)
        {
            DataGrid newGrid = new DataGrid(tablePath, reportInstance);
            newGrid.rows = list;
            newGrid.trim();
            result.add(newGrid);
        }
        return result;
    }

    public Date getInferredDate()
    {
        return inferredDate;
    }

    public void setInferredDate(Date inferredDate)
    {
        this.inferredDate = inferredDate;
    }

    public boolean removeHeadersAndFooters()
    {
        return true;
    }

    public void setMaxRows(int maxRows)
    {
        if (maxRows <= rows.size())
        {
            rows = rows.subList(0, maxRows);
        }
    }

    public boolean isTimeout()
    {
        return timeout;
    }

    public void setTimeout(boolean timeout)
    {
        this.timeout = timeout;
    }

    public boolean isParseDates()
    {
        return parseDates;
    }

    public void setParseDates(boolean parseDates)
    {
        this.parseDates = parseDates;
    }

    public void clearCache()
    {
        numCellCache = null;
    }
}
