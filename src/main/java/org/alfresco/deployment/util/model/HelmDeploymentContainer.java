package org.alfresco.deployment.util.model;

public class HelmDeploymentContainer {
    private String helmChartName; // note this does come from the template but is inserted later to make the name unique for printing purposes
    private String name;
    private String image;
    private String imagePullPolicy;

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

    public String getHelmChartName() {return helmChartName;}

    void setHelmChartName(String helmChartName) {this.helmChartName = helmChartName;}
}
