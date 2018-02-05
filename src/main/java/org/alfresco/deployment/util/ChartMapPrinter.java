package org.alfresco.deployment.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import org.apache.commons.collections4.map.MultiKeyMap;

import org.alfresco.deployment.util.model.HelmChart;

public class ChartMapPrinter implements IChartMapPrinter {

    protected MultiKeyMap charts;
    protected HelmChart chart;
    protected String outputFilename;
    protected FileWriter writer;

    public ChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart ) {
        this.outputFilename = outputFilename;
        this.charts = charts;
        this.chart = chart;
        try {
            writer = new FileWriter(outputFilename);
        }
        catch (IOException e) {
            System.out.println("Error creating FileWriter for file " + outputFilename + " : " + e.getMessage());
        }
    }

    protected void writeLine (String l ) throws IOException {
        try {
            writer.write(l + "\n");
            writer.flush();
        }
        catch (IOException e) {
            System.out.println("Error writing line to file " + outputFilename);
            throw(e);
        }
    }

    public void printHeader() throws IOException {
       writeLine("Chart Map for " + chart.getNameFull());
    }

    public void printFooter() throws IOException {
        writeLine("Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName());
    }

    public void printDependency (HelmChart parentChart, HelmChart dependentChart)   throws IOException {
        writeLine(parentChart.getNameFull() + " depends on " + dependentChart.getNameFull());
    }

    public void printChart(HelmChart chart)   throws IOException {
        writeLine("Chart: " + chart.getNameFull());
        writeLine("\tapiVersion: " + chart.getApiVersion());
        writeLine("\tappVersion: " + chart.getAppVersion());
        writeLine("\tcreated: " + chart.getCreated());
        writeLine("\tdependencies: " + chart.getDependencies());
        writeLine("\tdescription: " + chart.getDescription());
        writeLine("\tdigest: " + chart.getDigest());
        writeLine("\ticon: " + chart.getIcon());
        writeLine("\tkeywords: " + chart.getKeywords());
        writeLine("\tmaintainers: " + chart.getMaintainers());
        writeLine("\tname: " + chart.getName());
        writeLine("\tsources: " + chart.getSources());
        writeLine("\turls: " + chart.getUrls());
        writeLine("\tversion: " + chart.getVersion());
    }

    public void printComment(String comment)  throws IOException {
        writeLine(comment);
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setChart(HelmChart chart) {
        this.chart = chart;
    }

    public HelmChart getChart() {
        return chart;
    }

    protected String getCurrentDateTime () {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return(f.format(LocalDateTime.now()));
    }
}
