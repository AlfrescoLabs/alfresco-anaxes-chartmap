package org.alfresco.deployment.util;

import org.alfresco.deployment.util.model.HelmDeploymentContainer;
import org.alfresco.deployment.util.model.HelmChart;

import java.io.IOException;

public interface IChartMapPrinter {

    void printHeader() throws IOException;

    void printFooter() throws IOException;

    void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException;

    void printChartToContainerDependency(HelmChart parentChart, HelmDeploymentContainer container) throws IOException;

    void printChart(HelmChart chart)  throws IOException;

    void printContainer(HelmDeploymentContainer container) throws IOException;

    void printComment(String comment) throws IOException;

    void setOutputFilename (String outputFilename);

    String getOutputFilename ();

    void setChart(HelmChart chart);

    HelmChart getChart ();
}
