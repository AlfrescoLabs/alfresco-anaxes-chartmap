package org.alfresco.deployment.util.model;

import java.util.Map;

public class HelmChartLocalCache {
    private String apiVersion;

    private Map<String, HelmChart[]> entries;

    public Map<String, HelmChart[]> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, HelmChart[]> e) {
        entries = e ;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String a) {
       apiVersion = a;
    }
}
