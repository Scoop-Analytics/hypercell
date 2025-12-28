/**
 * Bridge module for enterprise integration with HyperCell.
 *
 * <p>This package provides extension points for connecting HyperCell
 * to enterprise platforms without modifying the core OSS code:
 *
 * <ul>
 *   <li>{@link io.hypercell.bridge.EnterpriseEvaluationContext} - Extended context with enterprise features</li>
 *   <li>{@link io.hypercell.bridge.QueryRefreshHandler} - Interface for data source refresh</li>
 *   <li>{@link io.hypercell.bridge.ExtendedWorkbook} - Workbook with enterprise extensions</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Enterprise platforms implement these interfaces to add platform-specific
 * functionality while using the core HyperCell calculation engine.</p>
 *
 * @since 0.1.0
 */
package io.hypercell.bridge;
