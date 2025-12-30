package io.hypercell.core.grid;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.util.HashMap;
import java.util.Map;

public class MemCellContext
{
    private MemCellStyle style;
    private int row;
    private int col;
    private boolean editable;
    private boolean checkBox;
    private boolean isDate;
    
    private String elementID;
    private Object userObject;

    private Map<String, String> properties;

    public Object getUserObject() { return userObject; }
    public void setUserObject(Object userObject) { this.userObject = userObject; }
    public MemCellContext()
    {

    }

    public MemCellContext(Cell cell, MemWorkbook workbook)
    {
        if (!workbook.isSkipStyle())
        {
            XSSFCellStyle cs = (XSSFCellStyle) cell.getCellStyle();
            style = MemCellStyle.getMemCellStyle(cs, workbook);
        }
        row = cell.getAddress().getRow();
        col = cell.getAddress().getColumn();
    }

    public MemCellContext(XSSFCellStyle cs, MemWorkbook workbook, int row, int col)
    {
        style = MemCellStyle.getMemCellStyle(cs, workbook);
        this.row = row;
        this.col = col;
    }

    public MemCellContext(int row, int column)
    {
        this.row = row;
        this.col = column;
    }

    public int getRow()
    {
        return row;
    }

    public int getCol()
    {
        return col;
    }

    public void setStyle(MemCellStyle style)
    {
        this.style = style;
    }

    public MemCellStyle getStyle()
    {
        return style;
    }

    public MemCellStyle getOrCreateStyle()
    {
        if (style == null)
        {
            style = new MemCellStyle();
        }
        return style;
    }

    public void setRow(int row)
    {
        this.row = row;
    }

    public void setCol(int col)
    {
        this.col = col;
    }

    public boolean isEditable()
    {
        return editable;
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    public boolean isCheckBox()
    {
        return checkBox;
    }

    public void setCheckBox(boolean checkBox)
    {
        this.checkBox = checkBox;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = properties;
    }

    public void addProperty(String key, String value)
    {
        if (properties == null)
        {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    public String getProperty(String key)
    {
        if (properties == null)
            return null;
        return properties.get(key);
    }

    

    

    public String getElementID()
    {
        return elementID;
    }

    public void setElementID(String elementID)
    {
        this.elementID = elementID;
    }

    public boolean isDate()
    {
        return isDate;
    }

    public void setDate(boolean date)
    {
        isDate = date;
    }
}
