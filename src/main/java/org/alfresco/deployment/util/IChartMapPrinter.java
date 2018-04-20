package org.alfresco.deployment.util;

import org.alfresco.deployment.util.model.HelmChart;

import java.io.IOException;

public interface IChartMapPrinter {

    void printHeader() throws IOException;

    void printFooter() throws IOException;

    void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException;

    void printChartToImageDependency(HelmChart parentChart, String imageName) throws IOException;

    void printChart(HelmChart chart)  throws IOException;

    void printImage(String s) throws IOException;

    void printComment(String comment) throws IOException;

    void setOutputFilename (String outputFilename);

    String getOutputFilename ();

    void setChart(HelmChart chart);

    HelmChart getChart ();
}
