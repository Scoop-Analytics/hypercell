package io.hypercell.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting and validating Excel format strings.
 * Extracted from legacy ExcelDataGrid.
 */
public class FormattingUtils {
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\/|\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\/|\-]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\s][A|a][M|m][\/][P|p][M|m])?)?");
    private static final Pattern EXCEL_DATE_PATTERN = Pattern.compile(
            "([[\]][$][-].+[\]])?([m|M]{1,4}|[d|D]{1,4}|y{2,4})?([\/|\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?([\/|\-| ]([m|M]{1,4}|[d|D]{1,4}|[y|Y]{2,4}))?(\s?[h|H]{1,2}[:][m|M]{1,2}([:][s]{1,2})?([\s][A|a][M|m][\/][P|p][M|m])?)?");

    public static String getNumericFormatString(String excelString)
    {
        return excelString.replace("\", "")
                   .replaceAll("([[\]][$][-].+[\]])?([_][(][^\s]*\s)?([_][)])?",
                           "");
    }

    public static boolean isDateFormat(String formatString)
    {
        if (formatString == null)
            return false;
        Matcher m = DATE_PATTERN.matcher(formatString.replace("\", ""));
        return m.matches();
    }

    public static String removeBadDateFormatCharacters(String formatString)
    {
        return formatString.replace("\", "").replace("\"", "");
    }

    public static boolean isExcelDateFormat(String formatString)
    {
        Matcher m = EXCEL_DATE_PATTERN.matcher(removeBadDateFormatCharacters(formatString));
        return m.matches();
    }
}
