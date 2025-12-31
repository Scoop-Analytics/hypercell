/**
 * Scoop-specific evaluation context.
 */
package com.scoopanalytics.hypercell.bridge.scoop;

import com.scoopanalytics.hypercell.api.DataSource;
import com.scoopanalytics.hypercell.bridge.AbstractEnterpriseContext;
import com.scoopanalytics.hypercell.bridge.QueryRefreshHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Evaluation context adapter for Scoop Analytics.
 *
 * <p>This abstract class provides the bridge between HyperCell's
 * {@link com.scoopanalytics.hypercell.api.EvaluationContext} and Scoop's ScoopContext.
 *
 * <p><b>Implementation in Scoop:</b>
 * <pre>{@code
 * public class ScoopContextAdapter extends ScoopEvaluationContext {
 *     private final ScoopContext sc;
 *
 *     public ScoopContextAdapter(ScoopContext sc) {
 *         this.sc = sc;
 *         setQueryRefreshHandler(new MyScoopQueryRefreshHandler(sc));
 *         setUserId(sc.getUserId());
 *         setTenantId(sc.getOrganizationId());
 *     }
 *
 *     protected Object getScoopContext() {
 *         return sc;
 *     }
 *
 *     public List<DataSource> getDataSources() {
 *         // Convert Scoop's InputQueries to DataSource
 *         return convertInputQueries(sc.getInputQueries());
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public abstract class ScoopEvaluationContext extends AbstractEnterpriseContext
{
    private static final Logger logger = LoggerFactory.getLogger(ScoopEvaluationContext.class);

    /**
     * Get the underlying Scoop context.
     * Must be implemented by Scoop to provide the ScoopContext.
     *
     * @return the Scoop context object
     */
    protected abstract Object getScoopContext();

    @Override
    public void logAuditEvent(String eventType, String details)
    {
        // Log to Scoop's audit system
        logger.info("[AUDIT] {} - User: {} Tenant: {} - {}",
                eventType, getUserId(), getTenantId(), details);

        // Implementation would also call Scoop's audit logging:
        // Object sc = getScoopContext();
        // if (sc != null) {
        //     ((ScoopContext) sc).logAudit(eventType, details);
        // }
    }

    @Override
    public boolean hasPermission(String permission)
    {
        // Check permission via Scoop's security system
        // Object sc = getScoopContext();
        // if (sc != null) {
        //     return ((ScoopContext) sc).hasPermission(permission);
        // }

        // Default: allow all (override for actual implementation)
        return true;
    }

    @Override
    public Object resolveReference(String sheetName, int row, int col)
    {
        // Resolve references using Scoop's worksheet system
        // Object sc = getScoopContext();
        // if (sc != null) {
        //     return ((ScoopContext) sc).getCellValue(sheetName, row, col);
        // }
        return null;
    }

    @Override
    public List<DataSource> getDataSources()
    {
        // Return Scoop's configured data sources
        // Object sc = getScoopContext();
        // if (sc != null) {
        //     return convertToDataSources(((ScoopContext) sc).getInputQueries());
        // }
        return Collections.emptyList();
    }

    @Override
    public void refreshDataSource(DataSource dataSource)
    {
        // Refresh data source via Scoop
        // Object sc = getScoopContext();
        // if (sc != null) {
        //     ((ScoopContext) sc).refreshInputQuery(dataSource.getId());
        // }
        logger.info("Data source refresh requested: {}", dataSource);
    }
}
