/**
 *
 */
package io.hypercell.core.grid;

import com.esotericsoftware.kryo.Kryo;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hypercell.core.expression.Range;
import io.hypercell.api.CellAddress;
import io.hypercell.api.RangeAddress;



import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import io.hypercell.api.FunctionRegistry;
import java.util.stream.IntStream;

/**
 * @author bradpeters
 */
public class MemWorkbook implements io.hypercell.api.WorkbookDimensions
{
    private transient FunctionRegistry registry;
    private static final Logger logger = LoggerFactory.getLogger(MemWorkbook.class);
    private String name;
    private List<MemSheet> sheets = new ArrayList<>();
    private transient Workbook workbook;
    private final transient Map<String, MemSheet> sheetMap = new HashMap<>();

    private transient Set<MemCell> changedCells = null;

    private Map<String, MemCellFont> fonts = new HashMap<>();
    private Map<String, MemCellStyle> styles = new HashMap<>();
    private Map<String, RangeAddress> ranges = new HashMap<>();
    private Map<String, String> promptMap = null;
    private boolean useIndices = false;
    private transient boolean requiresCompilation = true;
    private transient boolean processedFormulas = false;
    private transient boolean updatedData = false;
    private transient boolean refreshQueryDataOnUse = false;
    private transient Map<String, Map<String, Object>> lookupMap;
    private transient boolean skipStyle;


    public FunctionRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(FunctionRegistry registry) {
        this.registry = registry;
    }

    public MemWorkbook(io.hypercell.api.EvaluationContext context, String name, org.apache.poi.ss.usermodel.Workbook wb, boolean b) {
        this.name = name;
        this.registry = new io.hypercell.api.FunctionRegistry() {
            public void register(String name, io.hypercell.api.Function function) {}
            public io.hypercell.api.Function getFunction(String name) { return null; }
        };
    }

    @Override
    public int getNumRows() { return 1048576; }
    @Override
    public int getNumColumns() { return 16384; }

    public MemCell getCellAt(int row, int col) {
        if (sheets.isEmpty()) return null;
        return sheets.get(0).getCellAt(row, col);
    }

    public Double getDoubleValue(int row, int col) {
        MemCell cell = getCellAt(row, col);
        return cell != null ? cell.getDoubleValue() : null;
    }

    public Object getValue(int row, int col) {
        MemCell cell = getCellAt(row, col);
        return cell != null ? cell.getValue() : null;
    }

    public void calculate(io.hypercell.api.EvaluationContext context) {
        calculate();
    }


        public MemWorkbook() {}

    public MemWorkbook(MemWorkbook mw)
    {
        this.name = mw.name;
        this.sheets = new ArrayList<>();
        for (var sheet : mw.sheets)
        {
            var newMw = new MemSheet(sheet);
            this.sheets.add(newMw);
            sheetMap.put(sheet.getName(), newMw);
        }
        this.fonts = mw.fonts;
        this.styles = mw.styles;
        this.ranges = mw.ranges;
        this.useIndices = mw.useIndices;
        this.promptMap = mw.promptMap;
    }

    /**
     * @param skipStyle Skipping style in some instances for performance
     */

