/**
 *
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import io.hypercell.core.expression.DateAnalyzer;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author bradpeters
 */
public class DateTimeFunction extends BaseFunctionExpression
{
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    public static ZonedDateTime getDateFromSheetNumber(double val)
    {
        val -= 25569;
        // Go from days since 1/1/1970 to ms
        val *= 1000 * 60 * 60 * 24;
        Date d = new Date((long) val);
        return d.toInstant().atZone(ZoneId.of("UTC"));
    }

    public static String getJavaDateFormatFromSheetFormat(String format)
    {
        List<String> parts = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        format = format.replace("am/pm", "am");
        int startPos = 0;
        boolean lastDelimiter = true;
        for (int i = 0; i < format.length(); i++)
        {
            char c = format.charAt(i);
            boolean isDelimiter = c == ' ' || c == ',' || c == '/' || c == '-';
            if (isDelimiter)
            {
                if (i > startPos)
                {
                    parts.add(format.substring(startPos, i));
                    positions.add(startPos);
                }
                lastDelimiter = true;
            } else
            {
                if (lastDelimiter)
                {
                    startPos = i;
                }
                lastDelimiter = false;
            }
        }
        if (startPos < format.length())
        {
            parts.add(format.substring(startPos));
            positions.add(startPos);
        }
        StringBuilder result = new StringBuilder();
        int curPos = 0;
        for (int i = 0; i < parts.size(); i++)
        {
            String part = parts.get(i);
            int pos = positions.get(i);
            if (pos > curPos)
            {
                result.append(format, curPos, pos);
            }
            if (part.charAt(0) == 'm')
            {
                if ((i > 0 && parts.get(i - 1).charAt(0) == 'h')
                        || (i < parts.size() - 1 && parts.get(i + 1).charAt(0) == 's'))
                {
                    // Interpret as minute
                    result.append(part);
                } else
                {
                    // Interpret as month
                    result.append(part.toUpperCase());
                }
            } else if (part.equals("ddd"))
            {
                result.append("EE");
            } else if (part.equals("dddd"))
            {
                result.append("EEEE");
            } else if (part.equals("am"))
            {
                result.append("a");
            } else
            {
                result.append(part);
            }
            curPos = pos + part.length();
        }
        return result.toString();
    }

    public DateTimeFunction(ParseTree tree, CompileContext cc, io.hypercell.api.FunctionRegistry registry)
    {
        super(tree, cc, registry);
    }

    public static double getSheetDateNumber(long epochSecond)
    {
        return ((double) epochSecond / (60 * 60 * 24)) + 25569;
    }

