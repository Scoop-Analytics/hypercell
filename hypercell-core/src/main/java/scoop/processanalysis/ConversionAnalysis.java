package scoop.processanalysis;
import java.util.Map;
import java.util.Set;

public class ConversionAnalysis {
    public ConversionAnalysis(scoop.ScoopContext sc, String s1, String s2, String s3, java.util.List<io.hypercell.core.datatable.ColumnMetadata> l) {}
    public Map<ChangeCell, Map<String, Set<String>>> getSnapshotKeys() { return null; }
    public Map<ChangeCell, Summary> getFinalSummaries() { return null; }
    public Map<ChangeCell, Summary> getNextSummaries() { return null; }
    
    public static class Summary {
        public String[] keys;
        public int[][] counts;
        public Double[][] avgDurations;
    }
}
