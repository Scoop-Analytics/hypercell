package io.hypercell.core.datatable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ColumnMetadata {
    public String columnName;
    public JsonNode getNode(ObjectMapper om) { return null; }
    public boolean isMeasure() { return false; }
    
    public static ColumnMetadata getUniqueKey(ColumnMetadata[] cms) { return null; }
}
