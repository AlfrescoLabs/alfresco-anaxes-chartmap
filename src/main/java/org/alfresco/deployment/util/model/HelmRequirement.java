package org.alfresco.deployment.util.model;

public class HelmRequirement {

    private String condition;
    private String name;
    private String repository;
    private String version;

    public String getCondition() {return condition;}

    public void setCondition (String c) {condition = c;}

    public String getName() {return name;}

    public void setName (String n) {name = n;}

    public String getRepository() {return repository;}

    public void setRepository (String r) { repository = r;}

    public String getVersion() {return version;}

    public void setVersion (String v) {version = v;}
}


