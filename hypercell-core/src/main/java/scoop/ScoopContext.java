package scoop;

import scoop.connector.definition.ConnectorDefinition;
import scoop.ingest.ReportInbox;
import io.hypercell.core.metadata.ScoopMetadata;
import scoop.metric.Metric;
import scoop.reportseriestable.ReportSeriesTable;
import io.hypercell.core.user.Account;
import io.hypercell.core.user.User;
import scoop.worksheet.Worksheet;
import io.hypercell.core.workspace.Workspace;

import java.util.List;
import java.util.Map;

public class ScoopContext {
    private final ScoopMetadata sm;
    private Workspace workspace;
    private User user;
    private Account account;
    private Map<String, ConnectorDefinition> connectorDefinitionMap = null;

    public ScoopContext(ScoopMetadata sm, String workspaceID, String userID) {
        this.sm = sm;
    }

    public ScoopMetadata sm() {
        return sm;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getUser() {
        return user;
    }

    public Account getAccount() {
        return account;
    }

    public Worksheet getKPIWorkbookSheet() {
        return null;
    }

    public List<Metric> getMetrics(boolean includeNonKPIs, String metricName) {
        return null;
    }

    public List<Metric> getMetrics() {
        return null;
    }

    public List<ReportSeriesTable> getReportSeriesTablesForWorkspace() {
        return null;
    }

    public ReportSeriesTable getReportSeriesTable(String reportSeriesTableID) {
        return null;
    }

    public List<ReportInbox> getInboxesForWorkspace() {
        return null;
    }

    public List<ReportInbox> getInboxesForWorkspace(String workspaceID) {
        return null;
    }

    public List<Metric> getMetricsForWorkspace() {
        return null;
    }

    public List<Metric> getMetricsForInbox(String inboxID) {
        return null;
    }

    public List<ReportSeriesTable> getReportSeriesTablesForWorkspaceID(boolean includeDeprecated) {
        return null;
    }

    public List<ReportSeriesTable> getReportSeriesTablesForInboxID(String inboxID, boolean includeDeprecated) {
        return null;
    }

    public ReportSeriesTable getPrimaryReportSeriesTableForInboxID(String inboxID) {
        return null;
    }

    public List<ReportSeriesTable> getReportSeriesTablesForInboxID(String inboxID) {
        return null;
    }

    public Map<String, ConnectorDefinition> getConnectorDefinitionMap() {
        return connectorDefinitionMap;
    }
    
    public String workspaceID() { return ""; }
}