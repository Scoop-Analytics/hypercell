/**
 * Aggregation rule types for HyperCell.
 */
package com.scoopanalytics.hypercell.core.types;

import java.io.Serializable;

/**
 * Defines the aggregation rules available for formula calculations.
 */
public enum AggregationRule implements Serializable
{
    Sum, Count, Min, Max, Avg, CountDistinct, StdDev;

    /**
     * Parse a rule from a string.
     *
     * @param ruleString the string to parse
     * @return the matching rule, or null if not found
     */
    public static AggregationRule parseRule(String ruleString)
    {
        for (var v : values())
        {
            if (v.toString().equalsIgnoreCase(ruleString))
            {
                return v;
            }
        }
        return null;
    }
}
