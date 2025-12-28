package io.hypercell.core.grid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;

public class MemCellFont
{
    private int index = -1;
    private boolean bold;
    private boolean italic;
    private String color;
    private String fontFamily;
    private int height;

    public MemCellFont()
    {

    }

    public MemCellFont(MemCellFont font)
    {
        this.bold = font.bold;
        this.italic = font.italic;
        this.color = font.color;
        this.fontFamily = font.fontFamily;
        this.height = font.height;
    }
    public static MemCellFont getMemCellFont(XSSFFont f, MemWorkbook mw)
    {
        MemCellFont mcf = new MemCellFont(f);
        mcf = mw.getFont(mcf);
        return mcf;
    }

    public MemCellFont(XSSFFont f)
    {
        this.bold = f.getBold();
        this.italic = f.getItalic();
        XSSFColor fColor = f.getXSSFColor();
        if (fColor != null)
        {
            var strColor = fColor.getARGBHex().substring(2);
            if (!strColor.equals("000000"))
                this.color = strColor;
        }
        var fName = f.getFontName();
        if (fName != null)
        {
            this.fontFamily = fName;
        }
        this.height = f.getFontHeight() / 20;
    }

    public XSSFFont getFont(Workbook wb, XSSFCreationHelper creationHelper)
    {
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(bold);
        f.setItalic(italic);
        f.setFontHeightInPoints((short) height);
        if (color != null)
        {
            var fColor = creationHelper.createExtendedColor();
            fColor.setARGBHex("FF" + color);
            f.setColor(fColor);
        }
        return f;
    }

    public String getFontKey()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(height);
        if (bold)
            sb.append("b");
        if (italic)
            sb.append("i");
        if (color != null)
            sb.append(color);
        if (fontFamily != null)
            sb.append(fontFamily);
        return sb.toString();
    }

    public boolean isBold()
    {
        return bold;
    }

    public boolean isItalic()
    {
        return italic;
    }

    public String getColor()
    {
        return color;
    }

    public String getFontFamily()
    {
        return fontFamily;
    }

    public int getHeight()
    {
        return height;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public void setBold(boolean bold)
    {
        this.bold = bold;
    }

    public void setItalic(boolean italic)
    {
        this.italic = italic;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void setFontFamily(String fontFamily)
    {
        this.fontFamily = fontFamily;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public JsonNode getJson(ObjectMapper mapper) {
        ObjectNode fontNode = mapper.createObjectNode();
        if (isBold())
        {
            fontNode.put("bold", true);
        }
        if (isItalic())
        {
            fontNode.put("italic", true);
        }
        String fColor = getColor();
        if (fColor != null)
        {
            if (!fColor.equals("000000"))
            {
                fontNode.put("fcolor", fColor);
            }
        }
        var fName = getFontFamily();
        if (fName != null)
        {
            if (fName.equals("Calibri"))
            {
                fName = "Roboto,RobotoDraft,Helvetica,Arial,sans-serif";
            }
            fontNode.put("family", fName);
        }
        fontNode.put("height", getHeight());
        return fontNode;
    }
}
