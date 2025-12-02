/**
 *
 */
package io.hypercell.core.datatable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hypercell.core.datagrid.*;
import io.hypercell.core.dateparser.DateAnalyzer;
import scoop.reportseriestable.ReportSeriesTable;
import scoop.utilities.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * @author bradpeters
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TableCell
{
    static Logger logger = LoggerFactory.getLogger(TableCell.class);

    protected String cellValue;
    private CellType type = CellType.Empty;
    protected CellFormat format = null;
    private boolean hasError;
    protected transient FormatCache formatCache;

    private Date date;
    private Number number = null;
    private Boolean boolValue = null;
    //
    //
    private static final String[] CURRENCY_PREFIXES = {"$", "USD", "€", "EUR"};
    private static final String[] CURRENCY_VALUES = {"$", "$", "€", "€"};
    private static final SimpleDateFormat saveDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public TableCell()
    {
    }

    public TableCell(StringCell sCell, FormatCache formatCache, boolean tryParseDateStrings)
    {
        this.cellValue = sCell.getValue();
        this.formatCache = formatCache;
        this.hasError = sCell.isHasError();
        getCellType(tryParseDateStrings);
    }

    public TableCell(String string, FormatCache formatCache, boolean tryParseDateStrings)
    {
        this.cellValue = string;
        this.formatCache = formatCache;
        this.hasError = false;
        getCellType(tryParseDateStrings);
    }

    public TableCell(StringCell sCell)
    {
        this.cellValue = sCell.getValue();
        this.hasError = sCell.isHasError();
        this.type = CellType.String;
    }

    public TableCell(String string)
    {
        this.cellValue = string;
        this.hasError = false;
        this.type = CellType.String;
    }

    public TableCell(HTMLCell hCell, FormatCache formatCache)
    {
        this.cellValue = hCell.getCellValue();
        this.formatCache = formatCache;
        this.hasError = hCell.isHasError();
        getCellType(true);
    }

    public TableCell(NumericCell nCell)
    {
        this.cellValue = Double.toString(nCell.getValue() != null ? nCell.getValue().doubleValue() : 0);
        this.number = nCell.getValue();
        CellFormat cf = null;
        if (nCell.getFormat() != null && nCell.getFormat().getFormat() != null)
        {
            String valToSearch = nCell.getFormat().getFormat().replaceAll("\"", "");
            for (int i = 0; i < CURRENCY_PREFIXES.length; i++)
            {
                String prefix = CURRENCY_PREFIXES[i];
                if (valToSearch.startsWith(prefix))
                {
                    String val = valToSearch.replaceAll(prefix, "").trim();
                    if (!val.isEmpty())
                    {
                        cf = new CellFormat();
                        cf.setPrefix(prefix);
                        cf.setFormat(val);
                        cf.setCurrencyValue(CURRENCY_VALUES[i]);
                    }
                    break;
                }
            }
        }
        this.format = cf != null ? cf : nCell.getFormat();
        if (this.number instanceof Integer)
        {
            this.type = CellType.Integer;
        } else
        {
            this.type = cf != null ? CellType.Currency : CellType.Decimal;
        }
        this.hasError = nCell.isHasError();
    }

    public TableCell(BooleanCell bCell)
    {
        this.boolValue = bCell.getValue();
        this.cellValue = bCell.toString();
        this.type = CellType.Boolean;
    }

    public TableCell(DateCell dCell, FormatCache formatCache)
    {
        if (dCell.getDateValue() != null)
        {
            SimpleDateFormat f = null;
            if (formatCache != null)
            {
                if (formatCache.dateFormatter == null)
                {
                    formatCache.dateFormatter = (SimpleDateFormat) saveDateFormat.clone();
                }
                f = formatCache.dateFormatter;
            } else
            {
                f = (SimpleDateFormat) saveDateFormat.clone();
            }
            this.cellValue = f.format(dCell.getDateValue());
        }
        this.date = dCell.getDateValue();
        this.type = CellType.DateTime;
        this.hasError = dCell.isHasError();
    }

    public TableCell(TableCell tc)
    {
        cellValue = tc.cellValue;
        type = tc.type;
        number = tc.number;
        date = tc.date;
        hasError = tc.hasError;
        format = tc.format;
        boolValue = tc.boolValue;
    }

    public TableCell duplicate()
    {
        return new TableCell(this);
    }

    private CellFormat isCurrency()
    {
        var cv = cellValue;
        boolean negate = false;
        if (cv.startsWith("-"))
        {
            cv = cv.substring(1);
            negate = true;
        }
        for (int i = 0; i < CURRENCY_PREFIXES.length; i++)
        {
            String prefix = CURRENCY_PREFIXES[i];
            if (cv.startsWith(prefix))
            {
                String val = cv.substring(prefix.length()).trim();
                if (!val.isEmpty())
                {
                    CellFormat dformat = isDecimal(val, true, false, negate);
                    if (dformat != null)
                    {
                        CellFormat cformat = new CellFormat();
                        cformat.setFormat(dformat.getFormat());
                        cformat.setPrefix(prefix);
                        cformat.setCurrencyValue(CURRENCY_VALUES[i]);
                        return cformat;
                    }
                }
            }
        }
        return null;
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
            // Added 'E' and 'e' for scientific notation support
            if (c == '.' || c == '-' || c == '+' || c == ',' || c == '%' || c == '(' || c == ')' || c == 'E' || c == 'e')
            {
                continue;
            }
            return false;
        }
        return true;
    }

    private CellFormat isDecimal(String value, boolean createDecimal, boolean integerOnly, boolean negate)
    {
        boolean isPercentage = false;
        if (value.endsWith("%"))
        {
            isPercentage = true;
            value = value.substring(0, value.length() - 1);
        }
        if (!containsOnlyNumberCharacters(value))
        {
            return null;
        }
        if (integerOnly)
        {
            // Contains decimal places so can't be integer only
            int decPos = value.indexOf('.');
            if (decPos >= 0 && decPos < value.length() - 1)
            {
                return null;
            }
        }
        if (!negate && value.startsWith("(") && value.endsWith(")") && value.length() > 2)
        {
            negate = true;
            value = value.substring(1, value.length() - 1);
        }
        if (formatCache != null)
        {
            DecimalFormat df = integerOnly ? formatCache.integerFormatter : formatCache.decimalFormatter;
            if (df != null)
            {
                try
                {
                    if (createDecimal)
                    {
                        df.setParseBigDecimal(true);
                    }
                    Number n = df.parse(value);
                    if (createDecimal)
                    {
                        df.setParseBigDecimal(false);
                    }
                    number = n;
                    if (negate)
                    {
                        number = -number.doubleValue();
                    }
                    CellFormat cf = new CellFormat();
                    cf.setFormat(df.toPattern());
                    return cf;
                } catch (ParseException | NumberFormatException ignored)
                {
                }
            }
        }
        NumberFormat nf = formatCache == null || formatCache.locale == null ? NumberFormat.getNumberInstance() : NumberFormat.getNumberInstance(
                formatCache.locale);
        if (integerOnly)
        {
            nf.setParseIntegerOnly(true);
        } else
        {
            nf.setRoundingMode(RoundingMode.UNNECESSARY);
        }
        ParsePosition pp = new ParsePosition(0);
        number = nf.parse(value, pp);
        if (pp.getIndex() == value.length())
        {
            if (nf instanceof DecimalFormat)
            {
                DecimalFormat df = (DecimalFormat) nf;
                if (formatCache != null)
                {
                    if (integerOnly)
                        formatCache.integerFormatter = df;
                    else
                        formatCache.decimalFormatter = df;
                }
                CellFormat cf = new CellFormat();
                cf.setFormat(df.toPattern() + (isPercentage ? "%" : ""));
                return cf;
            }
        } else if (pp.getIndex() > 0 && pp.getErrorIndex() < 0)
        {
            // logger.info("Unable to parse as decimal: " + value);
        }
        return null;
    }

    private CellFormat isDate()
    {
        String val = cellValue.trim();
        if (formatCache != null)
        {
            SimpleDateFormat sdf = formatCache.dateFormatter;
            if (sdf != null)
            {
                try
                {
                    Date d = sdf.parse(val);
                    String test = sdf.format(d);
                    Date d2 = sdf.parse(test);
                    if (d.equals(d2))
                    {
                        date = d;
                        CellFormat cf = new CellFormat();
                        cf.setFormat(sdf.toPattern());
                        return cf;
                    }
                } catch (ParseException | NumberFormatException ignored)
                {
                }
            }
        }
        DateAnalyzer da = new DateAnalyzer(val);
        if (da.isAValidDate())
        {
            SimpleDateFormat sdf = new SimpleDateFormat(da.getFormatString());
            CellFormat cf = new CellFormat();
            cf.setFormat(da.getFormatString());
            date = da.getDate();
            if (formatCache != null)
            {
                formatCache.dateFormatter = sdf;
            }
            return cf;
        }
        return null;
    }

    protected CellType getCellType(boolean tryParseDateStrings)
    {
        if (cellValue == null || cellValue.isEmpty() || cellValue.equals("-"))
        {
            type = CellType.Empty;
            return type;
        }
        CellFormat cformat = null;
        if (tryParseDateStrings)
        {
            cformat = isDate();
            if (cformat != null)
            {
                type = CellType.DateTime;
                format = cformat;
                return type;
            }
        }
        cformat = isDecimal(cellValue, false, true, false);
        if (cformat != null)
        {
            type = CellType.Integer;
            return type;
        }
        cformat = isCurrency();
        if (cformat != null)
        {
            type = CellType.Currency;
            format = cformat;
            return type;
        }
        cformat = isDecimal(cellValue, false, false, false);
        if (cformat != null)
        {
            type = CellType.Decimal;
            format = cformat;
            return type;
        }
        type = CellType.String;
        return type;
    }

    public CellType getType()
    {
        return type;
    }

    public Number getNumber()
    {
        return number;
    }

    public Boolean getBoolValue()
    {
        return boolValue;
    }

    public Date getDate()
    {
        if (date == null && format !=null && cellValue != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(format.getFormat());
            try
            {
                date = sdf.parse(cellValue);
            } catch (ParseException ignored)
            {
            }
        }
        return date;
    }

    public String getCellValue()
    {
        return cellValue;
    }

    @JsonIgnore
    public String getDisplayValue()
    {
        if (type == CellType.Currency)
        {
            return format.getCurrencyValue() + number;
        }
        return cellValue;
    }

    public String getStoreValue(CellType expectedType)
    {
        if (type == CellType.Integer)
            return number == null ? "0" : Integer.toString(number.intValue());
        if (type == CellType.Currency || type == CellType.Decimal)
            return number == null ? "0" : Double.toString(number.doubleValue());
        if (expectedType == CellType.Currency || expectedType == CellType.Decimal || expectedType == CellType.Integer)
            return "0";
        if (expectedType == CellType.DateTime)
        {
            if (date != null)
                return saveDateFormat.format(date);
            else
                return "";
        }
        return cellValue;
    }

    /**
     * Sets an entry in a prepared statement as null based on an expected type
     */
    public static void setNull(CellType expectedType, int index, PreparedStatement stmt) throws SQLException
    {
        if (expectedType == CellType.Currency || expectedType == CellType.Decimal)
        {
            stmt.setNull(index, java.sql.Types.DECIMAL);
        } else if (expectedType == CellType.Integer)
        {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else if (expectedType == CellType.DateTime)
        {
            stmt.setNull(index, java.sql.Types.TIMESTAMP);
        } else if (expectedType == CellType.Boolean)
        {
            stmt.setNull(index, java.sql.Types.BOOLEAN);
        } else if (expectedType == CellType.String)
        {
            stmt.setNull(index, java.sql.Types.VARCHAR);
        }
        stmt.setNull(index, java.sql.Types.OTHER);
    }

    /**
     * Fill out a prepared statement with the data from the cell in the appropriate field index
     */
    public void setData(CellType expectedType, int index, PreparedStatement stmt) throws SQLException
    {
        if (expectedType == CellType.Empty)
            stmt.setNull(index, java.sql.Types.VARCHAR);
        else if (type == CellType.Empty)
        {
            setNull(expectedType, index, stmt);
        } else if (expectedType == CellType.String)
        {
            stmt.setString(index, cellValue);
        } else if (type == CellType.Currency)
        {
            if (number instanceof BigDecimal)
                stmt.setBigDecimal(index, (BigDecimal) number);
            else
                stmt.setDouble(index, number.doubleValue());
        } else if (type == CellType.Decimal)
        {
            stmt.setDouble(index, number.doubleValue());
        } else if (type == CellType.Integer)
        {
            stmt.setLong(index, number.longValue());
        } else if (type == CellType.Boolean)
        {
            stmt.setBoolean(index, boolValue);
        } else if (expectedType == CellType.DateTime)
        {
            if (date == null)
            {
                setNull(expectedType, index, stmt);
            } else
            {
                stmt.setTimestamp(index, new java.sql.Timestamp(date.getTime()));
            }
        } else
            stmt.setString(index, cellValue);
    }

    /**
     * Get the value that should be used to bulk load this cell
     */
    public String getBulkValue(CellType expectedType)
    {
        if (expectedType == CellType.Empty)
            return "";
        else if (type == CellType.Empty)
        {
            return "";
        } else if (expectedType == CellType.String)
        {
            return "\"" + StringUtils.escape(cellValue) + "\"";
        } else if (type == CellType.Currency)
        {
            if (number == null)
                return "0";
            if (number instanceof BigDecimal)
                return ((BigDecimal) number).toString();
            else
                return Double.toString(number.doubleValue());
        } else if (type == CellType.Decimal)
        {
            return number == null ? "0" : Double.toString(number.doubleValue());
        } else if (type == CellType.Integer)
        {
            return number == null ? "0" : Long.toString(number.longValue());
        } else if (type == CellType.Boolean)
        {
            return boolValue ? "1" : "0";
        } else if (expectedType == CellType.DateTime)
        {
            if (date == null)
            {
                return "";
            } else
            {
                SimpleDateFormat sdf = new SimpleDateFormat(ReportSeriesTable.timestampFormat);
                return sdf.format(date);
            }
        } else
            return cellValue;
    }

    /**
     * Update the cell with a new value
     */
    public void setCellValue(String cellValue)
    {
        this.cellValue = cellValue;
    }

    /**
     * Mark a cell as empty
     */
    public void setEmpty()
    {
        type = CellType.Empty;
    }

    /**
     * Whether this column spans into later rows
     */
    public boolean isColSpan()
    {
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof TableCell))
            return false;
        if (cellValue == null && ((TableCell) o).cellValue == null)
            return true;
        if (cellValue == null || ((TableCell) o).cellValue == null)
            return false;
        return cellValue.equals(((TableCell) o).cellValue);
    }

    @Override
    public String toString()
    {
        return getDisplayValue();
    }

    public CellFormat getFormat()
    {
        return format;
    }

    public void setFormat(CellFormat format)
    {
        this.format = format;
    }

    public boolean isHasError()
    {
        return hasError;
    }

    public void setHasError(boolean hasError)
    {
        this.hasError = hasError;
    }
}
