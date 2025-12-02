/**
 *
 */
package io.hypercell.core.dateparser;
import io.hypercell.formula.HyperCellDateParser;
import io.hypercell.formula.HyperCellDateLexer;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.BitSet;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import scoop.ScoopException;
import io.hypercell.formula.HyperCellDateParser.AmpmContext;
import io.hypercell.formula.HyperCellDateParser.DatepartsepContext;
import io.hypercell.formula.HyperCellDateParser.DatetimesepContext;
import io.hypercell.formula.HyperCellDateParser.DayContext;
import io.hypercell.formula.HyperCellDateParser.HourContext;
import io.hypercell.formula.HyperCellDateParser.MinContext;
import io.hypercell.formula.HyperCellDateParser.MonthContext;
import io.hypercell.formula.HyperCellDateParser.MonthnameContext;
import io.hypercell.formula.HyperCellDateParser.SecContext;
import io.hypercell.formula.HyperCellDateParser.ShortyearContext;
import io.hypercell.formula.HyperCellDateParser.StartContext;
import io.hypercell.formula.HyperCellDateParser.TimeContext;
import io.hypercell.formula.HyperCellDateParser.TimezoneContext;
import io.hypercell.formula.HyperCellDateParser.YearContext;

/**
 * @author bradpeters
 */
public class DateAnalyzer
{
    private boolean parsed = false;
    private boolean isTime = false;
    private final String dateStr;
    private String formatString;
    private boolean hasDay;
    private boolean hasMonth;
    private boolean hasHour;
    private boolean hasMinute;
    private boolean hasSecond;
    private boolean hasYear;
    private boolean hasAMPM;
    private boolean longTimeZone;
    private String timeZone;
    private boolean parsedSuccessfully = true;
    private int year;
    private int monthOfYear;
    private int dayOfMonth;
    private int hour;
    private int min;
    private int sec;
    private ZoneId zoneId = ZoneId.systemDefault();

    public DateAnalyzer(String dateStr, boolean isTime)
    {
        this.dateStr = dateStr;
        this.isTime = isTime;
    }

    public DateAnalyzer(String dateStr)
    {
        this.dateStr = dateStr;
    }

    private static class ParseError implements ANTLRErrorListener
    {
        private final AtomicBoolean hasError;

        public ParseError(AtomicBoolean hasError)
        {
            this.hasError = hasError;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String msg, RecognitionException e)
        {
            hasError.set(true);
        }

        @Override
        public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
                                    BitSet ambigAlts, ATNConfigSet configs)
        {
            hasError.set(true);
        }