    @Override
    public io.hypercell.api.CellValue evaluate()
    {
        
        MemCell memCellResult = null;
        try
        {
            if (type == io.hypercell.formula.HyperCellExpressionParser.EOMONTHTOKEN)
            {
                MemCell startd = (MemCell)expressions.getFirst().evaluate();
                if (startd == null)
                    return new MemCell(FormulaError.NA);
                Number n = startd.getNumberValue();
                if (n == null)
                    return new MemCell(FormulaError.VALUE);
                double val = n.doubleValue();
                MemCell mcell = (MemCell)expressions.get(1).evaluate();
                if (mcell == null)
                    return new MemCell(FormulaError.NA);
                n = mcell.getNumberValue();
                if (n == null)
                    return new MemCell(FormulaError.VALUE);
                int months = n.intValue();
                ZonedDateTime startIn = getDateFromSheetNumber(val);
                if (months != 0)
                {
                    startIn = startIn.plusMonths(months);
                }
                startIn = startIn.plusMonths(1);
                startIn = startIn.withDayOfMonth(1);
                startIn = startIn.minusDays(1);
                double d = getSheetDateNumber(startIn.toEpochSecond());
                memCellResult = new MemCell(d);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.EDATETOKEN)
            {
                MemCell startd = (MemCell)expressions.getFirst().evaluate();
                if (startd == null)
                    return new MemCell(FormulaError.NA);
                Number n = startd.getNumberValue();
                if (n == null)
                    return new MemCell(FormulaError.VALUE);
                double val = n.doubleValue();
                MemCell mcell = (MemCell)expressions.get(1).evaluate();
                if (mcell == null)
                    return new MemCell(FormulaError.NA);
                n = mcell.getNumberValue();
                if (n == null)
                    return new MemCell(FormulaError.VALUE);
                int months = n.intValue();
                ZonedDateTime startIn = getDateFromSheetNumber(val);
                startIn = startIn.plusMonths(months);
                double d = getSheetDateNumber(startIn.toEpochSecond());
                memCellResult = new MemCell(d);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.DATETOKEN)
            {
                int year = ((io.hypercell.core.grid.MemCell)expressions.get(0).evaluate()).getNumberValue().intValue();
                int month = ((io.hypercell.core.grid.MemCell)expressions.get(1).evaluate()).getNumberValue().intValue();
                int day = ((io.hypercell.core.grid.MemCell)expressions.get(2).evaluate()).getNumberValue().intValue();
                LocalDate ld = LocalDate.of(year, 1, 1);
                ld = ld.plusMonths(month - 1);
                ld = ld.plusDays(day - 1);
                double d = ld.toEpochDay() + 25569;
                memCellResult = new MemCell(d);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.TIMETOKEN)
            {
                double hour = ((io.hypercell.core.grid.MemCell)expressions.get(0).evaluate()).getNumberValue().doubleValue();
                double minute = ((io.hypercell.core.grid.MemCell)expressions.get(1).evaluate()).getNumberValue().doubleValue();
                double second = ((io.hypercell.core.grid.MemCell)expressions.get(2).evaluate()).getNumberValue().doubleValue();
                double d = (hour / 24) + (minute / 24 / 60) + (second / 24 / 60 / 60);
                memCellResult = new MemCell(d);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.DATEDIFTOKEN)
            {
                double date1 = ((io.hypercell.core.grid.MemCell)expressions.get(0).evaluate()).getNumberValue().doubleValue();
                double date2 = ((io.hypercell.core.grid.MemCell)expressions.get(1).evaluate()).getNumberValue().doubleValue();
                String unit = expressions.get(2).evaluate().getStringValue();
                ZonedDateTime zdt1 = getDateFromSheetNumber(date1);
                ZonedDateTime zdt2 = getDateFromSheetNumber(date2);
                double result = 0;
                switch (unit.toUpperCase())
                {
                    case "Y":
                        result = zdt1.until(zdt2, ChronoUnit.YEARS);
                        break;
                    case "M":
                        result = zdt1.until(zdt2, ChronoUnit.MONTHS);
                        break;
                    case "D":
                        result = zdt1.until(zdt2, ChronoUnit.DAYS);
                        break;
                    case "YD":
                        zdt2 = zdt2.withYear(zdt1.getYear());
                        result = zdt1.until(zdt2, ChronoUnit.DAYS);
                        break;
                    case "YM":
                        zdt2 = zdt2.withYear(zdt1.getYear());
                        result = zdt1.until(zdt2, ChronoUnit.MONTHS);
                        break;
                    case "MD":
                        zdt2 = zdt2.withYear(zdt1.getYear());
                        zdt2 = zdt2.withMonth(zdt1.getMonthValue());
                        result = zdt1.until(zdt2, ChronoUnit.DAYS);
                        break;
                    case "H":
                        result = zdt1.until(zdt2, ChronoUnit.HOURS);
                        break;
                }
                memCellResult = new MemCell(result);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.DATEVALUETOKEN)
            {
                String dateStr = expressions.getFirst().evaluate().getStringValue();
                int year;
/*                Optional<Date> d = Optional.empty();
                if (cc.getSheet().getWorkbook() != null)
                {
                    XSSFWorkbook wb = (XSSFWorkbook) cc.getSheet().getWorkbook().getWorkbook(null, null);
                    d = wb.getPackage().getPackageProperties().getLastModifiedByProperty().getCreatedProperty();
                }
                if (d.isEmpty())
                {*/
                year = LocalDate.now().getYear();
/*                } else
                {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d.get());
                    year = c.get(Calendar.YEAR);
                }*/
                Instant in = dateStr != null ? getDateFromString(dateStr, year) : null;
                double num = getSheetDateNumber(in != null ? in.getEpochSecond() : 0);
                memCellResult = new MemCell(num);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.TIMEVALUETOKEN)
            {
                String dateStr = expressions.getFirst().evaluate().getStringValue();
                Instant in = getDateFromString(dateStr, LocalDate.now().getYear());
                if (in == null)
                {
                    String timeStr = expressions.getFirst().evaluate().getStringValue();
                    in = getTimeFromString(timeStr);
                }
                double num = getSheetDateNumber(in != null ? in.getEpochSecond() : 0);
                num = num - Math.floor(num);
                memCellResult = new MemCell(num);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.DAYTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getDayOfMonth());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.MONTHTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getMonth().getValue());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.YEARTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getYear());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.HOURTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getHour());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.MINUTETOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getMinute());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.SECONDTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                {
                    return new MemCell(FormulaError.NA);
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                memCellResult = new MemCell(zdt.getSecond());
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.NOWTOKEN)
            {
                double num = getSheetDateNumber(Instant.now().getEpochSecond());
                memCellResult = new MemCell(num);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.TODAYTOKEN)
            {
                double num = getSheetDateNumber(Instant.now().getEpochSecond());
                num = Math.floor(num);
                memCellResult = new MemCell(num);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.NETWORKDAYSTOKEN)
            {
                MemCell date1 = (MemCell)expressions.get(0).evaluate();
                MemCell date2 = (MemCell)expressions.get(1).evaluate();
                ZonedDateTime zdt1 = getDateFromSheetNumber(date1.getDoubleValue());
                ZonedDateTime zdt2 = getDateFromSheetNumber(date2.getDoubleValue());
                List<ZonedDateTime> holidays = new ArrayList<>();
                if (expressions.size() > 2)
                {
                    io.hypercell.api.Expression holidayExp = expressions.get(2);
                    if (holidayExp instanceof Range)
                    {
                        List<CellAddress> calist = ((Range) holidayExp).getAddresses();
                        for (CellAddress ca : calist)
                        {
                            MemCell mc = cc.getSheet().getCellAt(ca);
                            if (mc != null)
                            {
                                mc.calculate();
                                Number n = mc.getNumberValue();
                                if (n != null)
                                {
                                    holidays.add(getDateFromSheetNumber(n.doubleValue()));
                                }
                            }
                        }
                    } else
                    {
                        MemCell mc = (MemCell)holidayExp.evaluate();
                        ZonedDateTime holidayDT = getDateFromSheetNumber(mc.getDoubleValue());
                        holidays.add(holidayDT);
                    }
                }
                int count = 0;
                while (zdt1.compareTo(zdt2) <= 0)
                {
                    if (!holidays.contains(zdt1))
                    {
                        int dow = zdt1.get(ChronoField.DAY_OF_WEEK);
                        if (dow <= 5)
                        {
                            count++;
                        }
                    }
                    zdt1 = zdt1.plusDays(1);
                }
                memCellResult = new MemCell(count);
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.WEEKDAYTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                    return new MemCell(FormulaError.NA);
                int type = 1;
                if (expressions.size() == 2)
                {
                    var typeCell = expressions.get(1).evaluate();
                    type = typeCell.getNumberValue().intValue();
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                var dow = zdt.getDayOfWeek().getValue();
                memCellResult = switch (type)
                {
                    case 1, 17 -> new MemCell(dow + 1);  // Sunday 1 - Sat 7 (Excel)
                    case 2, 11 -> new MemCell((dow + 6) % 7 + 1); // Monday 1 - Sunday 7
                    case 3 -> new MemCell((dow + 6) % 7); // Monday 0 - Sunday 6
                    case 12 -> new MemCell((dow + 5) % 7 + 1); // Tuesday 1 - Monday 7
                    case 13 -> new MemCell((dow + 4) % 7 + 1); // Wed 1 - Tue 7
                    case 14 -> new MemCell((dow + 3) % 7 + 1); // Thu 1 - Wed 7
                    case 15 -> new MemCell((dow + 2) % 7 + 1); // Fri 1 - Thu 7
                    case 16 -> new MemCell((dow + 1) % 7 + 1); // Sat 1 - Fri 7
                    default -> new MemCell(FormulaError.NUM);
                };
            } else if (type == io.hypercell.formula.HyperCellExpressionParser.WEEKNUMTOKEN)
            {
                Number dateNum = expressions.getFirst().evaluate().getNumberValue();
                if (dateNum == null)
                    return new MemCell(FormulaError.NA);
                int type = 1;
                if (expressions.size() == 2)
                {
                    var typeCell = expressions.get(1).evaluate();
                    type = typeCell.getNumberValue().intValue();
                }
                ZonedDateTime zdt = getDateFromSheetNumber(dateNum.doubleValue());
                WeekFields weekFields =
                        switch (type)
                        {
                            case 1, 17 -> WeekFields.of(DayOfWeek.SUNDAY, 1);
                            case 2, 11, 21 -> WeekFields.of(DayOfWeek.MONDAY, 1);
                            case 12 -> WeekFields.of(DayOfWeek.TUESDAY, 1);
                            case 13 -> WeekFields.of(DayOfWeek.WEDNESDAY, 1);
                            case 14 -> WeekFields.of(DayOfWeek.THURSDAY, 1);
                            case 15 -> WeekFields.of(DayOfWeek.FRIDAY, 1);
                            case 16 -> WeekFields.of(DayOfWeek.SATURDAY, 1);
                            default -> WeekFields.of(DayOfWeek.SUNDAY, 1);
                        };
                int weekNum = zdt.get(weekFields.weekOfYear());
                memCellResult = new MemCell(weekNum);
            }
        } catch (Exception e)
        {
            return new MemCell(FormulaError.NA);
        }
        
        return memCellResult;
    }

    private Instant getDateFromString(String dateStr, int year)
    {
        DateAnalyzer da = new DateAnalyzer(dateStr, false);
        String formatString = da.getFormatString();
        if (formatString == null)
            return null;
        Date d;
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(formatString);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            d = sdf.parse(dateStr);
        } catch (ParseException e)
        {
            return null;
        }
        Instant in = d.toInstant();
        if (!da.hasYear())
        {
            ZonedDateTime zdt = in.atZone(ZoneId.of("UTC"));
            zdt = zdt.withYear(year);
            in = zdt.toInstant();
        }
        return in;
    }

    private Instant getTimeFromString(String timeStr)
    {
        DateAnalyzer da = new DateAnalyzer(timeStr, true);
        String formatString = da.getFormatString();
        if (formatString == null)
            return null;
        Date d;
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(formatString);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            d = sdf.parse(timeStr);
        } catch (ParseException e)
        {
            return null;
        }
        Instant in = d.toInstant();
        if (!da.hasYear())
        {
            ZonedDateTime zdt = in.atZone(ZoneId.of("UTC"));
            zdt = zdt.withYear(1900);
            in = zdt.toInstant();
        }
        return in;
    }
}
