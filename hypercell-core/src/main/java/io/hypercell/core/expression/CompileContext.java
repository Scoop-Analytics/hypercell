/**
 *
 */
package io.hypercell.core.expression;

import java.util.ArrayList;
import java.util.List;

import scoop.ScoopContext;
import io.hypercell.core.grid.MemSheet;

/**
 * @author bradpeters
 */
public class CompileContext
{
    private final List<Identifier> identifierList = new ArrayList<>();
    private final List<Range> rangeList = new ArrayList<>();
    private ScoopContext sc;
    private MemSheet sheet;
    private boolean informationalOnly = false;
    private boolean containsAggregation;

    public CompileContext(ScoopContext sc, MemSheet sheet)
    {
        this.sc = sc;
        this.sheet = sheet;
    }

    public void addIdentifier(Identifier id)
    {
        identifierList.add(id);
    }

    public void addRange(Range range)
    {
        rangeList.add(range);
    }

    public boolean setIdentifierLocation(String name, int row, int column)
    {
        for (Identifier id : identifierList)
        {
            if (id.getName().equals(name))
            {
                id.setRow(row);
                id.setColumn(column);
                return true;
            }
        }
        return false;
    }

    public List<Identifier> getIdentifierList()
    {
        return identifierList;
    }

    public List<Range> getRangeList()
    {
        return rangeList;
    }

    public MemSheet getSheet()
    {
        return sheet;
    }

    public void setSheet(MemSheet sheet)
    {
        this.sheet = sheet;
    }

    public boolean isInformationalOnly()
    {
        return informationalOnly;
    }

    public void setInformationalOnly(boolean informationalOnly)
    {
        this.informationalOnly = informationalOnly;
    }

    public ScoopContext getSc()
    {
        return sc;
    }

    public void setSc(ScoopContext sc)
    {
        this.sc = sc;
    }

    public void setContainsAggregation(boolean containsAggregation)
    {
        this.containsAggregation = containsAggregation;
    }

    public boolean isContainsAggregation()
    {
        return containsAggregation;
    }
}
