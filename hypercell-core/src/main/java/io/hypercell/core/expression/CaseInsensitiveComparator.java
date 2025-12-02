package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import java.util.Comparator;

public class CaseInsensitiveComparator implements Comparator<String>
{
    @Override
    public int compare(String str1, String str2)
    {
        return str1.compareToIgnoreCase(str2);
    }
}
