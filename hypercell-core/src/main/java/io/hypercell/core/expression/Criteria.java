package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import io.hypercell.core.dateparser.DateAnalyzer;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class Criteria
{
    public MemCell criteriaValue = null;
    public String operator = null;
    public Pattern pattern = null;

    public Criteria(MemCell criteriaMc, AtomicInteger nonDateCount)
    {
        if (criteriaMc.getNumberValue() != null)
        {
            criteriaValue = criteriaMc;
        } else
        {
            String[] criteriaParts = extractOperator(criteriaMc.getStringValue());
            if (criteriaParts[1] != null)
            {
                DateAnalyzer da = new DateAnalyzer(criteriaParts[1]);
                if (nonDateCount.get() < 10 && da.isAValidDate())
                {
                    Date d = da.getDate();
                    double val = DateTimeFunction.getSheetDateNumber(d.getTime() / 1000);
                    criteriaValue = new MemCell(Math.floor(val));
                } else
                {
                    nonDateCount.incrementAndGet();
                    if (criteriaParts[1].equalsIgnoreCase("true"))
                    {
                        criteriaValue = new MemCell(true);
                    } else if (criteriaParts[1].equalsIgnoreCase("false"))
                    {
                        criteriaValue = new MemCell(false);
                    } else
                    {
                        DecimalFormat df = new DecimalFormat();
                        try
                        {
                            // Number criteria
                            Number n = df.parse(criteriaParts[1]);
                            if (criteriaParts[1].equals(df.format(n.doubleValue())))
                            {
                                criteriaValue = new MemCell(n.doubleValue());
                            }
                        } catch (ParseException ignored)
                        {
                        }
                        if (criteriaValue == null)
                        {
                            // String criteria
                            criteriaValue = new MemCell(criteriaParts[1]);
                            if (criteriaParts[1].contains("*") || criteriaParts[1].contains("?"))
                            {
                                pattern = Pattern.compile(criteriaParts[1].replace("*", ".*").replace("?", ".?"));
                            }
                        }
                    }
                }
            }
            operator = criteriaParts[0];
        }
    }

    public boolean indexAble()
    {
        return (operator == null || operator.equals("=")) && pattern == null;
    }

    public void evaluateCriteriaAddresses(MemSheet ws, List<CellAddress> criteriaAddresses,
                                          boolean[] results, boolean[] touched)
    {
        int index = 0;
        IntStream.range(0, criteriaAddresses.size()).parallel().forEach(i ->
        {
            CellAddress ca = criteriaAddresses.get(i);
            if (criteriaValue == null)
            {
                results[i] = false;
                touched[i] = true;
                return;
            }
            MemCell mc = ws.getCellAt(ca);
            boolean result = false;
            if (mc != null)
            {
                mc.calculate();
                if (operator == null || operator.charAt(0) == '=' || operator.equals("<>"))
                {
                    boolean equals = false;
                    if (pattern != null)
                    {
                        String value = mc.getStringValue();
                        if (value != null)
                        {
                            Matcher m = pattern.matcher(mc.getStringValue());
                            equals = m.matches();
                        }
                    } else
                    {
                        equals = mc.equals(criteriaValue, true);
                    }
                    if (operator == null || operator.charAt(0) == '=')
                    {
                        result = equals;
                    } else if (operator.equals("<>"))
                    {
                        result = !equals;
                    }
                } else
                {
                    int compare = mc.compareTo(criteriaValue, true);
                    result = switch (operator)
                    {
                        case ">" -> compare > 0;
                        case ">=" -> compare >= 0;
                        case "<" -> compare < 0;
                        case "<=" -> compare <= 0;
                        default -> result;
                    };
                }
            }

            if (touched[i])
            {
                results[i] = results[i] && result;
            } else
            {
                results[i] = result;
                touched[i] = true;
            }
        });
    }

    private String[] extractOperator(String criteria)
    {
        if (criteria == null)
        {
            return new String[]{null, null};
        }
        if (criteria.startsWith("<>"))
        {
            return new String[]{"<>", criteria.substring(2)};
        } else if (criteria.startsWith("="))
        {
            return new String[]{"=", criteria.substring(1)};
        } else if (criteria.startsWith(">="))
        {
            return new String[]{">=", criteria.substring(2)};
        } else if (criteria.startsWith("<="))
        {
            return new String[]{"<=", criteria.substring(2)};
        } else if (criteria.startsWith(">"))
        {
            return new String[]{">", criteria.substring(1)};
        } else if (criteria.startsWith("<"))
        {
            return new String[]{"<", criteria.substring(1)};
        }
        return new String[]{null, criteria};
    }

}
