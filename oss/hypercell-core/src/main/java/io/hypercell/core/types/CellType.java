/**
 * Cell type enumeration for HyperCell.
 */
package io.hypercell.core.types;

/**
 * Defines the types of values that can be stored in a cell.
 */
public enum CellType
{
    Empty, String, Currency, Decimal, Integer, DateTime, Boolean;

    /**
     * Check if this type represents a numeric value.
     *
     * @return true if the type is a number (Currency, Decimal, or Integer)
     */
    public boolean isNumber()
    {
        return this == CellType.Currency || this == CellType.Decimal || this == CellType.Integer;
    }

    /**
     * Check if this type can be used as an attribute.
     *
     * @return true if the type can be an attribute
     */
    public boolean isPotentialAttribute()
    {
        return this == CellType.String || this == CellType.Integer || this == CellType.DateTime
                || this == CellType.Boolean;
    }
}
