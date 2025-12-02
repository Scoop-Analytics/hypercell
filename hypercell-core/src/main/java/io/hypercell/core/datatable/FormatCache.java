/**
 * 
 */
package io.hypercell.core.datatable;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author bradpeters
 *
 */
public class FormatCache
{
	//
	public static SimpleDateFormat[] DATE_FORMATS = {
			// Datetime Formats
			new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a z"), new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a"),
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
			new SimpleDateFormat("yyyy-MM-dd HH:mm z"), new SimpleDateFormat("yyyy-MM-dd HH:mm"),
			// Date Formats
			new SimpleDateFormat("M/d/yyyy"), new SimpleDateFormat("MM/dd/yyyy"), new SimpleDateFormat("M/d/yy"),
			new SimpleDateFormat("MM/dd/yyyy"), new SimpleDateFormat("yyyy-MM-dd"),
			new SimpleDateFormat("MMM dd, yyyy"), new SimpleDateFormat("MMMM dd, yyyy") };
	public SimpleDateFormat[] dateFormats;
	public DecimalFormat integerFormatter = null;
	public DecimalFormat decimalFormatter = null;
	public SimpleDateFormat dateFormatter = null;
	public Locale locale = null;

	public FormatCache()
	{
		// Create a new instance of the formats for parallel processing in each thread
		dateFormats = new SimpleDateFormat[DATE_FORMATS.length];
		for (int i = 0; i < dateFormats.length; i++)
		{
			dateFormats[i] = (SimpleDateFormat) DATE_FORMATS[i].clone();
		}
	}
}
