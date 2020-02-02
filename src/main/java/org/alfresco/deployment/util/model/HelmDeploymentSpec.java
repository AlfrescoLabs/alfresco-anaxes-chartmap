package org.alfresco.deployment.util.model;

public class HelmDeploymentSpec {
    private HelmDeploymentSpecTemplate template;
    private String replicas;

    public HelmDeploymentSpecTemplate getTemplate() {
        return template;
    }

    public void setTemplate(HelmDeploymentSpecTemplate template) {
        this.template = template;
    }

    public String getReplicas() {
        return replicas;
    }

    public void setReplicas(String replicas) {
        this.replicas = replicas;
    }
}
