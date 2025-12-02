package io.hypercell.core.datatable;

import java.io.Serializable;

public enum AggregationRule implements Serializable
{
    Sum, Count, Min, Max, Avg, CountDistinct, StdDev;

    public static AggregationRule parseRule(String ruleString)
    {
        for(var v: values())
        {
            if (v.toString().equalsIgnoreCase(ruleString))
            {
                return v;
            }
        }
        return null;
    }
}
