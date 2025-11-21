package io.hypercell.api;



public class CellAddress
{
    public String sheetName;
    public int row;
    public int column;
    boolean noRow;

    public CellAddress()
    {

    }

    public CellAddress(int row, int column)
    {
        this.row = row;
        this.column = column;
        if (row == -1)
        {
            noRow = true;
        }
    }

    public CellAddress(String sheetName, int row, int column)
    {
        this.sheetName = sheetName;
        this.row = row;
        this.column = column;
        if (row == -1)
        {
            noRow = true;
        }
    }

    public static CellAddress getCellAddress(String excelAddress)
    {
        var ca = new CellAddress();
        int sheetIndex = excelAddress.indexOf('!');
        if (sheetIndex < 0)
            sheetIndex = excelAddress.indexOf('.');
        if (sheetIndex >= 0)
        {
            ca.sheetName = excelAddress.substring(0, sheetIndex);
            if (ca.sheetName.startsWith("'") && ca.sheetName.endsWith("'"))
            {
                ca.sheetName = ca.sheetName.substring(1, ca.sheetName.length() - 1);
            }
        }

        char[] colChars = new char[3];
        char[] rowChars = new char[10];
        int colLength = 0;
        int rowLength = 0;

        for (int pos = sheetIndex + 1; pos < excelAddress.length(); pos++)
        {
            char c = excelAddress.charAt(pos);
            if (c == '$')
            {
                continue;
            }
            if (c >= 'A' && c <= 'Z')
            {
                if (rowLength > 0)
                    return null;
                if (colChars.length == colLength)
                    return null;
                colChars[colLength++] = c;
            } else if (c >= 'a' && c <= 'z')
            {
                if (rowLength > 0)
                    return null;
                if (colLength == colChars.length)
                    return null;
                c -= ('a' - 'A');
                colChars[colLength++] = c;
            } else if (c >= '0' && c <= '9')
            {
                if (colLength == 0)
                    return null;
                rowChars[rowLength++] = c;
            } else
            {
                return null;
            }
        }
        ca.row = 0;
        ca.column = 0;
        ca.noRow = true;
        for (int i = 0; i < rowLength; i++)
        {
            ca.row = (ca.row * 10) + (rowChars[i] - '0');
            ca.noRow = false;
        }
        for (int i = 0; i < colLength; i++)
        {
            if (i > 0)
            {
                ca.column = (ca.column + 1) * 26;
            }
            ca.column += colChars[i] - 'A';
        }
        // Excel row numbers are 1-based, not zero based like Java
        if (ca.row > 0)
            ca.row--;
        return ca;
    }

    public static String colCharacters(int curValue)
    {
        StringBuilder sb = new StringBuilder();
        do
        {
            int c = curValue % 26;
            sb.insert(0, (char) ((int) 'A' + c));
            curValue = (curValue / 26) - 1;
        } while (curValue >= 0);
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return (sheetName != null ? sheetName + "!" : "") + colCharacters(column) + (noRow ? "" : (row + 1));
    }

    public String getString(boolean includeSheet)
    {
        return (sheetName != null && includeSheet ? sheetName + "!" : "") + colCharacters(
                column) + (noRow ? "" : (row + 1));
    }

    public boolean isNoRow()
    {
        return noRow;
    }

    public void setNoRow(boolean noRow)
    {
        this.noRow = noRow;
    }

    @Override
    public boolean equals(Object other)
    {
        CellAddress oaddress = (CellAddress) other;
        if (sheetName != null && oaddress.sheetName != null)
        {
            if (!sheetName.equals(oaddress.sheetName))
            {
                return false;
            }
        }
        return ((noRow && oaddress.noRow) || (row == oaddress.row)) && (column == oaddress.column);
    }

}