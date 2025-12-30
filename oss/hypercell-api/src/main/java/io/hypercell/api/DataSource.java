package io.hypercell.api;

/**
 * Represents a data source for the calculation engine.
 * Used by EvaluationContext to identify external data that may need refreshing.
 */
public record DataSource(String sheetName) {}
