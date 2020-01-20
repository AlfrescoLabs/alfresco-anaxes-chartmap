package org.alfresco.deployment.util;

import org.alfresco.deployment.util.model.HelmChart;
import org.alfresco.deployment.util.model.HelmMaintainer;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A class that generates a JSON file describing a Kubernetes
 * Helm Chart and its dependencies.
 *
 */
public class JSONChartMapPrinter extends ChartMapPrinter {

    /**
     * Constructor
     *
     * @param   outputFilename  the name of the PlantUML file to be created.
     * @param   charts          a multi-key map of all the Helm Charts that
     *                          might be referenced.  The map is keyed by
     *                          Chart Name and Chart Version.
     * @param   chart           a Helm Chart to be printed in PlantUML format
     */
    public JSONChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(outputFilename, charts, chart);
     }

    /**
     * For JSON, there is no header
     *
     * @throws IOException      IOException
     */
    public void printHeader() throws IOException {
    }

    /**
     * For JSON, there is no footer
     *
     * @throws IOException      IOException
     */
    public void printFooter() throws IOException {
        // NOP
    }

    /**
     * Prints a JSON representation of the chart including
     * all dependent charts and images
     *
     * @param   c   a Helm chart
     * @throws  IOException     IOException
     */
    public void printTree(HelmChart c) throws IOException {
        // create a root JSON object to get started
        JSONObject j = new JSONObject();  // root object
        addChartToObject(c, j);
        printObject(j);
    }

    /**
     * A recursive method to adds a chart or image to the tree
     *
     * @param   h   a Helm chart or Docker image to add to the tee
     * @param   j   a JSONObject to which the properties will
     *              be written
     * @throws  IOException     IOException
     */
    public void addChartToObject(HelmChart h, JSONObject j) {
        addProperties(h, j);
        JSONArray a = new JSONArray(); // array of children
        addContainers(h, a);
        Iterator<HelmChart> itr = h.getDependencies().iterator();
        while(itr.hasNext()){
            JSONObject c = new JSONObject(); // new child object
            addChartToObject(itr.next(),c);
            a.put(c);  // add new child to array
        }
        j.put("children", a);  // add the array to the object
    }

    /**
     * Adds the properties of a chart or image to a JSON Object
     *
     * @param   h   a Helm Chart
     * @param   j   a JSONObject to which the properties will
     *              be written
     * @throws  IOException     IOException
     */
    private void addProperties(HelmChart h, JSONObject j) {
        j.put("name",h.getNameFull());
        j.put("type","chart");
        j.put("shortName", h.getName());
        j.put("version", h.getVersion());
        j.put("description", h.getDescription());
        j.put("maintainers", h.getMaintainers());
        j.put("keywords", h.getKeywords());
    }

    /**
     * Adds the dependent containers to an array
     *
     * @param   h   a Helm Chart
     * @param   a   a JSONArray to which the container
     *              will be added
     * @throws  IOException     IOException
     */
    private void addContainers(HelmChart h, JSONArray a) {
        HashSet<String> c = h.getContainers();
        for (String s : h.getContainers()) {
            addContainer(s, a);;
        }
    }

    private void addContainer(String s, JSONArray a) {
        JSONObject c = new JSONObject(); // new child object for the container
        c.put("type","image");
        addImageDetails(s,c);
        c.put("children", new JSONArray());  // add an empty child array (containers have no children)
        a.put(c); // add the container to the parent children array
    }

    /**
     * Writes a JSON object to a file
     *
     * @param   j   a JSONObject to which the properties will
     *              be written
     * @throws  IOException     IOException
     */
    private void printObject(JSONObject j) throws IOException {
        String s = j.toString(indent);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
        writer.write(s);
        writer.close();
    }

    /**
     * @param   i   a string containing the image name which can be in a vaiety
     *           of formats
     * @param   j   a JSONObject to which the properties will
     *              be written
     */
    public void addImageDetails(String i, JSONObject j) {
        //     image: "quay.io/alfresco/service-sync:2.2-SNAPSHOT"
        String repoHost="Docker Hub";
        String imageName = i;
        String version="not specified";
        int count = i.length() - i.replace("/", "").length();
        if (count == 0) { // e.g. postgres:9.6.2
            imageName = i.substring(0,i.indexOf(':'));
        }
        else if (count == 1) { // e.g. : alfresco/process-services:1.8.0
            imageName = i.substring(0,i.indexOf(':'));
        } else { // e.g. quay.io/alfresco/service:1.0.0
            repoHost = i.substring(0,i.indexOf('/'));
            imageName = i.substring(i.indexOf('/')+1, i.indexOf(':'));
        }
        if (i.contains(":")) {
            version = i.substring(i.indexOf(':')+1,i.length());
        }
        j.put("name", imageName);
        j.put("repoHost", repoHost);
        j.put("version", version);
    }

    /**
     * Writes a section header.  Not relevant for JSON
     *
     * @param   header the header to be written
     * @throws  IOException     IOException
     */
    public void printSectionHeader(String header) throws IOException {
        // NOP
    }

}
