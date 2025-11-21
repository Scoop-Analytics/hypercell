/**
 *
 */
package io.hypercell.core.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.formula.HyperCellExpressionParser.CELLContext;
import io.hypercell.formula.HyperCellExpressionParser.OFFSETContext;
import io.hypercell.formula.HyperCellExpressionParser.REFContext;
import io.hypercell.formula.HyperCellExpressionParser.RangeContext;
import io.hypercell.formula.HyperCellExpressionParser.TablearrayContext;
import io.hypercell.api.CellAddress;
import io.hypercell.api.RangeAddress;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;

/**
 * @author bradpeters
 */
public class Range extends AbstractExpression
{
    private MemSheet sheet;
    private Identifier startAddress;
    private Identifier endAddress;
    private RangeAddress tableArray;
    private boolean isOffset = false;
    private AbstractExpression filter;

    public Range(Identifier startAddress, Identifier endAddress)
    {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public Range(MemSheet sheet, CellAddress start, CellAddress end)
    {
        startAddress = new Identifier(sheet.getWorkbook(), start);
        endAddress = new Identifier(sheet.getWorkbook(), end);
        this.sheet = startAddress.getSheet();
        if (this.sheet == null)
        {
            this.sheet = sheet;
        }
    }

    public Range(MemCell[][] array)
    {
        this.sheet = new MemSheet(array);
        startAddress = new Identifier(this.sheet, new CellAddress(0, 0));
        endAddress = new Identifier(this.sheet, new CellAddress(array.length - 1, array[0].length - 1));
    }

    public Range(MemSheet sheet, ParseTree tree)
    {
        if (tree instanceof CELLContext)
        {
            startAddress = new Identifier(tree.getChild(0), sheet);
            this.sheet = startAddress.getSheet();
        } else if (tree instanceof REFContext)
        {
            tree = tree.getChild(0);
            if (tree instanceof RangeContext)
            {
                // Make sure it's a range, because could be an expression
                startAddress = new Identifier(tree.getChild(0), sheet);
                endAddress = new Identifier(tree.getChild(2), sheet);
                this.sheet = startAddress.getSheet();
            }
        } else if (tree instanceof RangeContext)
        {
            startAddress = new Identifier(tree.getChild(0), sheet);
            endAddress = new Identifier(tree.getChild(2), sheet);
            this.sheet = startAddress.getSheet();
        } else if (tree instanceof OFFSETContext)
        {
            startAddress = new Identifier(tree.getChild(0), sheet);
            this.sheet = startAddress.getSheet();
            isOffset = true;
        } else if (tree instanceof TablearrayContext)
        {
            tableArray = new RangeAddress(tree.getChild(0).getText());
            this.sheet = sheet != null ? sheet.getWorkbook().getSheet(tableArray.start.sheetName) : null;
        }
        if (this.sheet == null)
        {
            this.sheet = sheet;
        }
    }

    public boolean valid()
    {
        if (isOffset && startAddress != null)
            return true;
        if (startAddress != null && endAddress != null && startAddress.getAddress() != null
                && endAddress.getAddress() != null)
            return true;
        return tableArray != null;
    }

    public Identifier getStartAddress()
    {
        if (tableArray != null)
        {
            return new Identifier(sheet.getWorkbook(), tableArray.start);
        }
        return startAddress;
    }

    public Identifier getEndAddress()
    {
        if (tableArray != null)
        {
            if (tableArray.end.isNoRow())
            {
                tableArray.end.row = sheet.getNumRows() - 1;
            }
            return new Identifier(sheet.getWorkbook(), tableArray.end);
        }
        return endAddress;
    }

    public void addIdentifiers(List<Identifier> idlist)
    {
        if (tableArray != null)
        {
            return;
        }
        idlist.add(startAddress);
        if (endAddress != null)
        {
            idlist.add(endAddress);
        }
    }

    public int[] getOffsets()
    {
        if (endAddress == null)
        {
            return new int[]{startAddress.getOffset()};
        } else
        {
            int startOffset = startAddress.getOffset();
            int[] offsets = new int[endAddress.getOffset() - startOffset + 1];
            for (int i = 0; i < offsets.length; i++)
            {
                offsets[i] = startOffset + i;
            }
            return offsets;
        }
    }

    public RangePositions getRangePositions()
    {
        int rowDirection = endAddress.getRow() > startAddress.getRow() ? 1 : -1;
        int colDirection = endAddress.getColumn() > startAddress.getColumn() ? 1 : -1;
        RangePositions rp = new RangePositions();
        rp.startRow = rowDirection > 0 ? startAddress.getRow() : endAddress.getRow();
        rp.endRow = rowDirection > 0 ? endAddress.getRow() : startAddress.getRow();
        rp.startCol = colDirection > 0 ? startAddress.getColumn() : endAddress.getColumn();
        rp.endCol = colDirection > 0 ? endAddress.getColumn() : startAddress.getColumn();
        return rp;
    }

    public List<CellAddress> getAddresses()
    {
        List<CellAddress> result;
        Map<String, List<CellAddress>> cache = null;
        String key = null;
        if (tableArray != null)
        {
            cache = sheet.getRangeAddressCache();
            key = tableArray.toString();
            result = cache.get(key);
            if (result != null)
                return result;
            if (tableArray.start.isNoRow())
            {
                result = new ArrayList<>();
                for (int rowNum = 0; rowNum < sheet.getNumRows(); rowNum += 1)
                {
                    for (int colNum = tableArray.start.column; colNum <= tableArray.end.column; colNum += 1)
                    {
                        {
                            result.add(new CellAddress(sheet.getName(), rowNum, colNum));
                        }
                    }
                }
                cache.put(key, result);
                return result;
            } else
            {
                startAddress = new Identifier(sheet.getWorkbook(), tableArray.start);
                endAddress = new Identifier(sheet.getWorkbook(), tableArray.end);
            }
        }
        result = new ArrayList<>();
        if (endAddress == null)
        {
            result.add(new CellAddress(startAddress.getSheetName(), startAddress.getRow(), startAddress.getColumn()));
            return result;
        }
        RangePositions rp = getRangePositions();
        for (int rowNum = rp.startRow; rowNum <= rp.endRow; rowNum += 1)
        {
            for (int colNum = rp.startCol; colNum <= rp.endCol; colNum += 1)
            {
                {
                    result.add(new CellAddress(startAddress.getSheetName(), rowNum, colNum));
                }
            }
        }
        if (tableArray != null)
        {
            assert cache != null;
            cache.put(key, result);
        }
        return result;
    }

    public RangeAddress getRangeAddress()
    {
        return tableArray;
    }

    public CellAddress getMatchingAddress(CellAddress ca, Range otherRange)
    {
        RangePositions rp = getRangePositions();
        RangePositions otherRp = otherRange.getRangePositions();
        int row = (ca.row - otherRp.startRow) % (rp.endRow - rp.startRow + 1);
        int col = (ca.column - otherRp.startCol) % (rp.endCol - rp.startCol + 1);
        return new CellAddress(row + rp.startRow, col + rp.startCol);
    }

    @Override
    public String getMetricFormula()
    {
        if (tableArray != null)
            return tableArray.toString();
        if (endAddress == null)
        {
            return startAddress.getMetricFormula();
        }
        return startAddress.getMetricFormula() + ":" + endAddress.getMetricFormula();
    }

    @Override
    public String getExcelFormula()
    {
        if (tableArray != null)
            return tableArray.toString();
        if (endAddress == null)
        {
            return startAddress.getExcelFormula();
        }
        return startAddress.getExcelFormula() + ":" + endAddress.getExcelFormula();
    }

    @Override
    public MemCell calculateCellValue()
    {
        return null;
    }

    public AbstractExpression getFilter()
    {
        return filter;
    }

    public void setFilter(AbstractExpression filter)
    {
        this.filter = filter;
    }

    public MemSheet getSheet()
    {
        return sheet;
    }

    public void setSheet(MemSheet sheet)
    {
        this.sheet = sheet;
    }

    public boolean isTableArray()
    {
        return tableArray != null;
    }

    @Override
    public String toString()
    {
        if (startAddress != null && endAddress != null)
        {
            return startAddress.toString() + ":" + endAddress.toString();
        } else if (tableArray != null)
        {
            return tableArray.toString();
        }
        return "invalid range";
    }
}
