/**
 * 
 */
package io.hypercell.core.datagrid;

import org.jsoup.nodes.Element;

import java.util.Objects;

/**
 * @author bradpeters
 *
 */
public class HTMLCell extends DataCell
{
	private transient Element element;
	private String cellValue;

	public HTMLCell()
	{

	}
	public HTMLCell(Element element)
	{
		this.element = element;
		this.cellValue = element.text().trim();
	}

	public Element getElement()
	{
		return element;
	}

	public void setElement(Element element)
	{
		this.element = element;
	}

	/**
	 * Whether this column spans into later rows
	 * 
	 */
	public boolean isColSpan()
	{
		return element.hasAttr("colspan");
	}

	public String getCellValue()
	{
		return cellValue;
	}

	public void setCellValue(String cellValue)
	{
		this.cellValue = cellValue;
        this.element.text(Objects.requireNonNullElse(cellValue, ""));
	}

	@Override
	public DataCell duplicate()
	{
		HTMLCell newcell = new HTMLCell(element);
		newcell.cellValue = cellValue;
		return newcell;
	}

	@Override
	public String toString()
	{
		return cellValue;
	}

	@Override
	public String getStringValue()
	{
		return cellValue;
	}

	@Override
	public Object getValue()
	{
		return cellValue;
	}

	@Override
	public boolean isBlank()
	{
		return cellValue == null || cellValue.isEmpty();
	}
}
