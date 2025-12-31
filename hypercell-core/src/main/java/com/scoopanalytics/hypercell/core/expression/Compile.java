package com.scoopanalytics.hypercell.core.expression;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scoopanalytics.hypercell.api.HyperCellException;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionLexer;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.ADDOPContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.BOOLEANContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.CELLContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.COMPOPPContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.CONCATOPPContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.DATETIMEContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.DECIMALVALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.FINANCIALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.FilteredrangeContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.INFORMATIONALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.INTEGERVALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.ItemContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.LOGICALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.LOOKUPContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.MATHContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.MULOPContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.NUMBERContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.OFFSETContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.PARENContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.POWERContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.REFContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.RangeContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.RangeorreferenceContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.STATISTICALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.STRINGContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.StartContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.TEXTUALContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.TablearrayContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.UMINUSContext;
import com.scoopanalytics.hypercell.formula.HyperCellExpressionParser.FILTERContext;
import com.scoopanalytics.hypercell.core.grid.FormulaError;
import com.scoopanalytics.hypercell.core.grid.MemCell;
import com.scoopanalytics.hypercell.core.grid.MemSheet;

/**
 * Compiles Excel formulas into executable expression trees.
 */
public class Compile
{
    static Logger logger = LoggerFactory.getLogger(Compile.class);
    private final ParseTree tree;
    private HyperCellExpression exp;
    private final CompileContext cc;

    /**
     * Compile a formula string for a given sheet.
     */
    public Compile(String formula, MemSheet sheet, boolean throwErrors)
    {
        CharStream input = CharStreams.fromString(formula);
        HyperCellExpressionLexer lex = new HyperCellExpressionLexer(input);
        if (throwErrors)
        {
            lex.removeErrorListeners();
            lex.addErrorListener(ThrowingErrorListener.INSTANCE);
        }
        CommonTokenStream tokens = new CommonTokenStream(lex);
        HyperCellExpressionParser parser = new HyperCellExpressionParser(tokens);
        if (throwErrors)
        {
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        }
        tree = parser.start();
        cc = new CompileContext(sheet);
        compile();
    }

    public Compile(ParseTree tree, CompileContext cc)
    {
        this.tree = tree;
        this.cc = cc;
        compile();
    }

    public Compile(String formula, CompileContext cc)
    {
        CharStream input = CharStreams.fromString(formula);
        HyperCellExpressionLexer lex = new HyperCellExpressionLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        HyperCellExpressionParser parser = new HyperCellExpressionParser(tokens);
        tree = parser.start();
        this.cc = cc;
        compile();
    }

    public Compile(ParseTree tree, MemSheet sheet)
    {
        this.tree = tree;
        this.cc = new CompileContext(sheet);
        compile();
    }

