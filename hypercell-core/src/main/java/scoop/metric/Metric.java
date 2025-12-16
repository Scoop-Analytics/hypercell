package scoop.metric;

public class Metric extends io.hypercell.core.metadata.ScoopMetadataObject {
    public MetricType getMetricType() { return new MetricType(); }
    public String getMetricName() { return ""; }
    public String getReportSeriesTableID() { return ""; }
    
    public static class MetricType {
        public boolean isKPI() { return false; }
    }
}