    public MemWorkbook(String name, Workbook workbook, boolean skipStyle)
    {
        this.name = name;
        this.workbook = workbook;
        this.skipStyle = skipStyle;
        try
        {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                XSSFSheet fieldsheet = (XSSFSheet) workbook.getSheetAt(i);
                if (fieldsheet == null)
                    return;
                int maxCol = 0;
                int maxRow = 0;
                Iterator<Row> rowit = fieldsheet.rowIterator();
                while (rowit.hasNext())
                {
                    Row row = rowit.next();
                    int lastCol = row.getLastCellNum();
                    if (lastCol < 0)
                        continue;
                    int rowNum = row.getRowNum();
                    if (rowNum > maxRow)
                        maxRow = rowNum;
                    if (lastCol > maxCol)
                        maxCol = lastCol;
                }
                MemSheet memSheet = new MemSheet(this, maxRow + 1, maxCol);
                memSheet.setName(fieldsheet.getSheetName());
                sheets.add(memSheet);
                sheetMap.put(memSheet.getName(), memSheet);
            }
            for (int i = 0; i < sheets.size(); i++)
            {
                XSSFSheet fieldsheet = (XSSFSheet) workbook.getSheetAt(i);
                MemSheet memSheet = sheets.get(i);
                final XSSFWorkbook wb = (XSSFWorkbook) workbook;
                IntStream.range(0, fieldsheet.getLastRowNum() + 1).parallel().forEach(rowNum -> {
                    Row row = fieldsheet.getRow(rowNum);
                    if (row == null)
                        return;
                    for (int col = 0; col < row.getLastCellNum(); col++)
                    {
                        XSSFCell cell = (XSSFCell) row.getCell(col);
                        if (cell == null)
                            continue;
                        if (!skipStyle)
                        {
                            memSheet.setColWidth(col, fieldsheet.getColumnWidth(col));
                        }
                        var ctCell = cell.getCTCell();
                        var ctType = ctCell.xgetT().getStringValue();
                        var ctFormula = ctCell.getF();
                        if (ctType.equals("s"))
                        {
                            String vS = ctCell.getV();
                            if (vS != null)
                            {
                                int v = Integer.parseInt(vS);
                                var ss = wb.getSharedStringSource();
                                XSSFRichTextString s = (XSSFRichTextString) ss.getItemAt(v);
                                MemCell mc = new MemCell(s.getString());
                                memSheet.setCellAt(rowNum, col, mc);
                                mc.setCellContext(cell);
                            }
                        } else if (ctType.equals("n") && ctFormula == null)
                        {
                            String numVal = ctCell.getV();
                            MemCell mc = null;
                            if (numVal != null)
                            {
                                mc = new MemCell(Double.parseDouble(numVal));
                                memSheet.setCellAt(rowNum, col, mc);
                                mc.setCellContext(cell);
                            }
                        } else if (ctType.equals("b"))
                        {
                            MemCell mc = new MemCell(cell.getBooleanCellValue());
                            memSheet.setCellAt(rowNum, col, mc);
                            mc.setCellContext(cell);
                        }
                        if (ctFormula != null)
                        {
                            CellRangeAddress arrayAddress = null;
                            try
                            {
                                arrayAddress = cell.getArrayFormulaRange();
                            } catch (IllegalStateException ignored)
                            {
                            }
                            CellType ct = cell.getCachedFormulaResultType();
                            MemCell mc = null;
                            if (ct == CellType.STRING)
                            {
                                mc = new MemCell(ctCell.getV());
                                memSheet.setCellAt(rowNum, col, mc);
                                mc.setCellContext(cell);
                            } else if (ct == CellType.NUMERIC)
                            {
                                String numVal = ctCell.getV();
                                if (numVal != null)
                                {
                                    mc = new MemCell(Double.parseDouble(numVal));
                                    memSheet.setCellAt(rowNum, col, mc);
                                    mc.setCellContext(cell);
                                    // For formula cells that are numeric and horizontal alignment is not set, right justify
                                    if (mc.getCellContext() != null && mc.getCellContext().getOrCreateStyle()
                                                                         .gethAlign() == HorizontalAlignment.GENERAL)
                                    {
                                        mc.getCellContext().getStyle().sethAlign(HorizontalAlignment.RIGHT);
                                    }
                                }
                            } else if (ct == CellType.BOOLEAN)
                            {
                                mc = new MemCell(cell.getBooleanCellValue());
                                memSheet.setCellAt(rowNum, col, mc);
                                mc.setCellContext(cell);
                            }
                            /*
                             * For now, for arrays- don't calc them, just use the embedded calculated values.
                             * Special case if the array address range is 1 wide and tall, then basically not an array
                             */
                            String formula = ctFormula.getStringValue();
                            Range sharedRange = null;
                            if (formula.isEmpty())
                            {
                                try
                                {
                                    var ft = ctFormula.getT();
                                    if (ft == STCellFormulaType.SHARED)
                                    {
                                        synchronized (MemWorkbook.class)
                                        {
                                            formula = cell.getCellFormula();
                                        }
                                    }
                                } catch (Exception ignored)
                                {

                                }
                            }
                            if (/* With new table array functions need to remove this constraint
                            (arrayAddress == null ||
                                    (arrayAddress.getFirstColumn() == arrayAddress.getLastColumn() && arrayAddress.getFirstRow() == arrayAddress.getLastRow())) &&*/
                                    ctFormula != null && formula != null && !formula.trim().isEmpty())
                            {
                                if (mc == null)
                                {
                                    mc = new MemCell(formula);
                                    mc.setCellType(MemCellType.Formula);
                                } else
                                {
                                    mc.setFormula(formula);
                                }
                                memSheet.processFormulaCell(mc);
                                mc.compileFormula(memSheet);
                                memSheet.addFormulaAt(rowNum, col, mc);
                                mc.setCellContext(cell);
                            }
                        }
                    }
                });
            }
            for (MemSheet ms : sheets)
            {
                ms.checkSpillAreas();
            }
            requiresCompilation = false;
            processAllFormulas();
            for (Name n : workbook.getAllNames())
            {
                try
                {
                    AreaReference af = new AreaReference(n.getRefersToFormula(), SpreadsheetVersion.EXCEL2007);
                    CellAddress start = new CellAddress(af.getFirstCell().getSheetName(), af.getFirstCell().getRow(),
                            af.getFirstCell().getCol());
                    CellAddress end = new CellAddress(af.getLastCell().getSheetName(), af.getLastCell().getRow(),
                            af.getLastCell().getCol());
                    ranges.put(n.getNameName(), new RangeAddress(start, end));

                } catch (IllegalArgumentException ignored)
                {
                    // Likely non-contiguous range - ignore
                }
            }
        } catch (EncryptedDocumentException e)
        {
            logger.error("Unable to compile worksheet: " + e.getMessage());
        }
    }

    public void processAllFormulas()
    {
        if (processedFormulas)
            return;
        for (MemSheet ms : sheets)
        {
            ms.processFormulas();
        }
        processedFormulas = true;
    }

    public MemSheet getSheet(String sheetName)
    {
        if (sheetName == null)
            return null;
        var result = sheetMap.get(sheetName);
        if (result == null)
        {
            result = sheetMap.get(getActualSheetName(sheetName));
            sheetMap.put(sheetName, result);
        }
        return result;
    }

    public MemSheet createSheet(String sheetName)
    {
        if (sheetName == null)
            return null;
        MemSheet memSheet = new MemSheet(this, 10, 10);
        memSheet.setName(sheetName);
        sheets.add(memSheet);
        sheetMap.put(memSheet.getName(), memSheet);
        return memSheet;
    }

    private String getActualSheetName(String sheetName)
    {
        if (sheetName.charAt(0) == '\'' && sheetName.charAt(sheetName.length() - 1) == '\'')
        {
            return sheetName.substring(1, sheetName.length() - 1);
        }
        return sheetName;
    }

    public int getNumSheets()
    {
        return sheets.size();
    }

    public MemSheet getSheet(int index)
    {
        return sheets.get(index);
    }

    public void compileFormulas()
    {
        if (!requiresCompilation)
            return;
        logger.info("Compiling formulas");
        for (MemSheet ms : sheets)
        {
            ms.compileFormulas();
        }
        for (MemSheet ms : sheets)
        {
            ms.checkSpillAreas();
        }
        processAllFormulas();
        requiresCompilation = false;
    }

    public void calculate()
    {
        if (requiresCompilation)
        {
            compileFormulas();
            requiresCompilation = false;
        }
        for (MemSheet ms : sheets)
        {
            ms.resetCalculations();
        }
        for (MemSheet ms : sheets)
        {
            ms.calculate();
        }
    }

    public void saveCalculations(Workbook wb)
    {
        for (MemSheet ms : sheets)
        {
            ms.saveCalculations(wb);
        }
    }

    public Workbook createWorkbook()
    {
        Workbook wb = null;
        try
        {
            wb = WorkbookFactory.create(true);
            Map<String, XSSFCellStyle> styleMap = new HashMap<>();
            for (MemSheet sheet : sheets)
            {
                try
                {
                    XSSFSheet xssfsheet = (XSSFSheet) wb.createSheet(sheet.getName());
                    sheet.saveSheet(xssfsheet, styleMap);
                } catch (Exception e)
                {
                    logger.error("Unable to create workbook sheet: " + e.getMessage());
                }
            }
            var helper = wb.getCreationHelper();
            if (ranges != null)
            {
                for (Map.Entry<String, RangeAddress> rangeEntry : ranges.entrySet())
                {
                    var n = wb.createName();
                    n.setNameName(rangeEntry.getKey());
                    try
                    {
                        n.setRefersToFormula(rangeEntry.getValue().toString());
                    } catch (Exception e)
                    {
                        logger.error("Unable to save range: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }
        return wb;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Set<MemCell> getChangedCells()
    {
        return changedCells;
    }

    public void setChangedCells(Set<MemCell> changedCells)
    {
        this.changedCells = changedCells;
    }

    public MemCellFont getFont(MemCellFont f)
    {
        String key = f.getFontKey();
        var mcf = fonts.get(key);
        if (mcf != null)
            return mcf;
        f.setIndex(fonts.size());
        fonts.put(key, f);
        return f;
    }

    public synchronized MemCellStyle getStyle(MemCellStyle s)
    {
        String key = s.getStyleKey();
        var mcs = styles.get(key);
        if (mcs != null)
            return mcs;
        s.setIndex(styles.size());
        styles.put(key, s);
        return s;
    }

    public int getNumFonts()
    {
        return fonts.size();
    }

    public int getNumStyles()
    {
        return styles.size();
    }

    public Collection<MemCellFont> getFonts()
    {
        return fonts.values();
    }

    public Collection<MemCellStyle> getStyles()
    {
        return styles.values();
    }

    public List<MemSheet> getSheets()
    {
        return sheets;
    }

    public void setFonts(Map<String, MemCellFont> fonts)
    {
        this.fonts = fonts;
    }

    public void setStyles(Map<String, MemCellStyle> styles)
    {
        this.styles = styles;
    }

    public void setSheets(List<MemSheet> sheets)
    {
        this.sheets = sheets;
        sheetMap.clear();
        for (var sheet : sheets)
        {
            sheetMap.put(sheet.getName(), sheet);
        }
    }

    public static Kryo getKryo()
    {
        var kryo = new Kryo();
        kryo.setReferences(true);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(MemWorkbook.class);
        kryo.register(MemSheet.class);
        kryo.register(MemCell.class);
        kryo.register(MemCellType.class);
        kryo.register(MemCell[].class);
        kryo.register(MemCell[][].class);
        kryo.register(int[].class);
        kryo.register(MemCellContext.class);
        kryo.register(MemCellStyle.class);
        kryo.register(MemCellFont.class);
        kryo.register(FormulaError.class);
        kryo.register(io.hypercell.api.CellAddress.class);
        kryo.register(io.hypercell.api.RangeAddress.class);
        // kryo.register(scoop.worksheet.memsheet.ScoopSheetObject.class);
        kryo.register(org.apache.poi.ss.usermodel.BorderStyle.class);
        kryo.register(org.apache.poi.ss.usermodel.HorizontalAlignment.class);
        kryo.register(org.apache.poi.ss.usermodel.VerticalAlignment.class);
        return kryo;
    }

    public Map<String, RangeAddress> getRanges()
    {
        return ranges;
    }

    public void setRanges(Map<String, RangeAddress> ranges)
    {
        this.ranges = ranges;
    }

    public void saveToWorkbook()
    {
        Map<String, XSSFCellStyle> styleMap = Collections.synchronizedMap(new HashMap<>());
        for (int i = 0; i < sheets.size(); i++)
        {
            XSSFSheet xssfSheet = null;
            if (workbook.getNumberOfSheets() <= i)
            {
                xssfSheet = (XSSFSheet) workbook.createSheet(sheets.get(i).getName());
            } else
            {
                xssfSheet = (XSSFSheet) workbook.getSheetAt(i);
            }
            sheets.get(i).saveSheet(xssfSheet, styleMap);
        }
    }

    public Workbook getWorkbook() { return workbook; }

public void setWorkbook(Workbook workbook)
    {
        this.workbook = workbook;
    }

    public boolean isUseIndices()
    {
        return useIndices;
    }

    public void setUseIndices(boolean useIndices)
    {
        this.useIndices = useIndices;
    }

    public boolean isRequiresCompilation()
    {
        return requiresCompilation;
    }

    public void setRequiresCompilation(boolean requiresCompilation)
    {
        this.requiresCompilation = requiresCompilation;
    }

    public Map<String, String> getPromptMap()
    {
        return promptMap;
    }

    public void setPromptMap(Map<String, String> promptMap)
    {
        this.promptMap = promptMap;
    }

    public boolean isUpdatedData()
    {
        return updatedData;
    }

    public void setUpdatedData(boolean updatedData)
    {
        this.updatedData = updatedData;
    }

    public boolean isRefreshQueryDataOnUse()
    {
        return refreshQueryDataOnUse;
    }

    public void setRefreshQueryDataOnUse(boolean refreshQueryDataOnUse)
    {
        this.refreshQueryDataOnUse = refreshQueryDataOnUse;
    }

    public synchronized Map<String, Map<String, Object>> getLookupMap()
    {
        if (lookupMap == null)
        {
            lookupMap = new ConcurrentHashMap<>();
        }
        return lookupMap;
    }

    public boolean isSkipStyle()
    {
        return skipStyle;
    }

    public void setSkipStyle(boolean skipStyle)
    {
        this.skipStyle = skipStyle;
    }

}