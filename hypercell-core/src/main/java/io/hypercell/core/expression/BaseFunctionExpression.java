package io.hypercell.core.expression;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hypercell.api.CellValue;
import io.hypercell.api.Expression;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.formula.HyperCellExpressionParser.SumproductargumentsContext;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemCellContext;
import io.hypercell.core.util.FormattingUtils; // for getStringValue
import io.hypercell.formula.HyperCellExpressionParser; // For context classes

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public abstract class BaseFunctionExpression extends AbstractExpression {
    private static final Logger logger = LoggerFactory.getLogger(BaseFunctionExpression.class);
    protected int type; // ANTLR token type
    protected String tokenStr; // ANTLR token string
    protected List<Expression> expressions = new ArrayList<>(); // Arguments
    protected CompileContext cc;
    protected FunctionRegistry registry; // Added registry

    public BaseFunctionExpression(ParseTree tree, CompileContext cc, FunctionRegistry registry) {
        this.cc = cc;
        this.registry = registry;
        
        Token t = ((TerminalNodeImpl) tree.getChild(0)).symbol;
        this.type = t.getType();
        this.tokenStr = t.getText();
        
        if (tree.getChildCount() > 1 && tree.getChild(1) instanceof TerminalNodeImpl && tree.getChild(1).getText().equals("(")) {
            for (int i = 2; i < tree.getChildCount(); i++) { 
                ParseTree child = tree.getChild(i);
                if (child instanceof TerminalNodeImpl && (child.getText().equals(")") || child.getText().equals(","))) {
                    continue; 
                }
                
                Compile c = new Compile(child, cc, registry);
                Expression exp = c.getExpression();
                if (exp != null) {
                    expressions.add(exp);
                }
            }
        }
    }

    public static String getStringValue(CellValue cell, boolean inConcant) {
        if (cell == null) return null;
        if (cell.getStringValue() == null) {
            if (cell.getNumberValue() != null) {
                return cell.getNumberValue().toString(); // Simplified
            } else {
                return null;
            }
        } else {
            return cell.getStringValue();
        }
    }

    @Override
    public abstract CellValue evaluate(); 

    @Override
    public String getMetricFormula() {
        StringBuilder sb = new StringBuilder(tokenStr + "(");
        boolean first = true;
        for (Expression expression : expressions) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            if (expression != null) {
                sb.append(expression.getMetricFormula());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getExcelFormula() {
        StringBuilder sb = new StringBuilder(tokenStr + "(");
        boolean first = true;
        for (Expression expression : expressions) {
            if (expression == null)
                continue;
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(expression.getExcelFormula());
        }
        sb.append(")");
        return sb.toString();
    }

    public List<Expression> getExpressions() {
        return expressions;
    }
}
