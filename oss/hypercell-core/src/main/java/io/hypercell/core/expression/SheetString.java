/**
 * 
 */
package io.hypercell.core.expression;

import org.antlr.v4.runtime.tree.ParseTree;

import io.hypercell.core.grid.MemCell;

/**
 * @author bradpeters
 *
 */
public class SheetString extends HyperCellExpression
{
	private String value;

	public SheetString(ParseTree tree)
	{
		value = tree.getChild(0).getText();
		if (value.startsWith("\"") && value.endsWith("\""))
		{
			value = value.substring(1, value.length() - 1);
		}
		value = value.replace("\"\"", "\"");
	}

	@Override
	public String getMetricFormula()
	{
		return "\"" + value + "\"";
	}

	@Override
	public String getExcelFormula()
	{
		return "\"" + value + "\"";
	}

	@Override
	public MemCell calculateCellValue()
	{
		return new MemCell(value);
	}
}
