package org.alfresco.deployment.util;

import org.apache.commons.collections4.map.MultiKeyMap;

import org.alfresco.deployment.util.model.HelmChart;
import org.alfresco.deployment.util.model.HelmMaintainer;

import java.io.IOException;

public class PlantUmlChartMapPrinter extends ChartMapPrinter {

    public PlantUmlChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(outputFilename, charts, chart);
    }

    public void printHeader() throws IOException {
        writeLine("@startuml");
        writeLine("skinparam linetype ortho");  // TODO: get this from config
        writeLine("title Chart Map for " + chart.getNameFull());
        writer.flush();
    }

    public void printFooter() throws IOException {
        writeLine("center footer Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName());
        writeLine("@enduml");
        writer.flush();
    }

    public void printDependency (HelmChart parentChart, HelmChart dependentChart) throws IOException {
        writeLine(getNameAsPlantUmlReference(parentChart.getNameFull()) + "--->" + getNameAsPlantUmlReference(dependentChart.getNameFull()) + ":depends on");
        writer.flush();
    }

    public void printChart (HelmChart chart)  throws IOException {
        writeLine("artifact \"" + chart.getNameFull() + getComponentBody(chart) + "\" as " + getNameAsPlantUmlReference(chart.getNameFull()) + " " + getArtifactColor());
    }

    public void printComment(String comment)  throws IOException {
        writer.write("'" + comment + "\n");
    }

    protected String getComponentBody(HelmChart chart) {
        String body = getSeparator();
        body += "\\t" + chart.getName();
        body += getSeparator();
        body += "\\t" + chart.getVersion();
        body += getSeparator();
        body += "\\t" + getMaintainers(chart.getMaintainers());
        body += getSeparator();
        body += "\\t" + getKeywords(chart.getKeywords());
        return body;
    }

    protected String getSeparator() {
        return new String("\\n====\\n");
    }

    protected String getMaintainers(HelmMaintainer[] m) {
        String maintainers = new String("Maintainers: ");
        boolean first=true;
        if (m != null) {
            for (HelmMaintainer hm : m) {
                if (first) {
                    maintainers += hm.getName();
                    first=false;
                }
                else {
                    maintainers += ", " + hm.getName();
                }
            }
        }
        return maintainers;
    }

    protected String getKeywords(String[] k) {
        String keywords = new String("Keywords: ");
        boolean first=true;
        if (k != null) {
            for (String aKeyword : k) {
                if (first) {
                    keywords += aKeyword;
                    first=false;
                }
                else {
                    keywords += ", " + aKeyword;
                }
            }
        }
        return keywords;
    }

    protected String getNameAsPlantUmlReference(String s) {
        String reference = s.replace(':','_');
        reference = reference.replace('.','_');
        reference = reference.replace('-','_');
        return reference;
    }

    protected String getArtifactColor () {
        return "#PaleGreen";
    }
}
