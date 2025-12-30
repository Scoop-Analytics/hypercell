/**
 * ANTLR4 grammar and generated parser for Excel formulas.
 *
 * <p>This package contains the ANTLR4-generated lexer and parser for Excel formula syntax:
 *
 * <ul>
 *   <li>{@link io.hypercell.formula.HyperCellExpressionLexer} - Tokenizer for formula strings</li>
 *   <li>{@link io.hypercell.formula.HyperCellExpressionParser} - Parser producing parse trees</li>
 *   <li>{@link io.hypercell.formula.HyperCellDateLexer} - Tokenizer for date strings</li>
 *   <li>{@link io.hypercell.formula.HyperCellDateParser} - Parser for date formats</li>
 * </ul>
 *
 * <p>The grammar supports full Excel formula syntax including:
 * <ul>
 *   <li>Cell references (A1, $A$1, Sheet1!A1)</li>
 *   <li>Range references (A1:B10)</li>
 *   <li>Named ranges</li>
 *   <li>All standard Excel operators (+, -, *, /, ^, &amp;, =, &lt;&gt;, etc.)</li>
 *   <li>200+ built-in functions</li>
 *   <li>Array formulas and spill ranges</li>
 * </ul>
 *
 * @since 0.1.0
 */
package io.hypercell.formula;
