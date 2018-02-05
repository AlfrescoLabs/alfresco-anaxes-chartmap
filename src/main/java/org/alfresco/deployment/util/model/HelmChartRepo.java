package org.alfresco.deployment.util.model;

import java.util.Map;

public class HelmChartRepo {
    String apiVersion;
    String generated;
    private Map<String, HelmChart[]> entries;

    public String getApiVersion() {
        return apiVersion;
    }
    public void setApiVersion(String s) {
        apiVersion = s ;
    }
    public String getGenerated() {
        return generated;
    }
    public void setGenerated(String s) {
        generated = s ;
    }
    public Map<String, HelmChart[]> getEntries() {
        return entries;
    }
    public void setEntries(Map<String, HelmChart[]> s) {
        entries = s ;
    }
}
