/**
 * 
 */
package io.hypercell.core.datagrid;

/**
 * @author bradpeters
 *
 */
public class EmptyCell extends DataCell
{

	@Override
	public DataCell duplicate()
	{
		return new EmptyCell();
	}

	@Override
	public String toString()
	{
		return "[Empty]";
	}

	@Override
	public String getStringValue()
	{
		return null;
	}

	@Override
	public Object getValue()
	{
		return null;
	}

	@Override
	public boolean isBlank()
	{
		return true;
	}

}
