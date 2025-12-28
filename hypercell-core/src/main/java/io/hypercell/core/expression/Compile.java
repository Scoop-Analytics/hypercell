package io.hypercell.core.expression;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hypercell.api.HyperCellException;
import scoop.ScoopContext;
import scoop.expression.ScoopExpressionParser.ADDOPContext;
import scoop.expression.ScoopExpressionParser.BOOLEANContext;
import scoop.expression.ScoopExpressionParser.CELLContext;
import scoop.expression.ScoopExpressionParser.COMPOPPContext;
import scoop.expression.ScoopExpressionParser.CONCATOPPContext;
import scoop.expression.ScoopExpressionParser.DATETIMEContext;
import scoop.expression.ScoopExpressionParser.DECIMALVALContext;
import scoop.expression.ScoopExpressionParser.FINANCIALContext;
import scoop.expression.ScoopExpressionParser.FilteredrangeContext;
import scoop.expression.ScoopExpressionParser.INFORMATIONALContext;
import scoop.expression.ScoopExpressionParser.INTEGERVALContext;
import scoop.expression.ScoopExpressionParser.ItemContext;
import scoop.expression.ScoopExpressionParser.LOGICALContext;
import scoop.expression.ScoopExpressionParser.LOOKUPContext;
import scoop.expression.ScoopExpressionParser.MATHContext;
import scoop.expression.ScoopExpressionParser.MULOPContext;
import scoop.expression.ScoopExpressionParser.NUMBERContext;
import scoop.expression.ScoopExpressionParser.OFFSETContext;
import scoop.expression.ScoopExpressionParser.PARENContext;
import scoop.expression.ScoopExpressionParser.POWERContext;
import scoop.expression.ScoopExpressionParser.REFContext;
import scoop.expression.ScoopExpressionParser.RangeContext;
import scoop.expression.ScoopExpressionParser.RangeorreferenceContext;
import scoop.expression.ScoopExpressionParser.STATISTICALContext;
import scoop.expression.ScoopExpressionParser.STRINGContext;
import scoop.expression.ScoopExpressionParser.StartContext;
import scoop.expression.ScoopExpressionParser.TEXTUALContext;
import scoop.expression.ScoopExpressionParser.TablearrayContext;
import scoop.expression.ScoopExpressionParser.UMINUSContext;
import scoop.expression.ScoopExpressionParser.FILTERContext;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.ThrowingErrorListener;

public class Compile
{
    static Logger logger = LoggerFactory.getLogger(Compile.class);
    private final ParseTree tree;
    private ScoopExpression exp;
    private final CompileContext cc;

    public Compile(ScoopContext sc, String formula, MemSheet sheet, boolean throwErrors)
    {
        CharStream input = CharStreams.fromString(formula);
        ScoopExpressionLexer lex = new ScoopExpressionLexer(input);
        if (throwErrors)
        {
            lex.removeErrorListeners();
            lex.addErrorListener(ThrowingErrorListener.INSTANCE);
        }
        String errorString = null;
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ScoopExpressionParser scoopparser = new ScoopExpressionParser(tokens);
        if (throwErrors)
        {
            scoopparser.removeErrorListeners();
            scoopparser.addErrorListener(ThrowingErrorListener.INSTANCE);
        }
        tree = scoopparser.start();
        cc = new CompileContext(sc, sheet);
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
        ScoopExpressionLexer lex = new ScoopExpressionLexer(input);
        String errorString = null;
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ScoopExpressionParser scoopparser = new ScoopExpressionParser(tokens);
        tree = scoopparser.start();
        this.cc = cc;
        compile();
    }

    public Compile(ScoopContext sc, ParseTree tree, MemSheet sheet)
    {
        this.tree = tree;
        this.cc = new CompileContext(sc, sheet);
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
                case ScoopExpressionLexer.VLOOKUPTOKEN:
                case ScoopExpressionLexer.HLOOKUPTOKEN:
                    exp = new LookupFunction(child, cc);
                    break;
                case ScoopExpressionParser.IFTOKEN:
                case ScoopExpressionParser.IFSTOKEN:
                case ScoopExpressionParser.IFERRORTOKEN:
                case ScoopExpressionParser.IFNATOKEN:
                case ScoopExpressionParser.TRUETOKEN:
                case ScoopExpressionParser.FALSETOKEN:
                case ScoopExpressionLexer.ANDTOKEN:
                case ScoopExpressionLexer.ORTOKEN:
                case ScoopExpressionLexer.XORTOKEN:
                case ScoopExpressionLexer.NOTTOKEN:
                case ScoopExpressionLexer.EQTOKEN:
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
        } else if (tree instanceof ScoopExpressionParser.CONSTANTContext)
        {
            exp = new SheetConstant(tree);
        } else if (tree instanceof TablearrayContext)
        {
            exp = new Range(cc.getSheet(), tree);
            cc.addRange((Range) exp);
        } else if (tree instanceof ScoopExpressionParser.SCOOPContext)
        {
            ParseTree child = tree.getChild(0);
            exp = new ScoopFunction(child, cc);
        } else if (tree instanceof ScoopExpressionParser.SHEETSContext)
        {
            ParseTree child = tree.getChild(0);
            if (child instanceof ScoopExpressionParser.COMSUMTOKENContext)
            {
                // Just return zero
                if (child.getText().startsWith("com.sun.star.sheet.addin.Analysis.getEomonth("))
                {
                    MemCell newCell = new MemCell();
                    newCell.setFormula("EOMONTH(" + child.getText().substring(45));
                    newCell.compileFormula(cc.getSc(), cc.getSheet());
                    exp = newCell.getCompile().getExpression();
                } else
                {
                    exp = new SheetNumber(0);
                }
            } else if (child instanceof ScoopExpressionParser.XLUDFContext)
            {
                /*
                 * Ignore the __xludf.DUMMYFUNCTION(" expression ") construct generated by Sheets
                 * when exporting to Excel functions that don't export. Just take the computed value if you cannot
                 * compile what's inside
                 */
                child = child.getChild(2);
                String formula = child.getText();
                Compile c = new Compile(child, cc);
                exp = new ErrorFunction(FormulaError.NA);
                exp = c.getExpression();
                formula = exp.calculateCellValue().getStringValue();
                try
                {
                    c = new Compile(formula, cc);
                    exp = c.getExpression();
                } catch (Exception ignored)
                {
                    exp = new ErrorFunction(FormulaError.NA);
                }
            }
        } else if (tree instanceof ScoopExpressionParser.BooleanarrayContext)
        {
            exp = new BooleanArray(cc, tree);
        } else if (tree instanceof ScoopExpressionParser.ExpressionarrayContext)
        {
            exp = new ExpressionAray(cc, tree);
        } else if (tree instanceof ScoopExpressionParser.StringContext)
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

    public ScoopExpression getExpression()
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