    private void compile()
    {
        if (tree instanceof StartContext)
        {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof PARENContext)
        {
            Compile c = new Compile(tree.getChild(1), cc);
            exp = c.getExpression();
        } else if (tree instanceof UMINUSContext)
        {
            exp = new UnaryOperator(tree.getChild(0), tree.getChild(1), cc);
        } else if (tree instanceof ADDOPContext || tree instanceof MULOPContext || tree instanceof COMPOPPContext
                || tree instanceof POWERContext || tree instanceof CONCATOPPContext)
        {
            exp = new BinaryOperator(tree.getChild(0), tree.getChild(1), tree.getChild(2), cc);
        } else if (tree instanceof NUMBERContext)
        {
            ParseTree child = tree.getChild(0);
            if (child instanceof INTEGERVALContext || child instanceof DECIMALVALContext)
            {
                try
                {
                    exp = new SheetNumber(tree.getChild(0));
                } catch (HyperCellException e)
                {
                    logger.error(e.getMessage());
                }
            }
        } else if (tree instanceof REFContext)
        {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof CELLContext)
        {
            Identifier id = new Identifier(tree.getChild(0), cc.getSheet());
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof OFFSETContext)
        {
            Identifier id = new Identifier(tree.getChild(0).getChild(1), cc.getSheet());
            int offset = Integer.parseInt(tree.getChild(0).getChild(3).getText());
            id.setOffset(offset);
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof ItemContext)
        {
            Identifier id = new Identifier(tree, cc.getSheet());
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof RangeContext)
        {
            exp = new Range(cc.getSheet(), tree);
            cc.addRange((Range) exp);
        } else if (tree instanceof RangeorreferenceContext)
        {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof FilteredrangeContext)
        {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
            if (tree.getChildCount() == 3)
            {
                c = new Compile(tree.getChild(2), cc);
                ((Range) exp).setFilter(c.getExpression());
            }
        } else if (tree instanceof STRINGContext)
        {
            exp = new SheetString(tree.getChild(0));
        } else if (tree instanceof MATHContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new MathFunction(child, cc);
        } else if (tree instanceof LOGICALContext)
        {
            ParseTree child = tree.getChild(0);
            Token t = ((TerminalNodeImpl) child.getChild(0)).symbol;
            switch (t.getType())
            {
                case HyperCellExpressionLexer.VLOOKUPTOKEN:
                case HyperCellExpressionLexer.HLOOKUPTOKEN:
                    exp = new LookupFunction(child, cc);
                    break;
                case HyperCellExpressionParser.IFTOKEN:
                case HyperCellExpressionParser.IFSTOKEN:
                case HyperCellExpressionParser.IFERRORTOKEN:
                case HyperCellExpressionParser.IFNATOKEN:
                case HyperCellExpressionParser.TRUETOKEN:
                case HyperCellExpressionParser.FALSETOKEN:
                case HyperCellExpressionLexer.ANDTOKEN:
                case HyperCellExpressionLexer.ORTOKEN:
                case HyperCellExpressionLexer.XORTOKEN:
                case HyperCellExpressionLexer.NOTTOKEN:
                case HyperCellExpressionLexer.EQTOKEN:
                    exp = new LogicalFunction(child, cc);
                    break;
                default:
                    logger.error("Unrecognized token: {}", tree.getText());
            }
        } else if (tree instanceof LOOKUPContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new LookupFunction(child, cc);
        } else if (tree instanceof FINANCIALContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new FinancialFunction(child, cc);
        } else if (tree instanceof DATETIMEContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new DateTimeFunction(child, cc);
        } else if (tree instanceof STATISTICALContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new StatisticalFunction(child, cc);
        } else if (tree instanceof INFORMATIONALContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new InformationFunction(child, cc);
        } else if (tree instanceof TEXTUALContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new TextualFunction(child, cc);
        } else if (tree instanceof FILTERContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new FilterFunction(child, cc);
        } else if (tree instanceof BOOLEANContext)
        {
            String text = tree.getChild(0).getText().toLowerCase();
            if (text.equals("true"))
            {
                exp = new SheetNumber(1);
            } else
            {
                exp = new SheetNumber(0);
            }
        } else if (tree instanceof HyperCellExpressionParser.CONSTANTContext)
        {
            exp = new SheetConstant(tree);
        } else if (tree instanceof TablearrayContext)
        {
            exp = new Range(cc.getSheet(), tree);
            cc.addRange((Range) exp);
        } else if (tree instanceof HyperCellExpressionParser.SHEETSContext)
        {
            ParseTree child = tree.getChild(0);
            if (child instanceof HyperCellExpressionParser.COMSUMTOKENContext)
            {
                // Handle LibreOffice Calc export format
                if (child.getText().startsWith("com.sun.star.sheet.addin.Analysis.getEomonth("))
                {
                    MemCell newCell = new MemCell();
                    newCell.setFormula("EOMONTH(" + child.getText().substring(45));
                    newCell.compileFormula(cc.getSheet());
                    exp = newCell.getCompile().getExpression();
                } else
                {
                    exp = new SheetNumber(0);
                }
            } else if (child instanceof HyperCellExpressionParser.XLUDFContext)
            {
                /*
                 * Ignore the __xludf.DUMMYFUNCTION(" expression ") construct generated by Sheets
                 * when exporting to Excel functions that don't export.
                 */
                child = child.getChild(2);
                Compile c = new Compile(child, cc);
                exp = new ErrorFunction(FormulaError.NA);
                exp = c.getExpression();
                String formula = exp.calculateCellValue().getStringValue();
                try
                {
                    c = new Compile(formula, cc);
                    exp = c.getExpression();
                } catch (Exception ignored)
                {
                    exp = new ErrorFunction(FormulaError.NA);
                }
            }
        } else if (tree instanceof HyperCellExpressionParser.GENERIC_FUNCTIONContext)
        {
            // Handle user-defined functions via the function registry
            logger.warn("Generic function not yet supported: {}", tree.getText());
            exp = new ErrorFunction(FormulaError.NAME);
        } else if (tree instanceof HyperCellExpressionParser.BooleanarrayContext)
        {
            exp = new BooleanArray(cc, tree);
        } else if (tree instanceof HyperCellExpressionParser.ExpressionarrayContext)
        {
            exp = new ExpressionAray(cc, tree);
        } else if (tree instanceof HyperCellExpressionParser.StringContext)
        {
            exp = new SheetString(tree);
        } else
        {
            logger.error("Unrecognized token: {}", tree.getText());
        }
    }

    public String getExcelFormula()
    {
        return exp.getExcelFormula();
    }

    public String getMetricFormula()
    {
        return exp.getMetricFormula();
    }

    public HyperCellExpression getExpression()
    {
        return exp;
    }

    public List<Identifier> getIdentifiers()
    {
        return cc.getIdentifierList();
    }

    public List<Range> getRanges()
    {
        return cc.getRangeList();
    }

    public boolean isInformationalOnly()
    {
        return cc.isInformationalOnly();
    }

    public boolean containsAggregation()
    {
        return cc.isContainsAggregation();
    }
}
