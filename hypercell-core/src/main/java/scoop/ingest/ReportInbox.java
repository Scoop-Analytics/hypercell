package scoop.ingest;

public class ReportInbox extends io.hypercell.core.metadata.ScoopMetadataObject {
    public String inboxID;
    public String workspaceID;
    public String forwardToInbox;
    public boolean transactional;
    
    public String getLabel() { return ""; }
    public String getInboxID() { return inboxID; }
}
