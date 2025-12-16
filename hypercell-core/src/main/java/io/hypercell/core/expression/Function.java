/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hypercell.core.util.FormattingUtils;
import io.hypercell.formula.HyperCellExpressionParser.SumproductargumentsContext;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemCellContext;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author bradpeters
 */
public class Function extends ScoopExpression
{
    private static final Logger logger = LoggerFactory.getLogger(Function.class);
    protected int type;
    protected String tokenStr;
    protected List<ScoopExpression> expressions = new ArrayList<>();
    protected CompileContext cc;

    public Function(ParseTree tree, CompileContext cc)
    {
        Token t = ((TerminalNodeImpl) tree.getChild(0)).symbol;
        this.type = t.getType();
        this.tokenStr = t.getText();
        this.cc = cc;
        if (tree.getChildCount() > 3)
        {
            for (int i = 2; i < tree.getChildCount(); i += 2)
            {
                ParseTree child = tree.getChild(i);
                if (child instanceof SumproductargumentsContext)
                {
                    for (int j = 0; j < child.getChildCount(); j += 1)
                    {
                        if (child.getChild(j) instanceof HyperCellExpressionParser.RangeorreferenceContext ||
                                child.getChild(j) instanceof HyperCellExpressionParser.FilteredrangeContext)
                        {
                            Compile c = new Compile(child.getChild(j), cc);
                            ScoopExpression exp = c.getExpression();
                            expressions.add(exp);
                        }
                    }
                } else
                {
                    Compile c = new Compile(child, cc);
                    ScoopExpression exp = c.getExpression();
                    expressions.add(exp);
                }
            }
        }
    }

    /**
     * Returns the string equivalent of a cell - even if it's a number
     *
     * @param inConcant - whether this is being returned within a concatenation (don't convert formats of dates)
     */
    public static String getStringValue(MemCell cell, boolean inConcant)
    {
        if (cell == null)
            return null;
        if (cell.getStringValue() == null)
        {
            if (cell.getNumberValue() != null)
            {
                MemCellContext context = cell.getCellContext();
                if (context != null)
                {
                    String formatString = context != null && context.getStyle() != null ? context.getStyle()
                                                                                                 .getFormatString() : null;
                    String numberFormat = "####";
                    if (FormattingUtils.isDateFormat(formatString))
                    {
                        /*
                         * in Excel, Google Sheets and Libre, when a date is turned into a string, if it's in a formula
                         * (like using the & to concat) then it is a number
                         */
                        if (!inConcant)
                        {
                            formatString = DateTimeFunction.getJavaDateFormatFromSheetFormat(formatString);
                            ZonedDateTime zdt = DateTimeFunction
                                    .getDateFromSheetNumber(cell.getNumberValue().doubleValue());
                            SimpleDateFormat sdf = new SimpleDateFormat(formatString);
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return sdf.format(Date.from(zdt.toInstant()));
                        }
                    } else if (formatString != null && !formatString.equals("General"))
                    {
                        numberFormat = formatString;
                    }
                    NumberFormat nf = null;
                    try
                    {
                        nf = new DecimalFormat(numberFormat);
                    } catch (IllegalArgumentException e)
                    {
                        return cell.getNumberValue().toString();
                    }
                    return nf.format(cell.getNumberValue());
                } else
                {
                    return cell.getNumberValue().toString();
                }
            } else
            {
                return null;
            }
        } else
        {
            return cell.getStringValue();
        }

    }

    @Override
    public MemCell calculateCellValue()
    {
        return null;
    }

    @Override
    public String getMetricFormula()
    {
        StringBuilder sb = new StringBuilder(tokenStr + "(");
        boolean first = true;
        for (ScoopExpression expression : expressions)
        {
            if (first)
            {
                first = false;
            } else
            {
                sb.append(",");
            }
            if (expression != null)
            {
                sb.append(expression.getMetricFormula());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getExcelFormula()
    {
        StringBuilder sb = new StringBuilder(tokenStr + "(");
        boolean first = true;
        for (ScoopExpression expression : expressions)
        {
            if (expression == null)
                continue;
            if (first)
            {
                first = false;
            } else
            {
                sb.append(",");
            }
            sb.append(expression.getExcelFormula());
        }
        sb.append(")");
        return sb.toString();
    }

    public List<ScoopExpression> getExpressions()
    {
        return expressions;
    }
}