        @Override
        public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                BitSet conflictingAlts, ATNConfigSet configs)
        {
        }

        @Override
        public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
                                             ATNConfigSet configs)
        {
        }
    }

    private void parse()
    {
        if (!parsed)
        {
            if (isTime)
            {
                parseTimeFormat();
            } else
            {
                parseDateFormat();
            }
        }
    }


    private void parseDateFormat()
    {
        if (dateStr.length() > 32)
        {
            // Ignore long strings
            parsedSuccessfully = false;
            return;
        }
        PrintStream _err = System.err;
        try
        {
            /*
             * Eliminate annoying error messages
             */
            CharStream input = CharStreams.fromString(dateStr);
            HyperCellDateLexer lex = null;
            AtomicBoolean hasError = new AtomicBoolean(false);
            ParseError parserErrors = new ParseError(hasError);
            lex = new HyperCellDateLexer(input);
            lex.removeErrorListeners();
            lex.addErrorListener(parserErrors);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            HyperCellDateParser scoopparser = new HyperCellDateParser(tokens);
            scoopparser.removeErrorListeners();
            scoopparser.addErrorListener(parserErrors);
            StartContext sc = scoopparser.start();
            if (!lex._hitEOF)
            {
                parsedSuccessfully = false;
                return;
            }
            if (sc.exception != null || hasError.get())
            {
                parsedSuccessfully = false;
                return;
            }
            StringBuilder formatStr = new StringBuilder();
            for (int i = 0; i < sc.getChildCount(); i++)
            {
                ParseTree child = sc.getChild(i);
                if (child instanceof ParserRuleContext && ((ParserRuleContext) child).exception != null)
                {
                    parsedSuccessfully = false;
                    return;
                }
                processParserRule(child, formatStr);
            }
            formatString = formatStr.toString();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatString);
        } catch (Exception e)
        {
            parsedSuccessfully = false;
        } finally
        {
            parsed = true;
            System.setErr(_err);
        }
    }

    private void parseTimeFormat()
    {
        PrintStream _err = System.err;
        try
        {
            /*
             * Eliminate annoying error messages
             */
            CharStream input = CharStreams.fromString(dateStr);
            HyperCellDateLexer lex = new HyperCellDateLexer(input);
            lex.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lex);
            HyperCellDateParser scoopparser = new HyperCellDateParser(tokens);
            scoopparser.removeErrorListeners();
            AtomicBoolean hasError = new AtomicBoolean(false);
            ParseError parserErrors = new ParseError(hasError);
            scoopparser.addErrorListener(parserErrors);
            TimeContext sc = scoopparser.time();
            if (!lex._hitEOF)
            {
                parsedSuccessfully = false;
                return;
            }
            if (sc.exception != null || hasError.get())
            {
                parsedSuccessfully = false;
                return;
            }
            StringBuilder formatStr = new StringBuilder();
            for (int i = 0; i < sc.getChildCount(); i++)
            {
                ParseTree child = sc.getChild(i);
                if (child instanceof ParserRuleContext && ((ParserRuleContext) child).exception != null)
                {
                    parsedSuccessfully = false;
                    return;
                }
                processParserRule(child, formatStr);
            }
            formatString = formatStr.toString();
        } finally
        {
            parsed = true;
            System.setErr(_err);
        }
    }

    private void processParserRule(ParseTree child, StringBuilder formatStr)
    {
        if (child instanceof MonthnameContext)
        {
            formatStr.append("MMMM");
            switch (((TerminalNodeImpl) child.getChild(0)).symbol.getType())
            {
                case HyperCellDateLexer.JANTOKEN:
                    monthOfYear = 1;
                    break;
                case HyperCellDateLexer.FEBTOKEN:
                    monthOfYear = 2;
                    break;
                case HyperCellDateLexer.MARTOKEN:
                    monthOfYear = 3;
                    break;
                case HyperCellDateLexer.APRILTOKEN:
                    monthOfYear = 4;
                    break;
                case HyperCellDateLexer.MAYTOKEN:
                    monthOfYear = 5;
                    break;
                case HyperCellDateLexer.JUNETOKEN:
                    monthOfYear = 6;
                    break;
                case HyperCellDateLexer.JULYTOKEN:
                    monthOfYear = 7;
                    break;
                case HyperCellDateLexer.AUGTOKEN:
                    monthOfYear = 8;
                    break;
                case HyperCellDateLexer.SEPTOKEN:
                    monthOfYear = 9;
                    break;
                case HyperCellDateLexer.OCTTOKEN:
                    monthOfYear = 10;
                    break;
                case HyperCellDateLexer.NOVTOKEN:
                    monthOfYear = 11;
                    break;
                case HyperCellDateLexer.DECTOKEN:
                    monthOfYear = 12;
                    break;
            }
            hasMonth = true;
        } else if (child instanceof MonthContext)
        {
            formatStr.append("MM");
            monthOfYear = Integer.parseInt(child.getText());
            hasMonth = true;
        } else if (child instanceof DayContext)
        {
            formatStr.append("dd");
            dayOfMonth = Integer.parseInt(child.getText());
            hasDay = true;
        } else if (child instanceof ShortyearContext)
        {
            String text = ((ShortyearContext) child).getText();
            if (text.length() == 2)
            {
                formatStr.append("yy");
            }
            year = Integer.parseInt(child.getText()) + 2000;
            hasYear = true;
        } else if (child instanceof YearContext)
        {
            String text = ((YearContext) child).getText();
            if (text.length() == 4)
            {
                formatStr.append("yyyy");
            }
            year = Integer.parseInt(child.getText());
            hasYear = true;
        } else if (child instanceof HourContext)
        {
            formatStr.append("hh");
            hour = Integer.parseInt(child.getText());
            hasHour = true;
        } else if (child instanceof MinContext)
        {
            formatStr.append("mm");
            min = Integer.parseInt(child.getText());
            hasHour = true;
        } else if (child instanceof SecContext)
        {
            if (((SecContext) child).exception != null)
            {
                parsedSuccessfully = false;
                return;
            }
            formatStr.append("ss");
            sec = (int) Double.parseDouble(child.getText());
            hasHour = true;
        } else if (child instanceof AmpmContext)
        {
            formatStr.append("a");
            if (((TerminalNodeImpl) child.getChild(0)).symbol.getType() == HyperCellDateLexer.PMTOKEN)
            {
                hour += 11; // 24 hour time is 0-23, whereas 12 hour is 1-12
            }
            hasAMPM = true;
        } else if (child instanceof TimezoneContext)
        {
            if (child.getChild(0) instanceof HyperCellDateParser.Timezone_uslongContext)
            {
                longTimeZone = true;
                switch (((TerminalNodeImpl) child.getChild(0).getChild(0)).symbol.getType())
                {
                    case HyperCellDateLexer.TIMEZONE_PACIFIC:
                        zoneId = ZoneId.of("America/Los_Angeles");
                        break;
                    case HyperCellDateLexer.TIMEZONE_MOUNTAIN:
                        zoneId = ZoneId.of("America/Denver");
                        break;
                    case HyperCellDateLexer.TIMEZONE_CENTRAL:
                        zoneId = ZoneId.of("America/Chicago");
                        break;
                    case HyperCellDateLexer.TIMEZONE_EASTERN:
                        zoneId = ZoneId.of("America/New_York");
                        break;
                }
                formatStr.append("z");
            } else
            {
                String tzStr = child.getText();
                try
                {
                    // See if we can use standard zones
                    zoneId = ZoneId.of(tzStr);
                } catch (ZoneRulesException ignored)
                {
                    if (tzStr.equalsIgnoreCase("PDT"))
                    {
                        zoneId = ZoneId.of("America/Los_Angeles");
                    } else if (tzStr.equalsIgnoreCase("MDT"))
                    {
                        zoneId = ZoneId.of("America/Denver");
                    } else if (tzStr.equalsIgnoreCase("CDT"))
                    {
                        zoneId = ZoneId.of("America/Chicago");
                    } else if (tzStr.equalsIgnoreCase("EDT"))
                    {
                        zoneId = ZoneId.of("America/New_York");
                    } else
                    {
                        TimeZone tz = TimeZone.getTimeZone(tzStr);
                        zoneId = tz.toZoneId();
                    }
                }
            }
            formatStr.append("z");
            timeZone = child.getText();
        } else if (child instanceof TerminalNodeImpl)
        {
            formatStr.append(((TerminalNodeImpl) child).getText());
        } else if (child instanceof DatepartsepContext || child instanceof DatetimesepContext)
        {
            if (child.getChildCount() > 0)
            {
                String text = ((TerminalNodeImpl) child.getChild(0)).getText();
                if (child instanceof DatetimesepContext && text.equals("T"))
                {
                    formatStr.append("'T'");
                } else
                {
                    formatStr.append(text);
                }
            }
        } else if (child instanceof TimeContext)
        {
            for (int i = 0; i < child.getChildCount(); i++)
            {
                ParseTree tchild = child.getChild(i);
                if (tchild instanceof ParserRuleContext && ((ParserRuleContext) tchild).exception != null)
                {
                    parsedSuccessfully = false;
                    return;
                }
                processParserRule(tchild, formatStr);
            }
        }
    }

    public Date getDate()
    {
        parse();
        // Parsed values
        if (!hasYear)
        {
            year = LocalDate.now().getYear();
        }
        if (!hasDay)
        {
            dayOfMonth = 1;
        }
        try
        {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(year, monthOfYear, dayOfMonth, hour, min, sec, 0, zoneId);
            if (!hasDay)
            {
                // If no day, take last day of the month
                zonedDateTime = zonedDateTime.plusMonths(1).minusDays(1);
            }
            return Date.from(zonedDateTime.toInstant());
        } catch (DateTimeException ex)
        {
            return null;
        }
    }

    public boolean isAValidDate()
    {
        parse();
        return parsedSuccessfully && (hasDay || hasYear) && hasMonth;
    }

    public boolean isParsedSuccessfully()
    {
        parse();
        return parsedSuccessfully;
    }

    public String getFormatString()
    {
        parse();
        return formatString;
    }

    public boolean hasDay()
    {
        parse();
        return hasDay;
    }

    public boolean hasMonth()
    {
        parse();
        return hasMonth;
    }

    public boolean hasHour()
    {
        parse();
        return hasHour;
    }

    public boolean hasMinute()
    {
        parse();
        return hasMinute;
    }

    public boolean hasSecond()
    {
        parse();
        return hasSecond;
    }

    public boolean hasYear()
    {
        parse();
        return hasYear;
    }

    public boolean hasAMPM()
    {
        parse();
        return hasAMPM;
    }

    public boolean isHasTimezone()
    {
        parse();
        return timeZone != null;
    }

    public boolean sdfCanReproduceExactly()
    {
        return !longTimeZone;
    }
}
