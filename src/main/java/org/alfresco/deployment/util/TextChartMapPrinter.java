package org.alfresco.deployment.util;

import org.apache.commons.collections4.map.MultiKeyMap;

import org.alfresco.deployment.util.model.HelmChart;


// TODO add some real tests

public class TextChartMapPrinter extends ChartMapPrinter {

    public TextChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(outputFilename, charts, chart);
    }
}
