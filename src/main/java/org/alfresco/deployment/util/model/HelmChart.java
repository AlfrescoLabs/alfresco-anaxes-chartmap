package org.alfresco.deployment.util.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import org.alfresco.deployment.util.ChartUtil;

public class HelmChart {
    private String apiVersion;
    private String appVersion;
    private String created;
    private String description;
    private HashSet<HelmChart> dependencies = new HashSet<>();
    private HashSet<HelmDeploymentTemplate> deploymentTemplates = new HashSet<>();
    private String digest;
    private String icon;
    private String[] keywords;
    private HelmMaintainer[] maintainers;
    private String name;
    private String[] sources;
    private String[] urls;
    private Map<String, Object> values;
    private String version;


    /**
     * Collects the containers referenced by this chart and returns a collection of them
     *
     * @return a collection of the containers referenced by this chart
     */
    /**
    public ArrayList<HelmDeploymentContainer> getContainers() {
        ArrayList<HelmDeploymentContainer> containers = new ArrayList<>();
        for (HelmDeploymentTemplate t : deploymentTemplates) {
            containers.addAll(Arrays.asList(t.getSpec().getTemplate().getSpec().getContainers()));
        }
        return containers;
    }
    **/
    public HashSet<String> getContainers() {
        HashSet<String> containers = new HashSet<String>();
        for (HelmDeploymentTemplate t : deploymentTemplates) {
            String image=null;
            HelmDeploymentContainer[] hdc = t.getSpec().getTemplate().getSpec().getContainers();
            if (hdc != null && hdc.length > 0) {
               image = hdc[0].getImage();
            }
            if (image !=null) {
                containers.add(image);
            }
        }
        return containers;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String a) {
        apiVersion = a;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String a) {
        appVersion = a;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String s) {
        created = s;
    }

    public HashSet<HelmChart> getDependencies() {
        return dependencies;
    }

    public void setDependencies(HashSet<HelmChart> d) {
        dependencies = d;
    }

    public HashSet<HelmDeploymentTemplate> getDeploymentTemplates() {
        return deploymentTemplates;
    }

    public void setDeploymentTemplates(HashSet<HelmDeploymentTemplate> deploymentTemplates) {
        this.deploymentTemplates = deploymentTemplates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        description = d;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String d) {
        digest = d;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String i) {icon = i;}

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] k) {
        keywords = k;
    }

    public HelmMaintainer[] getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(HelmMaintainer[] m) {
        maintainers = m;
    }

    public String getNameFull() {return name + ":" + version;}

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public String[] getSources() {
        return sources;
    }

    public void setSources(String[] s) {
        sources = s;
    }

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] u) {
        urls = u;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String v) {
        version = v;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> v) {
        values = v;
    }

    public String getValue(String k) {
        return ChartUtil.getValue(k, values);
    }
}
