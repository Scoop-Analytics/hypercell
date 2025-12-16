package io.hypercell.core.metadata;

import java.util.List;

public class ScoopMetadata {
    public Object loadMetadataObject(Class<?> clazz, String id) { return null; }
    public io.hypercell.core.user.User getUserFromID(String id) { return null; }
    public List<ScoopMetadataObject> queryForObjects(Class<?> clazz, String field, String value) { return new java.util.ArrayList<>(); }
    public List<ScoopMetadataObject> queryForObjects(Class<?> clazz, String query) { return new java.util.ArrayList<>(); }
    public List<ScoopMetadataObject> queryForObjects(Class<?> clazz, String field, String value, Class<?> clazz2) { return new java.util.ArrayList<>(); }
    public List<ScoopMetadataObject> queryForObjects(Class<?> clazz, String field, String value, String sort) { return new java.util.ArrayList<>(); }
    public ScoopConfig getConfig() { return new ScoopConfig(); }
    public List<String> getBucketS3Keys(String bucket, String folder) { return new java.util.ArrayList<>(); }
    public String getS3ObjectString(String bucket, String key) { return ""; }
    
    public String getCustomers3Bucket() { return ""; }
    public java.util.List<scoop.processanalysis.ProcessAnalysisDefinition> getProcessDiagrams(String id) { return new java.util.ArrayList<>(); }
    public byte[] getS3ObjectBytes(String bucket, String key) throws scoop.ScoopException { return new byte[0]; }
    public java.sql.Connection getDataDBConnection(String c, String s) { return null; }
    
    public static class ScoopConfig {
        public <T> T getProperty(String key, Class<T> clazz) { return null; }
    }
}