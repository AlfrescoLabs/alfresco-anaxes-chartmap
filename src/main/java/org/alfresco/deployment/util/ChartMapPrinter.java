package org.alfresco.deployment.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import org.alfresco.deployment.util.model.HelmMaintainer;
import org.apache.commons.collections4.map.MultiKeyMap;

import org.alfresco.deployment.util.model.HelmChart;

public class ChartMapPrinter implements IChartMapPrinter {

    HelmChart chart;
    private String outputFilename;
    FileWriter writer;

    ChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        this.outputFilename = outputFilename;
        this.chart = chart;
        try {
            writer = new FileWriter(outputFilename);
        } catch (IOException e) {
            System.out.println("Error creating FileWriter for file " + outputFilename + " : " + e.getMessage());
        }
    }

    void writeLine(String l) throws IOException {
        try {
            writer.write(l + "\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing line to file " + outputFilename);
            throw (e);
        }
    }

    public void printHeader() throws IOException {
        writeLine("Chart Map for " + chart.getNameFull());
    }

    public void printFooter() throws IOException {
        writeLine("Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName() + " (https://github.com/Alfresco/alfresco-anaxes-chartmap)");
    }

    public void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException {
        writeLine(parentChart.getNameFull() + " depends on " + dependentChart.getNameFull());
    }

    public void printChartToImageDependency(HelmChart chart, String imageName) throws IOException {
        writeLine(chart.getNameFull() + " uses " + imageName);
    }

    public void printChart(HelmChart chart) throws IOException {
        writeLine("Chart: " + chart.getNameFull());
        writeLine("\tapiVersion: " + chart.getApiVersion());
        writeLine("\tappVersion: " + chart.getAppVersion());
        writeLine("\tcreated: " + chart.getCreated());
        writeLine("\tdependencies: " + chart.getDependencies());
        writeLine("\tdescription: " + chart.getDescription());
        writeLine("\tdigest: " + chart.getDigest());
        writeLine("\ticon: " + chart.getIcon());
        writeLine("\tkeywords: " + printArray(chart.getKeywords()));
        writeLine("\tmaintainers: " + printMaintainers(chart.getMaintainers()));
        writeLine("\tname: " + chart.getName());
        writeLine("\tsources: " + printArray(chart.getSources()));
        writeLine("\turls: " + printArray(chart.getUrls()));
        writeLine("\tversion: " + chart.getVersion());
    }

    public void printImage(String c) throws IOException {
        writeLine("Image: " + c);
    }

    public void printComment(String comment) throws IOException {
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

    String getCurrentDateTime() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return (f.format(LocalDateTime.now()));
    }

    /**
     * Returns a string with the elements of an array separated by commas
     *
     * @param a the array to be formatted
     * @return the string form of the array
     */
    private String printArray(String[] a) {
        StringBuilder sb = new StringBuilder("");
        if (a !=null) {
            for (int i = 0; i < a.length; i++) {
                sb.append(a[i]);
                if (i != a.length - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns a string with the name and email addresses of the maintainers of a Helm Chart
     * separated by commas
     *
     * @param m an array of Helm Maintainers
     * @return the string form of the maintainers array
     */
    private String printMaintainers(HelmMaintainer[] m) {
        StringBuilder sb = new StringBuilder("");
        if (m != null) {
            for (int i = 0; i < m.length; i++) {
                sb.append(m[i].getName());
                sb.append(":");
                sb.append(m[i].getEmail());
                if (i != m.length - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
}
