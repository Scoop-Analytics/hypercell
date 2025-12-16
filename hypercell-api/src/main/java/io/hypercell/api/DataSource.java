package io.hypercell.api;

/**
 * Represents a data source for the calculation engine.
 * Replaces the legacy scoop.worksheet.InputQuery.
 */
public record DataSource(String sheetName) {}
