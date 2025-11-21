package io.hypercell.api;



public class RangeAddress
{
    public CellAddress start;
    public CellAddress end;
    private String stringValue;

    public RangeAddress()
    {

    }

    public RangeAddress(String excelAddress)
    {
        int split = excelAddress.indexOf(':');
        if (split < 0) return;
        String startStr = excelAddress.substring(0, split);
        String endStr = excelAddress.substring(split + 1);
        start = CellAddress.getCellAddress(startStr);
        end = CellAddress.getCellAddress(endStr);
    }

    public RangeAddress(CellAddress start, CellAddress end)
    {
        this.start = start;
        this.end = end;
    }

    /* 

    

    

     */

    @Override
    public String toString()
    {
        if (stringValue != null)
            return stringValue;
        if (start.sheetName == null && end.sheetName == null)
        {
            return start.getString(false) + ":" + end.getString(false);
        }
        if (start.sheetName != null && (end.sheetName == null || end.sheetName.equals(start.sheetName)))
        {
            return start.getString(true) + ":" + end.getString(false);
        }
        stringValue = start.toString() + ":" + end.toString();
        return stringValue;
    }
}