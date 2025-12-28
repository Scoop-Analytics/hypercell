/**
 * Public API interfaces for HyperCell.
 *
 * <p>This package contains the public interfaces that define the HyperCell contract:
 *
 * <ul>
 *   <li>{@link io.hypercell.api.EvaluationContext} - Context for formula evaluation with external data</li>
 *   <li>{@link io.hypercell.api.CellValue} - Interface for cell value access</li>
 *   <li>{@link io.hypercell.api.CellAddress} - Row/column address for cell references</li>
 *   <li>{@link io.hypercell.api.DataSource} - Interface for external data sources</li>
 *   <li>{@link io.hypercell.api.FunctionRegistry} - Registry for custom function implementations</li>
 * </ul>
 *
 * <h2>Implementing Custom Evaluation Context</h2>
 * <pre>{@code
 * public class MyContext implements EvaluationContext {
 *     public Object resolveReference(String sheet, int row, int col) {
 *         // Return value from external source
 *     }
 *     public List<DataSource> getDataSources() {
 *         return myDataSources;
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
package io.hypercell.api;
