package com.scoopanalytics.hypercell.core.grid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class MemCellStyle
{
    private int index = -1;
    private String formatString;
    private MemCellFont font;
    private HorizontalAlignment hAlign;
    private VerticalAlignment vAlign;
    private boolean wrap;
    private BorderStyle borderTop;
    private BorderStyle borderBottom;
    private BorderStyle borderLeft;
    private BorderStyle borderRight;
    private String borderTopColor;
    private String borderBottomColor;
    private String borderLeftColor;
    private String borderRightColor;
    private String backgroundColor;

    public MemCellStyle()
    {

    }

    public MemCellStyle(MemCellStyle mcs)
    {
        this.index = mcs.index;
        this.formatString = mcs.formatString;
        this.font = mcs.font;
        this.hAlign = mcs.hAlign;
        this.vAlign = mcs.vAlign;
        this.wrap = mcs.wrap;
        this.borderTop = mcs.borderTop;
        this.borderBottom = mcs.borderBottom;
        this.borderLeft = mcs.borderLeft;
        this.borderRight = mcs.borderRight;
        this.backgroundColor = mcs.backgroundColor;
        this.borderLeftColor = mcs.borderLeftColor;
        this.borderTopColor = mcs.borderTopColor;
        this.borderRightColor = mcs.borderRightColor;
        this.borderBottomColor = mcs.borderBottomColor;
    }


    public String getStyleKey()
    {
        return formatString + '|' + (font != null ? font.getFontKey() : "") + "|" + hAlign + "|" + vAlign + "|" + wrap + "|" +
                borderBottom + "|" + borderTop + "|" + borderLeft + "|" + borderRight + "|" +
                borderLeftColor + "|" + borderRightColor + "|" + borderTopColor + "|" + borderBottomColor + "|" + backgroundColor;
    }


    public static synchronized MemCellStyle getMemCellStyle(XSSFCellStyle style, MemWorkbook mw)
    {
        var mcs = new MemCellStyle(style, mw);
        mcs = mw.getStyle(mcs);
        return mcs;
    }

    public MemCellStyle(XSSFCellStyle style, MemWorkbook mw)
    {
        if (style == null) return;
        formatString = style.getDataFormatString();
        hAlign = style.getAlignment();
        vAlign = style.getVerticalAlignment();
        wrap = style.getWrapText();
        borderTop = style.getBorderTop();
        borderBottom = style.getBorderBottom();
        borderLeft = style.getBorderLeft();
        borderRight = style.getBorderRight();
        borderTopColor = style.getTopBorderXSSFColor() == null ? null : style.getTopBorderXSSFColor()
                                                                             .getARGBHex() == null ? null :
                style.getTopBorderXSSFColor().getARGBHex().substring(2);
        borderBottomColor = style.getBottomBorderXSSFColor() == null ? null : style.getBottomBorderXSSFColor()
                                                                                   .getARGBHex() == null ? null : style.getBottomBorderXSSFColor()
                                                                                                                       .getARGBHex()
                                                                                                                       .substring(
                                                                                                                               2);
        borderLeftColor = style.getLeftBorderXSSFColor() == null ? null : style.getLeftBorderXSSFColor()
                                                                               .getARGBHex() == null ? null : style.getLeftBorderXSSFColor()
                                                                                                                   .getARGBHex()
                                                                                                                   .substring(
                                                                                                                           2);
        borderRightColor = style.getRightBorderXSSFColor() == null ? null : style.getRightBorderXSSFColor()
                                                                                 .getARGBHex() == null ? null : style.getRightBorderXSSFColor()
                                                                                                                     .getARGBHex()
                                                                                                                     .substring(
                                                                                                                             2);

        XSSFColor bgColor = style.getFillBackgroundColorColor();
        if (bgColor != null && bgColor.getARGBHex() != null)
        {
            backgroundColor = bgColor.getARGBHex().substring(2);
        }
        font = MemCellFont.getMemCellFont(style.getFont(), mw);
    }

    public synchronized XSSFCellStyle getCellStyle(Workbook wb)
    {
        XSSFCreationHelper creationHelper = (XSSFCreationHelper) wb.getCreationHelper();
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        if (formatString != null)
        {
            style.setDataFormat(creationHelper.createDataFormat().getFormat(formatString));
        }
        if (hAlign != HorizontalAlignment.GENERAL && hAlign != null)
        {
            style.setAlignment(hAlign);
        }
        if (vAlign != null)
        {
            style.setVerticalAlignment(vAlign);
        }
        style.setWrapText(wrap);
        if (borderBottom != null)
        {
            style.setBorderBottom(borderBottom);
        }
        if (borderTop != null)
        {
            style.setBorderTop(borderTop);
        }
        if (borderLeft != null)
        {
            style.setBorderLeft(borderLeft);
        }
        if (borderRight != null)
        {
            style.setBorderRight(borderRight);
        }
        if (borderBottomColor != null)
        {
            var color = creationHelper.createExtendedColor();
            color.setARGBHex("FF" + borderBottomColor);
            style.setBottomBorderColor(color);
        }
        if (borderTopColor != null)
        {
            var color = creationHelper.createExtendedColor();
            color.setARGBHex("FF" + borderTopColor);
            style.setTopBorderColor(color);
        }
        if (borderLeftColor != null)
        {
            var color = creationHelper.createExtendedColor();
            color.setARGBHex("FF" + borderLeftColor);
            style.setLeftBorderColor(color);
        }
        if (borderRightColor != null)
        {
            var color = creationHelper.createExtendedColor();
            color.setARGBHex("FF" + borderRightColor);
            style.setRightBorderColor(color);
        }
        if (backgroundColor != null)
        {
            var bgColor = creationHelper.createExtendedColor();
            bgColor.setARGBHex("FF" + backgroundColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(bgColor);
        }
        if (font != null)
        {
            style.setFont(font.getFont(wb, creationHelper));
        }
        return style;
    }

    public String getFormatString()
    {
        return formatString;
    }

    public void setFormatString(String formatString)
    {
        this.formatString = formatString;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public MemCellFont getFont()
    {
        return font;
    }

    public HorizontalAlignment gethAlign()
    {
        return hAlign;
    }

    public VerticalAlignment getvAlign()
    {
        return vAlign;
    }

    public boolean isWrap()
    {
        return wrap;
    }

    public BorderStyle getBorderTop()
    {
        return borderTop;
    }

    public BorderStyle getBorderBottom()
    {
        return borderBottom;
    }

    public BorderStyle getBorderLeft()
    {
        return borderLeft;
    }

    public BorderStyle getBorderRight()
    {
        return borderRight;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setFont(MemCellFont font)
    {
        this.font = font;
    }

    public void sethAlign(HorizontalAlignment hAlign)
    {
        this.hAlign = hAlign;
    }

    public void setvAlign(VerticalAlignment vAlign)
    {
        this.vAlign = vAlign;
    }

    public void setWrap(boolean wrap)
    {
        this.wrap = wrap;
    }

    public void setBorderTop(BorderStyle borderTop)
    {
        this.borderTop = borderTop;
    }

    public void setBorderBottom(BorderStyle borderBottom)
    {
        this.borderBottom = borderBottom;
    }

    public void setBorderLeft(BorderStyle borderLeft)
    {
        this.borderLeft = borderLeft;
    }

    public void setBorderRight(BorderStyle borderRight)
    {
        this.borderRight = borderRight;
    }

    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public String getBorderTopColor()
    {
        return borderTopColor;
    }

    public void setBorderTopColor(String borderTopColor)
    {
        this.borderTopColor = borderTopColor;
    }

    public String getBorderBottomColor()
    {
        return borderBottomColor;
    }

    public void setBorderBottomColor(String borderBottomColor)
    {
        this.borderBottomColor = borderBottomColor;
    }

    public String getBorderLeftColor()
    {
        return borderLeftColor;
    }

    public void setBorderLeftColor(String borderLeftColor)
    {
        this.borderLeftColor = borderLeftColor;
    }

    public String getBorderRightColor()
    {
        return borderRightColor;
    }

    public void setBorderRightColor(String borderRightColor)
    {
        this.borderRightColor = borderRightColor;
    }

    public JsonNode getJson(ObjectMapper mapper)
    {
        ObjectNode styleNode = mapper.createObjectNode();
        if (getFormatString() != null)
        {
            styleNode.put("format", getFormatString());
        }
        if (getFont() != null)
        {
            styleNode.put("fontIndex", getFont().getIndex());
        }
        if (gethAlign() != null && gethAlign() != HorizontalAlignment.GENERAL)
        {
            styleNode.put("hAlign", gethAlign().toString());
        }
        if (getvAlign() != null && getvAlign() != VerticalAlignment.BOTTOM)
        {
            styleNode.put("vAlign", getvAlign().toString());
        }
        if (isWrap())
        {
            styleNode.put("wrap", true);
        }
        BorderStyle bs = getBorderBottom();
        if (bs != null)
        {
            if (bs != BorderStyle.NONE)
            {
                styleNode.put("bb", true);
            }
            bs = getBorderTop();
            if (bs != BorderStyle.NONE)
            {
                styleNode.put("bt", true);
            }
            bs = getBorderLeft();
            if (bs != BorderStyle.NONE)
            {
                styleNode.put("bl", true);
            }
            bs = getBorderRight();
            if (bs != BorderStyle.NONE)
            {
                styleNode.put("br", true);
            }
        }
        String bgColor = getBackgroundColor();
        if (bgColor != null)
        {
            styleNode.put("bgcolor", bgColor);
        }
        return styleNode;
    }
}
