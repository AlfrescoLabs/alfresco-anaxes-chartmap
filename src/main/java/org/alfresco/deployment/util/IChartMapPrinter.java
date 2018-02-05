package org.alfresco.deployment.util;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.alfresco.deployment.util.model.HelmChart;

import java.io.IOException;

public interface IChartMapPrinter {

    void printHeader() throws IOException;

    void printFooter() throws IOException;

    void printDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException;

    void printChart(HelmChart chart)  throws IOException;

    void printComment(String comment) throws IOException;

    void setOutputFilename (String outputFilename);

    String getOutputFilename ();

    void setChart(HelmChart chart);

    HelmChart getChart ();
}
