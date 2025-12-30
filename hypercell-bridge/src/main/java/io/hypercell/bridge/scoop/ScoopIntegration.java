/**
 * Main integration point for Scoop Analytics.
 */
package io.hypercell.bridge.scoop;

import io.hypercell.api.DataSource;
import io.hypercell.bridge.ExtendedWorkbook;
import io.hypercell.bridge.QueryRefreshHandler;
import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.MemWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main entry point for Scoop Analytics integration with HyperCell.
 *
 * <p>This class provides a simple, callback-based integration that allows
 * Scoop to use HyperCell without subclassing abstract classes.
 *
 * <p><b>Complete Usage Example in Scoop:</b>
 * <pre>{@code
 * // In Scoop's CalculatedSourceWorkbook or similar:
 * public class HyperCellIntegration {
 *     private final ScoopContext sc;
 *     private final ScoopIntegration integration;
 *
 *     public HyperCellIntegration(ScoopContext sc) {
 *         this.sc = sc;
 *         this.integration = createIntegration();
 *     }
 *
 *     private ScoopIntegration createIntegration() {
 *         ScoopCallbacks callbacks = ScoopCallbacks.builder()
 *             .userId(() -> sc.getUserId())
 *             .tenantId(() -> sc.getOrganizationId())
 *             .dataSources(() -> convertInputQueries(sc.getInputQueries()))
 *             .queryRefresher(this::refreshSheet)
 *             .auditLogger((event, details) ->
 *                 logger.info("[AUDIT] {} - {}", event, details))
 *             .build();
 *
 *         return new ScoopIntegration(callbacks);
 *     }
 *
 *     private boolean refreshSheet(MemWorkbook workbook, MemSheet sheet) {
 *         CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(sc, null, workbook);
 *         var queries = csw.getQueries(false, false);
 *         for (var iq : queries) {
 *             if (iq.sheetName.startsWith(sheet.getName())) {
 *                 csw.refreshInputQuery(iq);
 *             }
 *         }
 *         sheet.setQuerySheetUpdated(true);
 *         return true;
 *     }
 *
 *     public void calculate(MemWorkbook memWorkbook) {
 *         ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
 *         workbook.setAutoRefreshQueries(true);
 *         workbook.calculate();
 *     }
 *
 *     public Object getCellValue(MemWorkbook memWorkbook, String sheet, int row, int col) {
 *         ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
 *         return workbook.getCellValue(sheet, row, col);
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public class ScoopIntegration
{
    private static final Logger logger = LoggerFactory.getLogger(ScoopIntegration.class);

    private final ScoopCallbacks callbacks;
    private final CallbackEvaluationContext context;
    private final CallbackQueryRefreshHandler queryRefreshHandler;

    /**
     * Create a new Scoop integration with the provided callbacks.
     *
     * @param callbacks the callbacks providing Scoop-specific logic
     */
    public ScoopIntegration(ScoopCallbacks callbacks)
    {
        this.callbacks = callbacks;
        this.queryRefreshHandler = new CallbackQueryRefreshHandler(callbacks);
        this.context = new CallbackEvaluationContext(callbacks, queryRefreshHandler);
    }

    /**
     * Create an ExtendedWorkbook ready for calculation.
     *
     * @param memWorkbook the underlying workbook
     * @return an extended workbook with enterprise features
     */
    public ExtendedWorkbook createWorkbook(MemWorkbook memWorkbook)
    {
        return new ExtendedWorkbook(memWorkbook, context);
    }

    /**
     * Get the evaluation context for direct use.
     *
     * @return the configured evaluation context
     */
    public CallbackEvaluationContext getContext()
    {
        return context;
    }

    /**
     * Get the query refresh handler for direct use.
     *
     * @return the configured query refresh handler
     */
    public QueryRefreshHandler getQueryRefreshHandler()
    {
        return queryRefreshHandler;
    }

    /**
     * Evaluation context implementation that delegates to callbacks.
     */
    public static class CallbackEvaluationContext extends ScoopEvaluationContext
    {
        private final ScoopCallbacks callbacks;

        public CallbackEvaluationContext(ScoopCallbacks callbacks, QueryRefreshHandler handler)
        {
            this.callbacks = callbacks;
            setQueryRefreshHandler(handler);
            setUserId(callbacks.getUserId());
            setTenantId(callbacks.getTenantId());
        }

        @Override
        protected Object getScoopContext()
        {
            return callbacks;
        }

        @Override
        public List<DataSource> getDataSources()
        {
            return callbacks.getDataSources();
        }

        @Override
        public void refreshDataSource(DataSource dataSource)
        {
            callbacks.refreshDataSource(dataSource);
        }

        @Override
        public void logAuditEvent(String eventType, String details)
        {
            callbacks.logAuditEvent(eventType, details);
        }

        @Override
        public boolean hasPermission(String permission)
        {
            return callbacks.hasPermission(permission);
        }

        @Override
        public Object resolveReference(String sheetName, int row, int col)
        {
            return callbacks.resolveReference(sheetName, row, col);
        }
    }

    /**
     * Query refresh handler implementation that delegates to callbacks.
     */
    public static class CallbackQueryRefreshHandler implements QueryRefreshHandler
    {
        private final ScoopCallbacks callbacks;

        public CallbackQueryRefreshHandler(ScoopCallbacks callbacks)
        {
            this.callbacks = callbacks;
        }

        @Override
        public boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet)
        {
            boolean result = callbacks.refreshQuerySheet(workbook, sheet);
            if (result)
            {
                sheet.setQuerySheetUpdated(true);
            }
            return result;
        }

        @Override
        public boolean isQuerySheet(MemSheet sheet)
        {
            return callbacks.isQuerySheet(sheet);
        }

        @Override
        public boolean needsRefresh(MemSheet sheet)
        {
            return callbacks.needsRefresh(sheet);
        }
    }
}
