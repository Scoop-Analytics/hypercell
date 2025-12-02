package scoop.worksheet;

import io.hypercell.core.expression.Identifier;
import io.hypercell.core.grid.MemCell;

import java.util.HashMap;
import java.util.Map;

public class MemCellCalculationCache
{
    private Object currentKey;

    private Map<Object, MemCell> cache = new HashMap<>();

    public void setKey(Object key)
    {
        this.currentKey = key;
    }

    public void cacheValue(MemCell value)
    {
        cache.put(currentKey, value);
    }

    public MemCell getValue()
    {
        return cache.get(currentKey);
    }
}
