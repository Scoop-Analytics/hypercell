package scoop.reportseriestable;

import java.util.List;
import java.util.Set;
import scoop.ingest.ReportInbox;

public class ReportSeriesTable extends io.hypercell.core.metadata.ScoopMetadataObject {
    public static String timestampFormat = "yyyy-MM-dd HH:mm:ss";
    public String getTableName() { return ""; }
    public boolean isDepracated() { return false; }
    public io.hypercell.core.datatable.columnar.ColumnarDataTable getTableMetadata() { return new io.hypercell.core.datatable.columnar.ColumnarDataTable(); }
    public io.hypercell.core.datatable.ColumnMetadata[] getColumnMetadata() { return null; }
    public String getInboxID() { return ""; }
    
    public List<java.time.LocalDate> getDistinctSnapshotDates(String schema, java.sql.Statement stmt) { return null; }
    
    public io.hypercell.core.grid.MemWorkbook restrictColumns(List<String> list) { return null; }
    public void setKeyColumn(io.hypercell.core.datatable.ColumnMetadata cm) {}
    public void setChangeColumns(List<String> list) {}
    
    public static ReportSeriesTable pickBestTable(String id, List<ReportSeriesTable> l1, List<ReportInbox> l2, Set<String> s) { return null; }
}
