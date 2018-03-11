package org.alfresco.deployment.util.model;

public class HelmDeploymentSpecTemplate {
    private HelmDeploymentSpecTemplateSpec spec;

    HelmDeploymentSpecTemplateSpec getSpec() {
        return spec;
    }

    public void setSpec(HelmDeploymentSpecTemplateSpec spec) {
        this.spec = spec;
    }
}
