package org.alfresco.deployment.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;

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

    String formatString(String v) {
        if (v == null || v.trim().isEmpty() ) {
            return "Not specified";
        }
        else {
            return v;
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
        writeLine("\tapiVersion: " + formatString(chart.getApiVersion()));
        writeLine("\tappVersion: " + formatString(chart.getAppVersion()));
        writeLine("\tcreated: " + chart.getCreated());
        writeLine("\tdependencies: " + formatDependencies(chart.getDependencies()));
        writeLine("\tdescription: " + formatString(chart.getDescription()));
        writeLine("\tdigest: " + formatString(chart.getDigest()));
        writeLine("\ticon: " + formatString(chart.getIcon()));
        writeLine("\tkeywords: " + formatArray(chart.getKeywords()));
        writeLine("\tmaintainers: " + formatMaintainers(chart.getMaintainers()));
        if (chart.getRepoUrl() != null) {
            writeLine("\trepo url: " + chart.getRepoUrl());
        }
        writeLine("\tname: " + formatString(chart.getName()));
        writeLine("\tsources: " + formatArray(chart.getSources()));
        writeLine("\turls: " + formatArray(chart.getUrls()));
        writeLine("\tversion: " + formatString(chart.getVersion()));
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
    private String formatArray(String[] a) {
        StringBuilder sb = new StringBuilder("");
        if (a !=null) {
            for (int i = 0; i < a.length; i++) {
                sb.append(a[i]);
                if (i != a.length - 1) {
                    sb.append(",");
                }
            }
        }
        else {
            sb = sb.append("Not specified");
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
    private String formatMaintainers(HelmMaintainer[] m) {
        StringBuilder sb = new StringBuilder("");
        if (m != null) {
            for (int i = 0; i < m.length; i++) {
                sb.append(m[i].getName());
                String email = m[i].getEmail();
                if (email != null) {
                    sb.append(":");
                    sb.append(email);
                }
                if (i != m.length - 1) {
                    sb.append(",");
                }
            }
        }
        else {
            sb = sb.append(("Not specified"));
        }
        return sb.toString();
    }

    private String formatDependencies(HashSet<HelmChart> d) {
        StringBuilder sb = new StringBuilder("");
        if (d.size() == 0) {
            sb.append("None");
        } else {
            boolean first = true;
            Iterator<HelmChart> i = d.iterator();
            while (i.hasNext()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(i.next().getNameFull());
                first = false;
            }
            return sb.toString();
        }
        return sb.toString();
    }
}
