package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import org.antlr.v4.runtime.tree.ParseTree;
import scoop.datatable.AggregationRule;
import io.hypercell.formula.HyperCellExpressionParser.ItemContext;
import io.hypercell.formula.HyperCellExpressionParser.OffsetContext;
import scoop.metric.DataSetValue;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.MemWorkbook;

public class Identifier extends ScoopExpression
{
    private String name;
    private CellAddress address;
    private int offset;
    private DataSetValue dataSetValue;
    private MemSheet parentSheet;
    private Double nullValue;
    private boolean isTableCount;
    private AggregationRule aggregationRule;

    public Identifier(String name)
    {
        this.name = name;
        this.isTableCount = true;
    }

    public Identifier(ParseTree tree, MemSheet parentSheet)
    {
        if (tree instanceof OffsetContext)
        {
            this.name = tree.getChild(1).getText().trim();
            this.offset = Integer.parseInt(tree.getChild(3).getText());
        } else if (tree instanceof ItemContext)
        {
            this.name = tree.getText().trim();
            if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'')
            {
                name = name.substring(1, name.length() - 1);
            }
            address = CellAddress.getCellAddress(name);
            if (address != null && address.sheetName != null)
            {
                this.parentSheet = parentSheet.getWorkbook().getSheet(address.sheetName);
            }
        }
        if (this.parentSheet == null)
        {
            this.parentSheet = parentSheet;
        }
    }

    public Identifier(MemWorkbook workbook, CellAddress address)
    {
        this.parentSheet = workbook.getSheet(address.sheetName);
        this.address = address;
    }

    public Identifier(MemSheet parentSheet, CellAddress address)
    {
        this.parentSheet = parentSheet;
        this.address = address;
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (parentSheet != null)
        {
            if (address == null)
            {
                return null;
            }
            MemCell mc = parentSheet.getCellAt(address);
            if (mc == null)
            {
                return null;
            }
            mc.calculate();
            return mc;
        } else
        {
            if (dataSetValue == null)
            {
                if (nullValue != null)
                {
                    return new MemCell(nullValue);
                }
                return new MemCell(FormulaError.NA);
            }
            if (offset != 0)
            {
                if (dataSetValue.row - offset < 0)
                {
                    return new MemCell(0.0);
                } else
                {
                    return new MemCell(
                            dataSetValue.dataSet.getDoubleValue(dataSetValue.row - offset, dataSetValue.column));
                }
            } else
            {
                if (dataSetValue.dataSet == null)
                {
                    return new MemCell(dataSetValue.value);
                }
                Object value = dataSetValue.dataSet.getValue(dataSetValue.row, dataSetValue.column);
                if (value instanceof String stringValue)
                {
                    return new MemCell(stringValue);
                } else if (value instanceof Number numberValue)
                {
                    return new MemCell(numberValue);
                } else if (value instanceof Boolean boolValue)
                {
                    return new MemCell(boolValue);
                }
                return new MemCell(FormulaError.NA);
            }
        }
    }

    @Override
    public String getExcelFormula()
    {
        char letterCol = 'A';
        if (address == null)
        {
            return "@NA";
        }
        return getSheetColumn(address.column + offset) + Integer.toString(address.row + 1);
    }

    private String getSheetColumn(int colNumber)
    {
        // Use the same correct algorithm as CellAddress.colCharacters()
        // Excel columns are 1-based: A=0, B=1, ..., Z=25, AA=26, AB=27, etc.
        StringBuilder sb = new StringBuilder();
        int curValue = colNumber;
        do
        {
            int c = curValue % 26;
            sb.insert(0, (char) ((int) 'A' + c));
            curValue = (curValue / 26) - 1;  // Subtract 1 to handle 1-based Excel columns
        } while (curValue >= 0);
        return sb.toString();
    }

    @Override
    public String getMetricFormula()
    {
        if (offset != 0)
        {
            return "OFFSET('" + name + "'," + offset + ")";
        }
        return "'" + name + "'";
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public DataSetValue getValue()
    {
        return dataSetValue;
    }

    public void setValue(DataSetValue value)
    {
        this.dataSetValue = value;
    }

    @Override
    public String toString()
    {
        if (name == null)
        {
            return address.toString();
        }
        return name;
    }

    public int getRow()
    {
        if (address == null)
            return 0;
        return address.row;
    }

    public void setRow(int row)
    {
        if (this.address == null)
        {
            this.address = new CellAddress(row, 0);
        } else
        {
            this.address.row = row;
        }
    }

    public int getColumn()
    {
        return address.column;
    }

    public void setColumn(int column)
    {
        if (this.address == null)
        {
            this.address = new CellAddress(0, column);
        } else
        {
            this.address.column = column;
        }
    }

    public void setNullValue(Double nullValue)
    {
        this.nullValue = nullValue;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = Math.max(offset, 0);
    }

    public MemSheet getSheet()
    {
        return parentSheet;
    }

    public void setSheet(MemSheet sheet)
    {
        this.parentSheet = sheet;
    }

    public MemCell calculateCellValue(int rowOffset, int columnOffset)
    {
        if (parentSheet == null)
        {
            return null;
        }
        if (address.sheetName != null && !parentSheet.getName().equals(address.sheetName))
        {
            return parentSheet.getCellAt(address.sheetName, address.row + rowOffset, address.column + columnOffset);
        } else
        {
            return parentSheet.getCellAt(address.row + rowOffset, address.column + columnOffset);
        }
    }

    public MemCell calculateCellValue(MemSheet memSheet, int rowOffset, int columnOffset)
    {
        if (memSheet == null)
        {
            return null;
        }
        if (address.sheetName != null && !memSheet.getName().equals(address.sheetName))
        {
            return memSheet.getCellAt(address.sheetName, address.row + rowOffset, address.column + columnOffset);
        } else
        {
            return memSheet.getCellAt(address.row + rowOffset, address.column + columnOffset);
        }
    }

    public String getSheetName()
    {
        return address.sheetName;
    }

    public CellAddress getAddress()
    {
        return address;
    }

    public void setAddress(CellAddress address)
    {
        this.address = address;
    }

    public boolean isTableCount()
    {
        return isTableCount;
    }

    public AggregationRule getAggregationRule()
    {
        return aggregationRule;
    }

    public void setAggregationRule(AggregationRule aggregationRule)
    {
        this.aggregationRule = aggregationRule;
    }
}
