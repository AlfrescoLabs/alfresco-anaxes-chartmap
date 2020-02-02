package org.alfresco.deployment.util.model;

public class HelmDeploymentSpecTemplateSpec {
    private HelmDeploymentContainer[] containers;
    private String hostNetwork;

    public HelmDeploymentContainer[] getContainers() {
        return containers;
    }

    public void setContainers(HelmDeploymentContainer[] containers) {
        this.containers = containers;
    }

    public String getHostNetwork() {
        return hostNetwork;
    }

    public void setHostNetwork(String hostNetwork) {
        this.hostNetwork = hostNetwork;
    }
}
