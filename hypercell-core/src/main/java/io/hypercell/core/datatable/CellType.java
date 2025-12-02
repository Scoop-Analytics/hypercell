/**
 * 
 */
package io.hypercell.core.datatable;

/**
 * @author bradpeters
 *
 */
public enum CellType
{
	Empty, String, Currency, Decimal, Integer, DateTime, Boolean;

	public boolean isNumber()
	{
		return this == CellType.Currency || this == CellType.Decimal || this == CellType.Integer;
	}

	public boolean isPotentialAttribute()
	{
		return this == CellType.String || this == CellType.Integer || this == CellType.DateTime
				|| this == CellType.Boolean;
	}
}
