/**
 * 
 */
package com.scoopanalytics.hypercell.core.grid;

/**
 * @author bradpeters
 *
 */
public enum FormulaError
{
	NULL(1, "#NULL!", org.apache.poi.ss.usermodel.FormulaError.NULL.getCode()),
	DIV0(2, "#DIV/0!", org.apache.poi.ss.usermodel.FormulaError.DIV0.getCode()),
	VALUE(3, "#VALUE!", org.apache.poi.ss.usermodel.FormulaError.VALUE.getCode()),
	REF(4, "#REF!", org.apache.poi.ss.usermodel.FormulaError.REF.getCode()),
	NAME(5, "#NAME?", org.apache.poi.ss.usermodel.FormulaError.NAME.getCode()),
	NUM(6, "#NUM!", org.apache.poi.ss.usermodel.FormulaError.NUM.getCode()),
	NA(7, "#N/A", org.apache.poi.ss.usermodel.FormulaError.NA.getCode()), SPILL(9, "#SPILL!", 0), CALC(14, "#CALC!", 0),
	NOT_IMPLEMENTED(15, "#N/I", 0);

	private final int code;
	private final int poiErrorCode;
	private final String display;

	FormulaError(int code, String display, int poiErrorCode)
	{
		this.code = code;
		this.display = display;
		this.poiErrorCode = poiErrorCode;
	}

	public int getCode()
	{
		return code;
	}

	public String getDisplay()
	{
		return display;
	}

	public int getPoiErrorCode()
	{
		return poiErrorCode;
	}
}
