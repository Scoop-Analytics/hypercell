package io.hypercell.core.expression;

import io.hypercell.core.grid.MemCell;

/**
 * Simple cache for function calculation results.
 * This allows expressions to cache their computed values to avoid recalculation.
 */
public class MemCellCalculationCache {
    private MemCell cachedValue;

    /**
     * Get the cached value, or null if not yet cached.
     */
    public MemCell getValue() {
        return cachedValue;
    }

    /**
     * Cache a value for later retrieval.
     */
    public void cacheValue(MemCell value) {
        this.cachedValue = value;
    }

    /**
     * Clear the cached value.
     */
    public void clear() {
        this.cachedValue = null;
    }
}
