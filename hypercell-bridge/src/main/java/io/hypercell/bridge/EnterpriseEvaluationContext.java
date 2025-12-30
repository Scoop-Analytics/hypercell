/**
 * Extended evaluation context for enterprise features.
 */
package io.hypercell.bridge;

import io.hypercell.api.EvaluationContext;

/**
 * Extended evaluation context that provides enterprise-specific features.
 *
 * <p>This interface extends the base {@link EvaluationContext} with
 * additional capabilities for enterprise platforms:
 * <ul>
 *   <li>Query refresh handling</li>
 *   <li>User context and permissions</li>
 *   <li>Audit logging</li>
 *   <li>Custom function registration</li>
 * </ul>
 *
 * @since 0.1.0
 */
public interface EnterpriseEvaluationContext extends EvaluationContext
{
    /**
     * Get the query refresh handler for this context.
     *
     * @return the query refresh handler, or null if not available
     */
    QueryRefreshHandler getQueryRefreshHandler();

    /**
     * Get the current user ID for audit logging.
     *
     * @return the user ID, or null if not authenticated
     */
    String getUserId();

    /**
     * Get the current tenant/organization ID for multi-tenant environments.
     *
     * @return the tenant ID, or null if not applicable
     */
    String getTenantId();

    /**
     * Log a calculation event for audit purposes.
     *
     * @param eventType the type of event (e.g., "CALCULATE", "REFRESH")
     * @param details additional details about the event
     */
    void logAuditEvent(String eventType, String details);

    /**
     * Check if the current user has permission for an operation.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    boolean hasPermission(String permission);
}
