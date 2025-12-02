/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;

import org.checkerframework.checker.regex.qual.Regex;
import scoop.datagrid.ExcelDataGrid;
import scoop.datatable.CellType;
import scoop.datatable.TableCell;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemCellType;

/**
 * @author bradpeters
 */
public class TextualFunction extends Function
{
    public TextualFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (memCellCalculationCache != null)
        {
            var cacheValue = memCellCalculationCache.getValue();
            if (cacheValue != null)
            {
                return cacheValue;
            }
        }
        if (type == HyperCellExpressionParser.MIDTOKEN)
        {
            MemCell text = expressions.get(0).calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            MemCell start = expressions.get(1).calculateCellValue();
            if (start == null || start.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            MemCell length = expressions.get(2).calculateCellValue();
            if (length == null || length.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            int startIndex = Math.max(0, start.getNumberValue().intValue() - 1);
            int lengthVal = length.getNumberValue().intValue();
            String val = text.getStringValue();
            if (val.length() <= startIndex)
                return getReturn(new MemCell(FormulaError.NA));
            String result = val.substring(startIndex,
                    Math.max(startIndex, Math.min(startIndex + lengthVal, val.length())));
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionParser.FINDTOKEN || type == HyperCellExpressionParser.SEARCHTOKEN)
        {
            MemCell findText = expressions.get(0).calculateCellValue();
            if (findText == null || findText.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            MemCell inText = expressions.get(1).calculateCellValue();
            if (inText == null || inText.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            MemCell start = null;
            int startIndex = 0;
            if (expressions.size() > 2)
            {
                start = expressions.get(2).calculateCellValue();
                if (start == null || start.getNumberValue() == null)
                    return getReturn(new MemCell(FormulaError.NA));
                startIndex = start.getNumberValue().intValue() - 1;
            }
            String intext = inText.getStringValue();
            String findtext = findText.getStringValue();
            if (type == HyperCellExpressionParser.SEARCHTOKEN)
            {
                intext = intext.toLowerCase();
                findtext = findtext.toLowerCase();
            }
            int result = intext.indexOf(findtext, start != null ? startIndex : 0);
            if (result < 0)
            {
                return getReturn(new MemCell(FormulaError.VALUE));
            }
            return getReturn(new MemCell(result + 1));
        } else if (type == HyperCellExpressionParser.LEFTTOKEN || type == HyperCellExpressionParser.RIGHTTOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String s = text.getStringValue();
            int num = -1;
            if (expressions.size() > 1)
            {
                MemCell numChars = expressions.get(1).calculateCellValue();
                if (numChars == null || numChars.getNumberValue() == null)
                    return getReturn(new MemCell(FormulaError.NA));
                num = numChars.getNumberValue().intValue();
                if (type == HyperCellExpressionParser.LEFTTOKEN)
                {
                    return getReturn(
                            new MemCell(s.substring(0, (num >= 0 ? Math.min(num, s.length() - 1) : s.length() - 1))));
                } else
                {
                    return getReturn(new MemCell(s.substring(s.length() - (Math.min(s.length(), Math.max(num, 0))))));
                }
            }
            if (type == HyperCellExpressionParser.LEFTTOKEN)
            {
                return getReturn(new MemCell(s.substring(0, 1)));
            } else
            {
                return getReturn(new MemCell(s.substring(s.length() - 1)));
            }
        } else if (type == HyperCellExpressionParser.LENTOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String s = text.getStringValue();
            return getReturn(new MemCell(s.length()));
        } else if (type == HyperCellExpressionParser.LOWERTOKEN || type == HyperCellExpressionParser.UPPERTOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String s = text.getStringValue();
            if (type == HyperCellExpressionParser.LOWERTOKEN)
                return getReturn(new MemCell(s.toLowerCase()));
            else
                return getReturn(new MemCell(s.toUpperCase()));
        } else if (type == HyperCellExpressionParser.PROPERTOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String s = text.getStringValue();
            boolean start = true;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if (start && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
                {
                    if ((c >= 'a' && c <= 'z'))
                    {
                        c += (char) ('A' - 'a');
                    }
                    start = false;
                } else if (c == ' ' || c == '-')
                {
                    start = true;
                } else if ((c >= 'A' && c <= 'Z'))
                {
                    c -= (char) ('A' - 'a');
                }
                result.append(c);
            }
            return getReturn(new MemCell(result.toString()));
        } else if (type == HyperCellExpressionParser.TRIMTOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String s = text.getStringValue();
            return getReturn(new MemCell(s.trim()));
        } else if (type == HyperCellExpressionParser.TEXTTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            MemCell format = expressions.get(1).calculateCellValue();
            if (format == null || format.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String fmt = format.getStringValue();
            return getReturn(new MemCell(formatValueUsingSheetFormatString(fmt, number.getNumberValue())));
        } else if (type == HyperCellExpressionParser.TEXTAFTERTOKEN || type == HyperCellExpressionParser.TEXTBEFORETOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            if (text == null || text.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String textStr = text.getStringValue();
            MemCell delimiter = expressions.get(1).calculateCellValue();
            if (delimiter == null || delimiter.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String delimiterText = delimiter.getStringValue();
            int instanceNum = 1;
            if (expressions.size() > 2)
            {
                MemCell instanceNumCell = expressions.get(2).calculateCellValue();
                if (instanceNumCell == null || instanceNumCell.getNumberValue() == null)
                    return getReturn(new MemCell(FormulaError.NA));
                instanceNum = instanceNumCell.getNumberValue().intValue();
            }
            int matchMode = 0;
            if (expressions.size() > 3)
            {
                MemCell matchModeCell = expressions.get(3).calculateCellValue();
                if (matchModeCell == null || matchModeCell.getNumberValue() == null)
                    return getReturn(new MemCell(FormulaError.NA));
                matchMode = matchModeCell.getNumberValue().intValue();
            }
            MemCell ifNotFound = null;
            if (expressions.size() > 5)
            {
                ifNotFound = expressions.get(5).calculateCellValue();
            }
            MemCell result = null;
            int pos = 0;
            String compTextStr = textStr;
            String compDelStr = delimiterText;
            if (matchMode == 1)
            {
                compTextStr = textStr.toLowerCase();
                compDelStr = delimiterText.toLowerCase();
            }
            if (instanceNum > 0)
            {
                for (int i = 0; i < instanceNum; i++)
                {
                    if (compDelStr.isEmpty())
                        continue;
                    pos = compTextStr.indexOf(compDelStr, pos + 1);
                    if (pos < 0)
                        break;
                }
            } else
            {
                pos = compTextStr.length();
                for (int i = 0; i < -instanceNum; i++)
                {
                    if (compDelStr.isEmpty())
                        continue;
                    pos = compTextStr.lastIndexOf(compDelStr, pos - 1);
                    if (pos < 0)
                        break;
                }
            }
            if (pos >= 0)
            {
                if (type == HyperCellExpressionParser.TEXTAFTERTOKEN)
                    result = new MemCell(textStr.substring(pos + delimiterText.length()));
                else
                    result = new MemCell(textStr.substring(0, pos));
            } else
            {
                result = ifNotFound != null ? ifNotFound : new MemCell(FormulaError.NA);
            }
            return getReturn(result);
        } else if (type == HyperCellExpressionParser.TEXTJOINTOKEN)
        {
            MemCell delimiter = null;
            Range delRange = null;
            String delString = null;
            if (expressions.get(0) instanceof Range)
            {
                delRange = (Range) expressions.getFirst();
            } else
            {
                delimiter = expressions.getFirst().calculateCellValue();
                delString = getStringValue(delimiter, false);
                if (delString == null)
                {
                    return getReturn(new MemCell(FormulaError.NA));
                }
            }
            ScoopExpression igexp = expressions.get(1);
            MemCell ignoreEmpty = igexp.calculateCellValue();
            if (ignoreEmpty == null || ignoreEmpty.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            int ignoreEmptyNum = ignoreEmpty.getNumberValue().intValue();
            StringBuilder sb = new StringBuilder();
            List<MemCell> mcList = new ArrayList<>();
            if (expressions.get(2) instanceof Range range)
            {
                List<CellAddress> addresses = new ArrayList<>(((Range) expressions.get(2)).getAddresses());
                List<CellAddress> delAddresses = new ArrayList<>();
                if (delRange != null)
                {
                    List<CellAddress> alist = delRange.getAddresses();
                    delAddresses.addAll(alist);
                }
                CellAddress minAdd = new CellAddress(Integer.MAX_VALUE, Integer.MAX_VALUE);
                CellAddress maxAdd = new CellAddress(0, 0);
                for (CellAddress ca : addresses)
                {
                    if (ca.row < minAdd.row)
                        minAdd.row = ca.row;
                    if (ca.column < minAdd.column)
                        minAdd.column = ca.column;
                    if (ca.row > maxAdd.row)
                        maxAdd.row = ca.row;
                    if (ca.column > maxAdd.column)
                        maxAdd.column = ca.column;
                }
                String lastDelimiter = null;
                for (int aindex = 0; aindex < addresses.size(); aindex++)
                {
                    CellAddress ca = addresses.get(aindex);
                    MemCell mc = cc.getSheet().getCellAt(ca);
                    String val = "";
                    if (mc == null)
                    {
                        if (ignoreEmptyNum > 0)
                            continue;
                    } else
                    {
                        mc.calculate();
                        val = getStringValue(mc, false);
                        if (val == null)
                        {
                            val = "";
                        }
                    }
                    if (ignoreEmptyNum > 0 && val.isEmpty())
                        continue;
                    if (aindex > 0 && lastDelimiter != null)
                    {
                        sb.append(lastDelimiter);
                    }
                    if (delRange != null)
                    {
                        CellAddress delca = delRange.getMatchingAddress(ca, new Range(cc.getSheet(), minAdd, maxAdd));
                        mc = cc.getSheet().getCellAt(delca);
                        lastDelimiter = getStringValue(mc, false);
                    } else
                    {
                        lastDelimiter = delString;
                    }
                    sb.append(val);
                }
            } else
            {
                MemCell mc = expressions.get(2).calculateCellValue();
                var array = mc.getArray();
                boolean first = true;
                if (array != null)
                {
                    for (MemCell[] memCells : array)
                    {
                        for (int col = 0; col < memCells.length; col++)
                        {
                            MemCell arraymc = memCells[col];
                            if (ignoreEmptyNum > 0 && arraymc == null)
                                continue;
                            String val = getStringValue(arraymc, false);
                            if (ignoreEmptyNum > 0 && val.isEmpty())
                                continue;
                            if (first)
                                first = false;
                            else
                                sb.append(delString);
                            sb.append(val);
                        }
                    }
                }
            }
            return getReturn(new MemCell(sb.toString()));
        } else if (type == HyperCellExpressionParser.REPLACETOKEN)
        {
            MemCell oldtext = expressions.getFirst().calculateCellValue();
            String s = getStringValue(oldtext, false);
            if (s == null)
            {
                return getReturn(new MemCell(FormulaError.NA));
            }
            MemCell startNum = expressions.get(1).calculateCellValue();
            if (startNum == null || startNum.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            int startnum = startNum.getNumberValue().intValue();
            MemCell numChars = expressions.get(2).calculateCellValue();
            if (numChars == null || numChars.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            int numchars = numChars.getNumberValue().intValue();
            MemCell newtext = expressions.get(3).calculateCellValue();
            if (newtext == null || newtext.getStringValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            String n = newtext.getStringValue();
            String sb = s.substring(0, startnum - 1) +
                        n +
                        s.substring(startnum + numchars - 1);
            return getReturn(new MemCell(sb));
        } else if (type == HyperCellExpressionParser.SUBSTITUTETOKEN)
        {
            MemCell text = expressions.getFirst().calculateCellValue();
            String textStr = getStringValue(text, false);
            if (textStr == null)
            {
                return getReturn(new MemCell(FormulaError.NA));
            }
            MemCell oldtext = expressions.get(1).calculateCellValue();
            String oldtextStr = getStringValue(oldtext, false);
            if (oldtextStr == null)
            {
                return getReturn(new MemCell(FormulaError.NA));
            }
            MemCell newtext = expressions.get(2).calculateCellValue();
            String newtextStr = getStringValue(newtext, false);
            if (newtextStr == null)
            {
                return getReturn(new MemCell(FormulaError.NA));
            }
            if (expressions.size() > 3)
            {
                MemCell instanceNum = expressions.get(3).calculateCellValue();
                if (instanceNum == null || instanceNum.getNumberValue() == null)
                    return new MemCell(FormulaError.NA);
                int instance = instanceNum.getNumberValue().intValue();
                int pos = 0;
                int count = 0;
                int foundpos = 0;
                while (count++ < instance && pos >= 0)
                {
                    foundpos = textStr.indexOf(oldtextStr, pos++);
                    if (foundpos >= 0)
                        pos = foundpos + 1;
                }
                String s = textStr.substring(0, foundpos) + newtextStr
                           + textStr.substring(foundpos + oldtextStr.length());
                return getReturn(new MemCell(s));
            } else
            {
                return getReturn(new MemCell(textStr.replace(oldtextStr, newtextStr)));
            }
        } else if (type == HyperCellExpressionParser.VALUETOKEN)
        {
            MemCell valuemc = expressions.getFirst().calculateCellValue();
            if (valuemc == null)
                return null;
            if (valuemc.getCellType() == MemCellType.Number)
            {
                return getReturn(valuemc);
            } else if (valuemc.getCellType() == MemCellType.Formula)
            {
                if (valuemc.getNumberValue() != null)
                {
                    return getReturn(valuemc);
                }
            }
            TableCell tc = new TableCell(valuemc.getStringValue(), null, true);
            var type = tc.getType();
            if (type.isNumber())
            {
                return getReturn(new MemCell(tc.getNumber()));
            }
            if (type == CellType.DateTime)
            {
                return getReturn(new MemCell(DateTimeFunction.getSheetDateNumber(tc.getDate().getTime() / 1000)));
            }
            String str = tc.getCellValue();
            if (str != null)
            {
                // See if it is a time
                Pattern p = Pattern.compile("\\d\\d:\\d\\d:\\d\\d");
                if (p.matcher(str).matches())
                {
                    double hours = Integer.parseInt(str.substring(0, 2));
                    double min = Integer.parseInt(str.substring(3, 5));
                    double sec = Integer.parseInt(str.substring(6, 8));
                    double val = (hours / 24) + (min / (24 * 60)) + (sec / (24 * 60 * 60));
                    return new MemCell(val);
                }
            }
            return getReturn(new MemCell(FormulaError.VALUE));
        } else if (type == HyperCellExpressionParser.CONCATENATETOKEN)
        {
            StringBuilder result = new StringBuilder();
            for (var expression : expressions)
            {
                MemCell cell = expression.calculateCellValue();
                if (cell != null)
                {
                    result.append(cell.getStringValue());
                }
            }
            return getReturn(new MemCell(result.toString()));
        } else if (type == HyperCellExpressionParser.REGEXREPLACETOKEN)
        {
            ScoopExpression textExpr = expressions.get(0);
            ScoopExpression patterExpr = expressions.get(1);
            ScoopExpression replacementExpr = expressions.get(2);
            String text = textExpr.calculateCellValue().getStringValue();
            String patterText = patterExpr.calculateCellValue().getStringValue();
            String replacementText = replacementExpr.calculateCellValue().getStringValue();
            String result = text.replaceAll(patterText, replacementText);
            return getReturn(new MemCell(result));
        }
        return null;
    }

    private MemCell getReturn(MemCell result)
    {
        if (memCellCalculationCache != null)
        {
            memCellCalculationCache.cacheValue(result);
        }
        return result;
    }


    public static String formatValueUsingSheetFormatString(String fmt, Number numberValue)
    {
        String s = null;
        if (ExcelDataGrid.isExcelDateFormat(fmt))
        {
            ZonedDateTime zdt = DateTimeFunction.getDateFromSheetNumber(
                    numberValue.doubleValue());
            String jfmt = DateTimeFunction.getJavaDateFormatFromSheetFormat(
                    ExcelDataGrid.removeBadDateFormatCharacters(fmt.toLowerCase()));
            SimpleDateFormat sdf = new SimpleDateFormat(jfmt);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            s = sdf.format(Date.from(zdt.toInstant()));
        } else
        {
            CellFormat cf = CellFormat.getInstance(fmt);
            CellFormatResult cfr = cf.apply(numberValue);
            s = cfr.text;
        }
        return s;
    }

    public static String formatValueUsingSheetFormatString(String fmt, Date date)
    {
        String jfmt = DateTimeFunction.getJavaDateFormatFromSheetFormat(fmt.toLowerCase());
        SimpleDateFormat sdf = new SimpleDateFormat(jfmt);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}
