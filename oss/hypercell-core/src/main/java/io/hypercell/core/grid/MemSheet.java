/**
 *
 */
package io.hypercell.core.grid;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hypercell.core.expression.Identifier;
import io.hypercell.core.expression.Range;
import io.hypercell.core.expression.SpillArea;
import io.hypercell.api.CellAddress;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author bradpeters
 */
public class MemSheet implements io.hypercell.api.EvaluationContext
{
    private static final Logger logger = LoggerFactory.getLogger(MemSheet.class);
    private static final int MAX_LINES_PER_QUERY_SHEET = 10000;
    private transient MemWorkbook workbook;
    private String name;
    private MemCell[][] array;
    private int[] widths;
    private transient boolean querySheet;
    private transient boolean querySheetUpdated;
    private final transient Map<CellAddress, MemCell> formulas = new ConcurrentHashMap<>();
    private final transient AtomicInteger numParseErrors = new AtomicInteger(0);
    private transient List<String> parseErrors = new ArrayList<>();
    private final transient Map<String, List<CellAddress>> rangeAddressCache = new ConcurrentHashMap<>();
    private final transient Map<MemCell, SpillArea> spillAreaCache = new ConcurrentHashMap<>();

    public MemSheet()
    {
    }

    public MemSheet(MemSheet ms)
    {
        this.workbook = ms.workbook;
        this.name = ms.name;
        this.array = new MemCell[ms.array.length][];
        this.widths = ms.widths;
        IntStream.range(0, ms.array.length).parallel().forEach(row -> {
            array[row] = new MemCell[ms.array[row].length];
            for (int col = 0; col < array[row].length; col++)
            {
                MemCell mc = ms.array[row][col];
                if (mc != null)
                {
                    array[row][col] = new MemCell(mc);
                    array[row][col].setSheet(this);
                    if (array[row][col].getCellType() == MemCellType.Formula)
                    {
                        array[row][col].compileFormula(this);
                        addFormulaAt(row, col, array[row][col]);
                    }
                }
            }
        });
    }

    public MemSheet(MemCell[][] array)
    {
        this.array = array;
    }

    public MemSheet(MemWorkbook workbook, int numRows, int numColumns)
    {
        this.workbook = workbook;
        array = new MemCell[numRows][numColumns];
        widths = new int[numColumns];
    }

    public void updateFromSheet(MemSheet ms)
    {
        array = ms.array;
        widths = ms.widths;
        name = ms.name;
    }

    public void setCellAt(int row, int col, MemCell cell)
    {
        if (array.length <= row)
        {
            MemCell[][] newArr = new MemCell[array.length + 10][];
            System.arraycopy(array, 0, newArr, 0, array.length);
            array = newArr;
        }
        if (array[row] == null)
        {
            array[row] = new MemCell[col + 1];
        }
        if (array[row].length <= col)
        {
            MemCell[] newArr = new MemCell[col + 1];
            System.arraycopy(array[row], 0, newArr, 0, array[row].length);
            array[row] = newArr;
        }
        array[row][col] = cell;
        if (cell != null)
            cell.setSheet(this);
    }

    public void addFormulaAt(int row, int col, MemCell cell)
    {
        array[row][col] = cell;
        CellAddress ca = new CellAddress(row, col);
        formulas.put(ca, cell);
        cell.setSheet(this);
    }

    public void removeFormulaAt(int row, int col)
    {
        CellAddress found = null;
        for (var ca : formulas.keySet())
        {
            if (ca.row == row && ca.column == col)
            {
                found = ca;
                break;
            }
        }
        if (found != null)
        {
            formulas.remove(found);
        }
    }

    public void reset()
    {
        array = new MemCell[1][1];
    }


    public int getNumCellsInRow(int row)
    {
        if (row < 0 || row >= array.length || array[row] == null)
            return 0;
        return array[row].length;
    }

    public MemCell getCellAt(int row, int col)
    {
        if (row < 0 || row >= array.length || array[row] == null || col < 0 || col >= array[row].length)
            return null;
        return array[row][col];
    }

