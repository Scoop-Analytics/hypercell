/**
 * Scoop Analytics integration for HyperCell.
 *
 * <p>This package provides ready-to-use integration classes for Scoop Analytics:
 *
 * <ul>
 *   <li>{@link com.scoopanalytics.hypercell.bridge.scoop.ScoopIntegration} - Main entry point (recommended)</li>
 *   <li>{@link com.scoopanalytics.hypercell.bridge.scoop.ScoopCallbacks} - Callback-based configuration</li>
 *   <li>{@link com.scoopanalytics.hypercell.bridge.scoop.ScoopQueryRefreshHandler} - Abstract query refresh handler</li>
 *   <li>{@link com.scoopanalytics.hypercell.bridge.scoop.ScoopEvaluationContext} - Abstract evaluation context</li>
 * </ul>
 *
 * <p><b>Recommended Usage (callback-based, no subclassing):</b>
 * <pre>{@code
 * ScoopCallbacks callbacks = ScoopCallbacks.builder()
 *     .userId(() -> sc.getUserId())
 *     .tenantId(() -> sc.getOrganizationId())
 *     .queryRefresher((workbook, sheet) -> refreshQuerySheet(workbook, sheet))
 *     .auditLogger((event, details) -> sc.logAudit(event, details))
 *     .build();
 *
 * ScoopIntegration integration = new ScoopIntegration(callbacks);
 * ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
 * workbook.calculate();
 * }</pre>
 *
 * @since 0.1.0
 */
package com.scoopanalytics.hypercell.bridge.scoop;
