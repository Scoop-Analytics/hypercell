/**
 * 
 */
package io.hypercell.core.datagrid;

/**
 * @author bradpeters
 *
 */
public class BooleanCell extends DataCell
{
	private Boolean value;

	public BooleanCell(Boolean value)
	{
		this.value = value;
	}

	@Override
	public Boolean getValue()
	{
		return value;
	}

	public void setValue(Boolean value)
	{
		this.value = value;
	}

	@Override
	public DataCell duplicate()
	{
		BooleanCell newcell = new BooleanCell(value);
		return newcell;
	}

	@Override
	public String toString()
	{
		if (value == null)
			return "[null]";
		return value.toString();
	}

	@Override
	public String getStringValue()
	{
		return toString();
	}

	@Override
	public boolean isBlank()
	{
		return value == null;
	}

}
