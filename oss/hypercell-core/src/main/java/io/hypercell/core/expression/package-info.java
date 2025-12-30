/**
 * Expression compilation and evaluation for HyperCell.
 *
 * <p>This package contains the core formula compilation and evaluation infrastructure:
 *
 * <ul>
 *   <li>{@link io.hypercell.core.expression.Compile} - Compiles formula strings into expression trees</li>
 *   <li>{@link io.hypercell.core.expression.HyperCellExpression} - Base class for all expression nodes</li>
 *   <li>{@link io.hypercell.core.expression.CompileContext} - Holds state during compilation</li>
 *   <li>{@link io.hypercell.core.expression.Function} - Base class for function implementations</li>
 * </ul>
 *
 * <h2>Function Categories</h2>
 * <ul>
 *   <li>{@link io.hypercell.core.expression.MathFunction} - Mathematical functions (SUM, AVERAGE, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.LogicalFunction} - Logical functions (IF, AND, OR, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.TextualFunction} - Text functions (CONCAT, LEFT, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.DateTimeFunction} - Date/time functions (DATE, NOW, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.LookupFunction} - Lookup functions (VLOOKUP, INDEX, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.StatisticalFunction} - Statistical functions (STDEV, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.FinancialFunction} - Financial functions (PMT, NPV, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.InformationFunction} - Information functions (ISBLANK, etc.)</li>
 *   <li>{@link io.hypercell.core.expression.FilterFunction} - Array filter functions (FILTER, SORT)</li>
 * </ul>
 *
 * @since 0.1.0
 */
package io.hypercell.core.expression;
