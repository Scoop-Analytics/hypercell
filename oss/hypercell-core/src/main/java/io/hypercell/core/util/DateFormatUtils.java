/**
 * Utility methods for date format detection and manipulation.
 */
package io.hypercell.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities for detecting and manipulating Excel date formats.
 */
public final class DateFormatUtils
{
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\\/|\\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\\/|\\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\\s][A|a][M|m][\\/][P|p][M|m])?)?");

    private static final Pattern EXCEL_DATE_PATTERN = Pattern.compile(
            "([\\[][$][-].+[\\]])?([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\\/|\\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\\/|\\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\\s][A|a][M|m][\\/][P|p][M|m])?)?");

    private DateFormatUtils()
    {
        // Utility class - no instantiation
    }

    /**
     * Check if a format string represents a date format.
     *
     * @param formatString the format string to check
     * @return true if the format string is a date format
     */
    public static boolean isDateFormat(String formatString)
    {
        if (formatString == null)
            return false;
        Matcher m = DATE_PATTERN.matcher(formatString.replace("\\", ""));
        return m.matches();
    }

    /**
     * Remove characters that cause issues when parsing date format strings.
     *
     * @param formatString the format string to clean
     * @return the cleaned format string
     */
    public static String removeBadDateFormatCharacters(String formatString)
    {
        return formatString.replace("\\", "").replace("\"", "");
    }

    /**
     * Check if a format string is an Excel-style date format.
     *
     * @param formatString the format string to check
     * @return true if the format string is an Excel date format
     */
    public static boolean isExcelDateFormat(String formatString)
    {
        Matcher m = EXCEL_DATE_PATTERN.matcher(removeBadDateFormatCharacters(formatString));
        return m.matches();
    }
}
