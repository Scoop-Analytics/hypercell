/**
 *
 */
package io.hypercell.core.datagrid;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import scoop.ScoopContext;
import io.hypercell.core.datatable.CellFormat;
import scoop.reportinstance.ReportInstance;
import scoop.reportinstance.TablePath;
import scoop.reportinstance.TablePathEntryType;
import io.hypercell.api.RangeAddress;
import io.hypercell.core.grid.MemWorkbook;

/**
 * @author bradpeters
 */
public class ExcelDataGrid extends DataGrid
{
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\\/|\\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\\/|\\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\\s][A|a][M|m][\\/][P|p][M|m])?)?");
    private static final Pattern EXCEL_DATE_PATTERN = Pattern.compile(
            "([\\[][$][-].+[\\]])?([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\\/|\\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\\/|\\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\\s][A|a][M|m][\\/][P|p][M|m])?)?");
    private transient Sheet sheet;
    private transient String rangeName;
    private int numColumns = 0;

    public ExcelDataGrid()
    {
    }

    public ExcelDataGrid(TablePath tablePath)
    {
        super(tablePath, null);
    }

    public ExcelDataGrid(ScoopContext sc, TablePath tablePath, Sheet sheet, String rangeName, ReportInstance reportInstance)
    {
        super(tablePath, reportInstance);
        if (tablePath.getPathEntries().getLast().type != TablePathEntryType.ExcelRange)
        {
            tablePath.addPathEntry(TablePathEntryType.ExcelSheet, sheet.getSheetName());
        }
        this.sheet = sheet;
        this.rangeName = rangeName;
        loadGrid(sc);
        trim();
    }

    private void loadGrid(ScoopContext sc)
    {
        Workbook wb = sheet.getWorkbook();
        MemWorkbook mw = new MemWorkbook(sc,"MEMWB", wb, true);
        mw.calculate(sc);
        mw.saveCalculations(wb);
        setGrid(mw);
    }

    private void setGrid(MemWorkbook mw)
    {
        /*
         * Ignore any leading blank columns
         */
        int firstColumn = Integer.MAX_VALUE;
        int lastColumn = Integer.MAX_VALUE;
        for (Row row : sheet)
        {
            int firstCol = row.getFirstCellNum();
            if (firstCol < 0)
                continue;
            for (; firstCol < row.getLastCellNum(); firstCol++)
            {
                if (row.getCell(firstCol, MissingCellPolicy.RETURN_BLANK_AS_NULL) != null)
                {
                    break;
                }
            }
            if (firstCol < firstColumn)
            {
                firstColumn = firstCol;
            }
        }

        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        if (lastRow > 2000)
        {
            // Don't parse dates for huge sheets
            setParseDates(false);
        }
        if (rangeName != null)
        {
            RangeAddress range = mw.getRanges().get(rangeName);
            if (range != null)
            {
                firstRow = range.getFirstRowNumber(mw);
                lastRow = range.getLastRowNumber(mw);
                firstColumn = range.getFirstColumnNumber(mw);
                lastColumn = range.getLastColumnNumber(mw);
            }
        }

        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++)
        {
            Row row = sheet.getRow(rowNum);
            if (row == null)
                continue;
            int lastCol = Math.min(lastColumn, row.getLastCellNum());
            int numCells = lastCol - firstColumn + 1;
            DataRow dr = new DataRow();
            if (numCells > numColumns)
            {
                numColumns = numCells;
            }
            for (int cellNum = firstColumn; cellNum <= lastCol; cellNum++)
            {
                Cell cell = row.getCell(cellNum);
                if (cell == null)
                {
                    dr.addCell(new EmptyCell());
                    continue;
                }
                CellType ctype = cell.getCellType();
                DataCell dc = null;
                if (ctype == CellType.FORMULA)
                {
                    ctype = cell.getCachedFormulaResultType();
                }
                if (ctype == CellType.STRING)
                {
                    dc = new StringCell(cell.getStringCellValue());
                } else if (ctype == CellType.BOOLEAN)
                {
                    dc = new BooleanCell(cell.getBooleanCellValue());
                } else if (ctype == CellType.NUMERIC)
                {
                    String formatString = cell.getCellStyle().getDataFormatString();
                    if (isDateFormat(formatString))
                    {
                        Date d = cell.getDateCellValue();
                        dc = new DateCell(d);
                    } else
                    {
                        dc = getNumericCell(cell);
                    }
                } else if (ctype == CellType.BLANK)
                {
                    dc = new EmptyCell();
                }
                dr.addCell(dc);
            }
            rows.add(dr);
        }
    }

    public static DataCell getNumericCell(Cell cell)
    {
        CellStyle style = cell.getCellStyle();
        String formatString = style != null ? style.getDataFormatString() : null;
        DataCell dc = null;
        if (formatString != null && isExcelDateFormat(formatString))
        {
            dc = new DateCell(cell.getDateCellValue());
        } else
        {
            NumericCell nc = null;
            double d = cell.getNumericCellValue();
            if ((d % 1) == 0)
            {
                // Make integer if possible
                nc = new NumericCell((int) d);
            } else
            {
                nc = new NumericCell(d);
            }
            dc = nc;
            if (formatString != null && !formatString.equals("General"))
            {
                int index = formatString.indexOf("\\");
                if (index >= 0)
                {
                    formatString = formatString.substring(0, index);
                }
                CellFormat cf = nc.getFormat();
                if (cf == null)
                {
                    cf = new CellFormat();
                    nc.setFormat(cf);
                }
                String dataFormatString = getNumericFormatString(formatString);
                cf.setFormat(dataFormatString);
            }
        }
        return dc;
    }

    public static String getNumericFormatString(String excelString)
    {
        excelString.replace("\\", "")
                   .replaceAll("([\\[][$][-].+[\\]])?([_][(][^\\s]*\\s)?([_][)])?",
                           "");
        return excelString;
    }

    public static boolean isDateFormat(String formatString)
    {
        if (formatString == null)
            return false;
        Matcher m = DATE_PATTERN.matcher(formatString.replace("\\", ""));
        return m.matches();
    }

    public static String removeBadDateFormatCharacters(String formatString)
    {
        return formatString.replace("\\", "").replace("\"", "");
    }

    public static boolean isExcelDateFormat(String formatString)
    {
        Matcher m = EXCEL_DATE_PATTERN.matcher(removeBadDateFormatCharacters(formatString));
        return m.matches();
    }

    @Override
    public void removeColumns(boolean[] columnsToRemove)
    {
        super.removeColumns(columnsToRemove);
        for (int j = 0; j < columnsToRemove.length; j++)
        {
            if (columnsToRemove[j])
            {
                numColumns--;
            }
        }
    }

    @Override
    public boolean removeHeadersAndFooters()
    {
        /*
         * If an exact range is used - don't try to prune bad headers and footers
         */
        return rangeName == null;
    }
}
