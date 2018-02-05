package org.alfresco.deployment.util.model;

public class HelmRequirements {
    private HelmRequirement[] dependencies;

    public HelmRequirement[] getDependencies() { return dependencies;}

    public void setDependencies (HelmRequirement[] r) { dependencies = r ;}
}