    public MemCell getOrCreateCellAt(int row, int col, MemCellType type)
    {
        if (array.length <= row)
        {
            var newArray = new MemCell[row + 1][];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
        if (array[row] == null)
        {
            array[row] = new MemCell[col + 1];
        }
        if (array[row].length <= col)
        {
            var newRow = new MemCell[col + 1];
            System.arraycopy(array[row], 0, newRow, 0, array[row].length);
            array[row] = newRow;
        }
        if (array[row][col] == null)
        {
            array[row][col] = new MemCell();
            array[row][col].setCellType(type);
            array[row][col].setCellContext(new MemCellContext(row, col));
        }
        return array[row][col];
    }

    public void removeCellAt(int row, int col)
    {
        if (array[row] == null)
            return;
        if (array[row][col] == null)
            return;
        array[row][col] = null;
    }

    public MemCell getOrCreateCellAt(int row, int col, MemCellType type, MemCellStyle style)
    {
        var mc = getOrCreateCellAt(row, col, type);
        mc.setCellType(type);
        if (mc.getCellContext() == null)
        {
            mc.setCellContext(new MemCellContext(row, col));
        }
        if (style != null)
        {
            mc.getCellContext().setStyle(style);
        }
        return mc;
    }

    public MemCell getOrCreateCellAt(int row, int col, MemCellType type, XSSFCellStyle style)
    {
        var mc = getOrCreateCellAt(row, col, type);
        mc.setCellType(type);
        if (style != null)
        {
            mc.setCellContext(new MemCellContext(style, this.workbook, row, col));
        }
        return mc;
    }

    public MemCell getCellAt(String sheetName, int row, int col)
    {
        MemSheet ms = workbook.getSheet(sheetName);
        if (ms == null)
            return null;
        return ms.getCellAt(row, col);
    }

    public MemCell getCellAt(CellAddress address)
    {
        if (address.sheetName == null)
        {
            if (address.row >= array.length || address.column >= array[address.row].length)
                return null;
            return array[address.row][address.column];
        }
        MemSheet ms = workbook.getSheet(address.sheetName);
        if (ms == null)
            return null;
        return ms.getCellAt(address.row, address.column);
    }

    public void deleteCellAt(int row, int col)
    {
        array[row][col] = null;
    }

    public void deleteCellValueAt(int row, int col)
    {
        MemCell mc = array[row][col];
        if (mc != null)
        {
            if (mc.getFormula() != null)
            {
                mc.setFormula(null);
            }
            mc.setNumberValue(null);
            mc.setStringValue(null);
        }
    }

    public void trimRow(int row, int lastCol)
    {
        if (row >= array.length)
            return;
        var newRow = new MemCell[lastCol];
        for (int i = 0; i < array[row].length && i < newRow.length; i++)
        {
            newRow[i] = array[row][i];
        }
        array[row] = newRow;

    }

    public int getNumRows()
    {
        return array.length;
    }

    public int getNumColumns()
    {
        var maxCol = 0;
        for (MemCell[] memCells : array)
        {
            if (memCells == null)
                continue;
            if (memCells.length > maxCol)
                maxCol = memCells.length;
        }
        return maxCol;
    }

    /**
     * Compile all the formulas in the sheet
     */
    public void compileFormulas()
    {
        if (formulas.isEmpty())
        {
            processFormulas();
        }
        formulas.values().parallelStream().forEach(memCell -> memCell.compileFormula(this));
    }

    /**
     * Check spill areas against any identifiers and ranges. If they could be impacted by the spill area calculations
     * then add the spill cell to dependencies for the memcell so that it can be calculated first. Each memcell should
     * check if there are dependencies and cause them to calculate first before they self-calculate
     */
    public void checkSpillAreas()
    {
        formulas.values().parallelStream().forEach(memCell ->
        {
            List<Identifier> ids = memCell.getIdentifiers();
            // Check for address dependencies
            if (ids != null && !ids.isEmpty())
            {
                for (Identifier id : ids)
                {
                    for (var sheet : workbook.getSheets())
                    {
                        for (var spillCell : sheet.getSpillAreaCache().keySet())
                        {
                            if (sheet != this && !spillCell.getSheet().name.equals(id.getAddress().sheetName))
                                continue;
                            SpillArea sa = sheet.getSpillAreaCache().get(spillCell);
                            int row = id.getAddress().row;
                            int col = id.getAddress().column;
                            int spillRow = spillCell.getCellContext().getRow();
                            int spillCol = spillCell.getCellContext().getCol();
                            if ((sa.height == -1 || ((row >= spillRow) && (row <= spillRow + sa.height))) &&
                                    (sa.width == -1 || ((col >= spillCol) && (col <= spillCol + sa.width))))
                            {
                                var dependecies = memCell.getDependencies();
                                if (dependecies == null)
                                {
                                    dependecies = new ArrayList<>();
                                    memCell.setDependencies(dependecies);
                                }
                                dependecies.add(spillCell);
                            }
                        }
                    }
                }
            }
            /*
             * If a range could overlap with a spill range add to dependencies
             */
            List<Range> ranges = memCell.getRanges();
            if (ranges != null && !ranges.isEmpty())
            {
                for (var range : ranges)
                {
                    for (var sheet : workbook.getSheets())
                    {
                        for (var spillCell : sheet.getSpillAreaCache().keySet())
                        {
                            if (!spillCell.getSheet().name.equals(range.getSheet().name))
                                continue;
                            SpillArea sa = sheet.getSpillAreaCache().get(spillCell);
                            /*
                             * See if anything in the spill area could fall within this range
                             */
                            int spillRowStart = spillCell.getCellContext().getRow();
                            int spillRowEnd = spillRowStart + (sa.height >= 0 ? sa.height : 100000000);
                            int spillColStart = spillCell.getCellContext().getCol();
                            int spillColEnd = spillColStart + (sa.width >= 0 ? sa.width : 100000000);
                            int rangeRowStart = Math.min(range.getStartAddress().getRow(),
                                    range.getEndAddress().getRow());
                            int rangeRowEnd = Math.max(range.getStartAddress().getRow(),
                                    range.getEndAddress().getRow());
                            int rangeColStart = Math.min(range.getStartAddress().getColumn(),
                                    range.getEndAddress().getColumn());
                            int rangeColEnd = Math.max(range.getStartAddress().getColumn(),
                                    range.getEndAddress().getColumn());
                            boolean isOverlapping = spillRowStart <= rangeRowEnd && spillRowEnd >= rangeRowStart &&
                                    spillColStart <= rangeColEnd && spillColEnd >= rangeColStart;
                            if (isOverlapping)
                            {
                                var dependecies = memCell.getDependencies();
                                if (dependecies == null)
                                {
                                    dependecies = new ArrayList<>();
                                    memCell.setDependencies(dependecies);
                                }
                                dependecies.add(spillCell);
                            }
                        }
                    }
                }
            }
        });
    }

    public synchronized void processFormulas()
    {
        IntStream.range(0, array.length).parallel().forEach(row -> {
                    if (array[row] == null)
                        return;
                    for (int col = 0; col < array[row].length; col++)
                    {
                        var mc = array[row][col];
                        if (mc != null)
                        {
                            if (mc.getCellType() == MemCellType.Formula)
                            {
                                CellAddress ca = new CellAddress(row, col);
                                formulas.put(ca, mc);
                            }
                            mc.setSheet(this);
                        }
                    }
                }
        );
        for (var mc : formulas.values())
        {
            processFormulaCell(mc);
        }
    }

    public void processFormulaCell(MemCell mc)
    {
        // Extension point for custom formula processing
    }

    public void resetCalculations()
    {
        for (MemCell mc : formulas.values())
        {
            mc.clearError();
            mc.clearInferred();
            mc.setCalculated(false);
        }
    }

    public void calculate()
    {
        formulas.values().parallelStream().forEach(MemCell::calculate);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (MemCell[] memCells : array)
        {
            if (memCells == null)
                continue;
            for (int col = 0; col < memCells.length; col++)
            {
                if (col > 0)
                    sb.append(",");
                MemCell mc = memCells[col];
                if (mc != null)
                    sb.append(mc.toString());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public MemWorkbook getWorkbook()
    {
        return workbook;
    }

    public Collection<MemCell> getFormulas()
    {
        return formulas.values();
    }

    private void setCellErrorValue(XSSFCell cell, MemCell mc)
    {
        if (mc.getErrorValue() == FormulaError.VALUE)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.VALUE);
        } else if (mc.getErrorValue() == FormulaError.NA)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.NA);
        } else if (mc.getErrorValue() == FormulaError.DIV0)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.DIV0);
        } else if (mc.getErrorValue() == FormulaError.NAME)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.NAME);
        } else if (mc.getErrorValue() == FormulaError.REF)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.REF);
        } else if (mc.getErrorValue() == FormulaError.NULL)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.NULL);
        } else if (mc.getErrorValue() == FormulaError.SPILL)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.NA);
        } else if (mc.getErrorValue() == FormulaError.CALC)
        {
            cell.setCellErrorValue(org.apache.poi.ss.usermodel.FormulaError.NA);
        }
    }

    private void saveCell(XSSFCell cell, MemCell mc)
    {
        CellType ct = cell.getCachedFormulaResultType();
        if (mc.getErrorValue() != null)
        {
            setCellErrorValue(cell, mc);
            if (mc.getFormula() != null)
            {
                cell.setCellFormula(mc.getFormula());
            }
        } else if (ct == CellType.STRING)
        {
            if (mc.getStringValue() != null)
            {
                cell.setCellValue(mc.getStringValue());
            } else
            {
                cell.getRow().removeCell(cell);
            }
        } else if (ct == CellType.NUMERIC || ct == CellType.BOOLEAN)
        {
            if (mc.getNumberValue() != null)
            {
                cell.setCellValue(mc.getNumberValue().doubleValue());
            } else
            {
                cell.getRow().removeCell(cell);
            }
        }
    }

    private boolean saveCellNew(XSSFRow sheetRow, XSSFCell cell, MemCell mc, XSSFWorkbook wb, XSSFCreationHelper ch)
    {
        boolean removed = false;
        if (mc.getErrorValue() != null)
        {
            setCellErrorValue(cell, mc);
            if (mc.getFormula() != null)
            {
                cell.setCellFormula(mc.getFormula());
            }
        } else if (mc.getCellType() == MemCellType.String)
        {
            if (mc.getStringValue() != null)
            {
                var ctCell = cell.getCTCell();
                XSSFRichTextString rts = null;
                ctCell.setT(STCellType.S);
                int index = 0;
                synchronized (this)
                {
                    rts = ch.createRichTextString(mc.getStringValue());
                    index = wb.getSharedStringSource().addSharedStringItem(rts);
                }
                ctCell.setV(Integer.toString(index));
            } else
            {
                removed = true;
                sheetRow.removeCell(cell);
            }
        } else if (mc.getCellType() == MemCellType.Number)
        {
            if (mc.isBooleanColumn())
            {
                cell.setCellValue(mc.getNumberValue().intValue() != 0);
            } else if (mc.hasNumberValue())
            {
                cell.setCellValue(mc.getNumberValue().doubleValue());
            } else
            {
                sheetRow.removeCell(cell);
                removed = true;
            }
        } else if (mc.getCellType() == MemCellType.Formula)
        {
            CTCellFormula f = CTCellFormula.Factory.newInstance();
            f.setStringValue(mc.getFormula());
            cell.getCTCell().setF(f);
        }
        return removed;
    }

    public void saveSheet(XSSFSheet sheet, Map<String, XSSFCellStyle> styleMap)
    {
        for (int row = 0; row < array.length; row++)
        {
            if (array[row] == null)
                continue;
            XSSFRow sheetRow = sheet.getRow(row);
            if (sheetRow == null)
                sheetRow = sheet.createRow(row);
        }
        final XSSFWorkbook wb = (XSSFWorkbook) sheet.getWorkbook();
        final XSSFCreationHelper ch = wb.getCreationHelper();
        int length = array.length;
        if (querySheet)
        {
            // Limit query sheets to MAX_LINES_PER_QUERY_SHEET rows
            length = Math.min(length, MAX_LINES_PER_QUERY_SHEET);
        }
        IntStream.range(0, length).parallel().forEach(row ->
        {
            XSSFRow sheetRow = sheet.getRow(row);
            if (sheetRow == null || array[row] == null)
                return;
            for (int col = 0; col < array[row].length; col++)
            {
                MemCell mc = getCellAt(row, col);
                XSSFCell cell = sheetRow.getCell(col);
                if (mc != null)
                {
                    if (cell == null)
                    {
                        cell = sheetRow.createCell(col);
                    }
                    boolean removed = saveCellNew(sheetRow, cell, mc, wb, ch);
                    if (!removed && mc.getCellContext() != null)
                    {
                        MemCellStyle s = mc.getCellContext().getStyle();
                        if (s != null)
                        {
                            XSSFCellStyle style = null;
                            synchronized (this)
                            {
                                style = styleMap.get(s.getStyleKey());
                                if (style == null)
                                {
                                    style = mc.getCellContext().getStyle().getCellStyle(sheet.getWorkbook());
                                    styleMap.put(mc.getCellContext().getStyle().getStyleKey(), style);
                                }
                            }
                            cell.setCellStyle(style);
                        }
                    }
                } else
                {
                    if (cell != null)
                    {
                        sheetRow.removeCell(cell);
                        logger.debug("Removing cell: " + cell.getAddress().toString());
                    }
                }
            }
        });
        for (int row = sheet.getLastRowNum(); row > array.length; row--)
        {
            XSSFRow r = sheet.getRow(row);
            if (r != null)
            {
                sheet.removeRow(r);
            }
        }
        int numCols = getNumColumns();
        for (int col = 0; col < numCols; col++)
        {
            sheet.setColumnWidth(col, Math.min(100 * 256, getColWidth(col)));
        }
    }

    public void saveCalculations(Workbook wb)
    {
        XSSFSheet sheet = (XSSFSheet) wb.getSheet(name);
        for (Entry<CellAddress, MemCell> en : formulas.entrySet())
        {
            CellAddress ca = en.getKey();
            MemCell mc = en.getValue();
            XSSFRow row = sheet.getRow(ca.row);
            XSSFCell cell = row.getCell(ca.column);
            saveCell(cell, mc);
        }
    }

    public void setWorkbook(MemWorkbook workbook)
    {
        this.workbook = workbook;
    }

    public int getColWidth(int col)
    {
        if (col >= widths.length)
        {
            return 0;
        }
        return widths[col];
    }

    public void setColWidth(int col, int width)
    {
        if (col >= widths.length)
        {
            var newWidths = new int[col];
            System.arraycopy(widths, 0, newWidths, 0, widths.length);
            for (int i = widths.length; i < newWidths.length; i++)
            {
                newWidths[i] = width;
            }
            widths = newWidths;
            return;
        }
        widths[col] = width;
    }

    public void normalizeFirstRowForSheetlet()
    {
        int numCols = getNumColumns();
        getOrCreateCellAt(0, numCols - 1, MemCellType.String);
    }

    public boolean isQuerySheet()
    {
        return querySheet;
    }

    public void setQuerySheet(boolean querySheet)
    {
        this.querySheet = querySheet;
    }

    public int incrementAndGetNumParseErrors()
    {
        return numParseErrors.incrementAndGet();
    }

    public void addParseError(String parseError)
    {
        if (parseErrors == null)
        {
            parseErrors = new ArrayList<>();
        }
        parseErrors.add(parseError);
    }

    public boolean isQuerySheetUpdated()
    {
        return querySheetUpdated;
    }

    public void setQuerySheetUpdated(boolean querySheetUpdated)
    {
        this.querySheetUpdated = querySheetUpdated;
    }

    public Map<String, List<CellAddress>> getRangeAddressCache()
    {
        return rangeAddressCache;
    }

    public List<String> getParseErrors()
    {
        if (parseErrors == null || parseErrors.isEmpty())
            return null;
        return parseErrors;
    }

    public Map<MemCell, io.hypercell.core.expression.SpillArea> getSpillAreaCache()
    {
        return spillAreaCache;
    }

    @Override
    public Object resolveReference(String sheetName, int row, int col) {
        MemCell cell = getCellAt(row, col); // Simplification: ignoring sheet arg for now if null
        return cell;
    }

    @Override
    public java.util.List<io.hypercell.api.DataSource> getDataSources() {
        return java.util.Collections.emptyList(); // Not used in standalone version
    }

    @Override
    public void refreshDataSource(io.hypercell.api.DataSource dataSource) {
        // Not used in standalone version
    }

}