package io.hypercell.core.datatable;

import java.io.Serializable;
import java.text.DecimalFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CellFormat implements Serializable
{
    private String format;
    private String currencyPrefix;
    private String currencyValue;

    public CellFormat()
    {

    }

    public CellFormat(String format)
    {
        this.format = format;
    }

    public String getPrefix()
    {
        return currencyPrefix;
    }

    public void setPrefix(String prefix)
    {
        this.currencyPrefix = prefix;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    @JsonIgnore
    public DecimalFormat getDecimalFormat()
    {
        /*
         * IF this is an Excel format string, construct a decimal format
         */
        if (format == null)
            return null;
        String[] formats = format.split("[;]");
        try
        {
            if (formats.length == 1)
            {
                return new DecimalFormat(formats[0]);
            } else if (formats.length >= 2)
            {
                return new DecimalFormat(formats[0] + ";" + formats[1]);
            }
        } catch (IllegalArgumentException e)
        {
            return new DecimalFormat();
        }
        return null;
    }

    public String getCurrencyValue()
    {
        return currencyValue;
    }

    public void setCurrencyValue(String currencyValue)
    {
        this.currencyValue = currencyValue;
    }

}
