/**
 * TableCell for value parsing in HyperCell.
 */
package com.scoopanalytics.hypercell.core.types;

import com.scoopanalytics.hypercell.core.dateparser.DateAnalyzer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple cell that can parse and hold different types of values.
 * Used primarily for the VALUE() function to parse strings into numbers/dates.
 */
public class TableCell
{
    private String cellValue;
    private CellType type = CellType.Empty;
    private Date date;
    private Number number = null;

    /**
     * Create a TableCell from a string value, optionally parsing dates.
     *
     * @param value the string value
     * @param formatCache unused, kept for API compatibility
     * @param tryParseDateStrings whether to try parsing date strings
     */
    public TableCell(String value, Object formatCache, boolean tryParseDateStrings)
    {
        this.cellValue = value;
        getCellType(tryParseDateStrings);
    }

    private CellType getCellType(boolean tryParseDateStrings)
    {
        if (cellValue == null || cellValue.isEmpty() || cellValue.equals("-"))
        {
            type = CellType.Empty;
            return type;
        }

        if (tryParseDateStrings)
        {
            if (isDate())
            {
                type = CellType.DateTime;
                return type;
            }
        }

        if (isDecimal(true))
        {
            type = CellType.Integer;
            return type;
        }

        if (isDecimal(false))
        {
            type = CellType.Decimal;
            return type;
        }

        type = CellType.String;
        return type;
    }

    private boolean isDate()
    {
        String val = cellValue.trim();
        DateAnalyzer da = new DateAnalyzer(val);
        if (da.isAValidDate())
        {
            date = da.getDate();
            return true;
        }
        return false;
    }

    private boolean isDecimal(boolean integerOnly)
    {
        String value = cellValue;

        // Strip currency symbols for VALUE() function compatibility
        value = stripCurrencySymbols(value);

        if (!containsOnlyNumberCharacters(value))
        {
            return false;
        }

        if (integerOnly)
        {
            int decPos = value.indexOf('.');
            if (decPos >= 0 && decPos < value.length() - 1)
            {
                return false;
            }
        }

        NumberFormat nf = NumberFormat.getNumberInstance();
        if (integerOnly)
        {
            nf.setParseIntegerOnly(true);
        }
        ParsePosition pp = new ParsePosition(0);
        number = nf.parse(value, pp);
        return pp.getIndex() == value.length();
    }

    /**
     * Strip currency symbols from a value string.
     * Handles common currency symbols: $, £, €, ¥, etc.
     */
    private String stripCurrencySymbols(String value)
    {
        if (value == null || value.isEmpty())
        {
            return value;
        }
        // Remove leading/trailing whitespace first
        value = value.trim();
        // Remove common currency symbols
        // Using a simple approach - strip leading currency symbol
        if (value.length() > 0)
        {
            char first = value.charAt(0);
            if (first == '$' || first == '£' || first == '€' || first == '¥' || first == '₹')
            {
                value = value.substring(1).trim();
            }
        }
        return value;
    }

    private boolean containsOnlyNumberCharacters(String value)
    {
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9')
            {
                continue;
            }
            if (c == '.' || c == '-' || c == '+' || c == ',' || c == '%' || c == '(' || c == ')' || c == 'E' || c == 'e')
            {
                continue;
            }
            return false;
        }
        return true;
    }

    public CellType getType()
    {
        return type;
    }

    public Number getNumber()
    {
        return number;
    }

    public Date getDate()
    {
        return date;
    }

    public String getCellValue()
    {
        return cellValue;
    }
}
