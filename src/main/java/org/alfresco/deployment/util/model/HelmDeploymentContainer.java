package org.alfresco.deployment.util.model;

public class HelmDeploymentContainer {
    private String name;
    private String image;
    private String imagePullPolicy;
    // The _parent property is not found in the yaml file but rather is inserted
    // during template processing.  It records the chart that uses this container
    // since some charts are common across a deployment but differ where used in
    // the imageTag property
    private HelmChart _parent; // not in the model

    public String getImagePullPolicy() {return imagePullPolicy;}

    public void setImagePullPolicy(String imagePullPolicy) {this.imagePullPolicy = imagePullPolicy;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public HelmChart _getParent() {return this._parent;}

    public void _setParent(HelmChart parent) {this._parent = parent;}
}
