package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.Token;

import io.hypercell.api.Expression;
import io.hypercell.api.Function;
import io.hypercell.api.FunctionRegistry;
import io.hypercell.formula.HyperCellExpressionLexer;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionParser.*;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.FormulaError;

import io.hypercell.core.expression.CompileContext;
import io.hypercell.core.expression.*;
import io.hypercell.core.expression.ScoopExpression;
import io.hypercell.core.expression.ScoopExpressionWrapper;

import java.util.ArrayList;
import java.util.List;

public class StandardCompilerDelegate implements CompilerDelegate {
    private static final Logger logger = LoggerFactory.getLogger(StandardCompilerDelegate.class);

    @Override
    public Expression compile(ParseTree tree, io.hypercell.core.expression.CompileContext cc) {
        // Note: We receive io.hypercell.core.expression.CompileContext, but legacy functions expect io.hypercell.core.expression.CompileContext.
        // We need to bridge or ensure compatibility.
        // Currently, io.hypercell.core.expression.CompileContext is a separate class.
        // We should probably construct a Scoop CompileContext wrapping the HyperCell one or just create it fresh if needed?
        // The existing logic in Compile.java was creating new io.hypercell.core.expression.CompileContext(cc.getSheet(), registry) for each function call.
        // We will maintain that pattern.
        
        Expression exp = null;
        FunctionRegistry registry = cc.getRegistry();
        
        // Helper for legacy context creation
        io.hypercell.core.expression.CompileContext scc = new io.hypercell.core.expression.CompileContext(cc.getSheet(), registry);

        if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.StartContext) {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.PARENContext) {
            Compile c = new Compile(tree.getChild(1), cc);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.UMINUSContext) {
            exp = new UnaryOperator(tree.getChild(0), tree.getChild(1), cc);
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.ADDOPContext || tree instanceof io.hypercell.formula.HyperCellExpressionParser.MULOPContext || tree instanceof io.hypercell.formula.HyperCellExpressionParser.COMPOPPContext
                || tree instanceof io.hypercell.formula.HyperCellExpressionParser.POWERContext || tree instanceof io.hypercell.formula.HyperCellExpressionParser.CONCATOPPContext) {
            exp = new BinaryOperator(tree.getChild(0), tree.getChild(1), tree.getChild(2), cc);
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.NUMBERContext) {
            ParseTree child = tree.getChild(0);
            if (child instanceof io.hypercell.formula.HyperCellExpressionParser.INTEGERVALContext || child instanceof io.hypercell.formula.HyperCellExpressionParser.DECIMALVALContext) {
                try {
                    exp = new SheetNumber(tree.getChild(0));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.REFContext) {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.CELLContext) {
            Identifier id = new Identifier(tree.getChild(0), cc.getSheet());
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.OFFSETContext) {
            Identifier id = new Identifier(tree.getChild(0).getChild(1), cc.getSheet());
            int offset = Integer.parseInt(tree.getChild(0).getChild(3).getText());
            id.setOffset(offset);
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.ItemContext) {
            Identifier id = new Identifier(tree, cc.getSheet());
            cc.addIdentifier(id);
            exp = id;
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.RangeContext) {
            exp = new Range(cc.getSheet(), tree);
            cc.addRange((Range) exp);
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.RangeorreferenceContext) {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.FilteredrangeContext) {
            Compile c = new Compile(tree.getChild(0), cc);
            exp = c.getExpression();
            if (tree.getChildCount() == 3) {
                c = new Compile(tree.getChild(2), cc);
                ((Range) exp).setFilter(new ScoopExpressionWrapper(c.getExpression()));
            }
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.STRINGContext) {
            exp = new SheetString(tree.getChild(0));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.MATHContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.MathFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.LOGICALContext) {
            ParseTree child = tree.getChild(0);
            Token t = ((TerminalNodeImpl) child.getChild(0)).symbol;
            switch (t.getType()) {
                case HyperCellExpressionLexer.VLOOKUPTOKEN:
                case HyperCellExpressionLexer.HLOOKUPTOKEN:
                    exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.LookupFunction(child, scc));
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
                    exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.LogicalFunction(child, scc));
                    break;
                default:
                    logger.error("Unrecognized token: {}", tree.getText());
            }
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.LOOKUPContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.LookupFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.FINANCIALContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.FinancialFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.DATETIMEContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.DateTimeFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.STATISTICALContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.StatisticalFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.INFORMATIONALContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.InformationFunction(child, scc));
            if (scc.isInformationalOnly()) {
                cc.setInformationalOnly(true);
            }
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.TEXTUALContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.TextualFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.FILTERContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.FilterFunction(child, scc));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.BOOLEANContext) {
            String text = tree.getChild(0).getText().toLowerCase();
            if (text.equals("true")) {
                exp = new SheetNumber(1);
            } else {
                exp = new SheetNumber(0);
            }
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.CONSTANTContext) {
            exp = new SheetConstant(tree);
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.TablearrayContext) {
            exp = new Range(cc.getSheet(), tree);
            cc.addRange((Range) exp);
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.SCOOPContext) {
            ParseTree child = tree.getChild(0);
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.ErrorFunction(io.hypercell.core.grid.FormulaError.NAME));
        } else if (tree instanceof io.hypercell.formula.HyperCellExpressionParser.SHEETSContext) {
            ParseTree child = tree.getChild(0);
            if (child instanceof HyperCellExpressionParser.COMSUMTOKENContext) {
                // Just return zero
                if (child.getText().startsWith("com.sun.star.sheet.addin.Analysis.getEomonth(")) {
                    MemCell newCell = new MemCell();
                    newCell.setFormula("EOMONTH(" + child.getText().substring(45));
                    newCell.compileFormula(cc.getSheet());
                    exp = new ScoopExpressionWrapper(newCell.getCompile().getExpression());
                } else {
                    exp = new SheetNumber(0);
                }
            } else if (child instanceof HyperCellExpressionParser.XLUDFContext) {
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
                // The original code logic here was recursive compilation of the inner string.
                // Just keeping it structurally identical to what was in Compile.java
                try {
                    String innerFormula = ((ScoopExpression)exp).calculateCellValue().getStringValue();
                    c = new Compile(innerFormula, cc);
                    exp = c.getExpression();
                } catch (Exception ignored) {
                    exp = new ErrorFunction(FormulaError.NA);
                }
            }
        } else if (tree instanceof HyperCellExpressionParser.BooleanarrayContext) {
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.BooleanArray(scc, tree));
        } else if (tree instanceof HyperCellExpressionParser.ExpressionarrayContext) {
            exp = new ScoopExpressionWrapper(new io.hypercell.core.expression.ExpressionAray(scc, tree));
        } else if (tree instanceof HyperCellExpressionParser.StringContext) {
            exp = new SheetString(tree);
        } else if (tree instanceof HyperCellExpressionParser.GENERIC_FUNCTIONContext) {
            return handleFunction(tree, cc, registry);
        } else {
            logger.error("Unrecognized token: {}", tree.getText());
        }
        return exp;
    }

    private Expression handleFunction(ParseTree funcTree, io.hypercell.core.expression.CompileContext cc, FunctionRegistry registry) {
        String funcName = "";
        if (funcTree.getChildCount() > 0) {
             ParseTree first = funcTree.getChild(0);
             if (first instanceof TerminalNodeImpl) {
                 funcName = first.getText().toUpperCase();
             }
        }
        
        List<Expression> args = new ArrayList<>();
        for (int i = 0; i < funcTree.getChildCount(); i++) {
            ParseTree child = funcTree.getChild(i);
            if (child instanceof TerminalNodeImpl) continue; 
            Compile c = new Compile(child, cc, registry);
            if (c.getExpression() != null) {
                args.add(c.getExpression());
            }
        }
        
        if (registry != null && registry.getFunction(funcName) != null) {
            Function func = registry.getFunction(funcName);
            return new FunctionCallExpression(func, args, cc.getSheet());
        }
        return null;
    }
}