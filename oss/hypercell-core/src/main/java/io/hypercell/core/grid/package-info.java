/**
 * In-memory spreadsheet grid representation for HyperCell.
 *
 * <p>This package contains the core data structures for representing Excel workbooks:
 *
 * <ul>
 *   <li>{@link io.hypercell.core.grid.MemWorkbook} - In-memory workbook containing multiple sheets</li>
 *   <li>{@link io.hypercell.core.grid.MemSheet} - Individual worksheet with cell grid</li>
 *   <li>{@link io.hypercell.core.grid.MemCell} - Cell containing value, formula, and formatting</li>
 *   <li>{@link io.hypercell.core.grid.MemCellType} - Enumeration of cell value types</li>
 *   <li>{@link io.hypercell.core.grid.MemCellStyle} - Cell formatting and style information</li>
 *   <li>{@link io.hypercell.core.grid.FormulaError} - Excel error types (#VALUE!, #REF!, etc.)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MemWorkbook workbook = new MemWorkbook("MyWorkbook");
 * MemSheet sheet = workbook.addSheet("Sheet1");
 * sheet.setCellValue(0, 0, 100);
 * sheet.setCellValue(0, 1, 200);
 * MemCell cell = sheet.getCellAt(0, 2);
 * cell.setFormula("=A1+B1");
 * workbook.calculate();
 * Number result = cell.getNumberValue();  // 300
 * }</pre>
 *
 * @since 0.1.0
 */
package io.hypercell.core.grid;
