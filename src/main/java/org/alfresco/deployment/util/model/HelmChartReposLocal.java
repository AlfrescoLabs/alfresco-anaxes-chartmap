package org.alfresco.deployment.util.model;

public class HelmChartReposLocal {
    private String apiVersion;
    private String generated;
    private HelmChartRepoLocal[] repositories;

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
    public HelmChartRepoLocal[] getRepositories() {
        return repositories;
    }
    public void setRepos(HelmChartRepoLocal[] r) {
        repositories = r ;
    }
}