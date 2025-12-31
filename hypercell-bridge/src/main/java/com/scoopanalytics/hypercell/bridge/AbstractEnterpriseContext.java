/**
 * Abstract base implementation of EnterpriseEvaluationContext.
 */
package com.scoopanalytics.hypercell.bridge;

import com.scoopanalytics.hypercell.api.DataSource;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class providing default implementations for
 * {@link EnterpriseEvaluationContext}.
 *
 * <p>Enterprise platforms can extend this class and override only
 * the methods they need.
 *
 * @since 0.1.0
 */
public abstract class AbstractEnterpriseContext implements EnterpriseEvaluationContext
{
    private QueryRefreshHandler queryRefreshHandler;
    private String userId;
    private String tenantId;

    @Override
    public QueryRefreshHandler getQueryRefreshHandler()
    {
        return queryRefreshHandler;
    }

    public void setQueryRefreshHandler(QueryRefreshHandler queryRefreshHandler)
    {
        this.queryRefreshHandler = queryRefreshHandler;
    }

    @Override
    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    @Override
    public String getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(String tenantId)
    {
        this.tenantId = tenantId;
    }

    @Override
    public void logAuditEvent(String eventType, String details)
    {
        // Default: no-op. Override to implement audit logging.
    }

    @Override
    public boolean hasPermission(String permission)
    {
        // Default: all permissions granted. Override for access control.
        return true;
    }

    @Override
    public List<DataSource> getDataSources()
    {
        // Default: no data sources. Override to provide external data.
        return Collections.emptyList();
    }

    @Override
    public void refreshDataSource(DataSource dataSource)
    {
        // Default: no-op. Override to implement data refresh.
    }

    @Override
    public Object resolveReference(String sheetName, int row, int col)
    {
        // Default: return null. Override to resolve external references.
        return null;
    }
}
