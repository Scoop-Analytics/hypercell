package io.hypercell.core.expression;

public class SpillArea
{
    public int width;
    public int height;

    public static SpillArea getLargestSpillArea(SpillArea a, SpillArea b)
    {
        if (a == null && b == null)
            return null;
        else if (a == null)
            return b;
        else if (b == null)
            return a;
        SpillArea largest = new SpillArea();
        if (a.height == -1 || b.height == -1)
            largest.height = -1;
        else
            largest.height = Math.max(a.height, b.height);
        if (a.width == -1 || b.width == -1)
            largest.width = -1;
        else
            largest.width = Math.max(a.width, b.width);
        return largest;
    }
}
