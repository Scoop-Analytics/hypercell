/**
 * 
 */
package io.hypercell.core.datagrid;

/**
 * @author bradpeters
 *
 */
public abstract class DataCell
{
	protected boolean hasError = false;

	public abstract DataCell duplicate();

	@Override
	public abstract String toString();

	public abstract String getStringValue();

	public abstract Object getValue();
	
	public abstract boolean isBlank();

	public boolean isHasError()
	{
		return hasError;
	}

	public void setHasError(boolean hasError)
	{
		this.hasError = hasError;
	}
}
