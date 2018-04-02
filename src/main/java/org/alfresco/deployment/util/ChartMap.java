
package org.alfresco.deployment.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardCopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.yaml.snakeyaml.Yaml;
import org.alfresco.deployment.util.model.*;

public class ChartMap {

    private String apprSpec;
    private HelmChart chart;
    private String chartFileName;
    private MultiKeyMap charts;
    private String chartName;
    private String chartVersion;
    private String chartUrl;
    private MultiKeyMap chartsReferenced;
    private HashMap<String, HelmDeploymentContainer> containersReferenced;
    private HashMap<String, WeightedDeploymentTemplate> deploymentTemplatesReferenced;
    private String helmHome;
    private HelmChartReposLocal localRepos;
    private String outputFilename;
    private PrintFormat printFormat;
    private IChartMapPrinter printer;
    private boolean refreshLocalRepo;
    private String tempDirName;
    private boolean verbose;
    final private String RENDERED_TEMPLATE_FILE = "_renderedtemplates.yaml"; // this is the suffix of the name of the file we use to hold the rendered templates
    final private int MAX_WEIGHT = 100;

    /**
     * This inner class is used to assign a 'weight' to a template based on its
     * position in the file system (parent templates having the lower weight).
     * A template of the lowest weight is used to determine which containers will
     * be referenced.
     *
     */
    private class WeightedDeploymentTemplate {
        private int weight;
        private HelmDeploymentTemplate template;
        private ArrayList<HelmDeploymentTemplate> affectedTemplates=new ArrayList<>();
        WeightedDeploymentTemplate(String fileName, HelmDeploymentTemplate t) {
            weight=MAX_WEIGHT;
            if (fileName != null) {
                String[] segments = fileName.split(File.separator);
                if (weight > 0) {
                    weight = segments.length;
                }
            }
            template = t;
        }
        private int getWeight() {
            return weight;
        }
        private void setTemplate(HelmDeploymentTemplate t) {template = t;}
        private HelmDeploymentTemplate getTemplate() { return template;}
    }
    /**
     * Parses the command line and generates a Chart Map file
     *
     * @param   arg   The command line args
     * @throws  IOException
     */
    public static void main(String[] arg) {
        ChartMap chartMap = new ChartMap();
        try {
            chartMap.parseArgs(arg);
            chartMap.print();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    /**
     * Constructor
     *
     * @param option            The format of the Helm Chart
     * @param chart             The name of the Helm Chart in one of the formats specified by
     *                          the option parameter
     * @param outputFilename    The name of the file to which to write the generated Chart Map.
     *                          Note the file is overwritten if it exists.
     * @param helmHome          The location of the user helm directory.  This is needed to find
     *                          the local cache of index files downloaded from the Helm Chart repos.
     * @param refresh           When true, refresh the local Helm repo
     * @param verbose           When true, provides a little more information as the Chart Map is
     *                          generated
     *
     **/

    public ChartMap(ChartOption option,
                    String chart,
                    String outputFilename,
                    String helmHome,
                    boolean refresh,
                    boolean verbose) throws Exception {
        initialize();
        ArrayList <String> args = new ArrayList<>();
        if (option.equals(ChartOption.APPRSPEC)) {
           args.add("-a");
        }
        else if (option.equals(ChartOption.CHARTNAME)) {
            args.add("-c");
        }
        else if (option.equals(ChartOption.FILENAME)) {
            args.add("-f");;
        }
        else if (option.equals(ChartOption.URL)) {
            args.add("-u");
        }
        else {
            throw new Exception ("Invalid Option Specification");
        }
        args.add(chart);
        if (refresh) {
            args.add("-r");
        }
        if (verbose) {
            args.add("-v");
        }
        args.add("-o");
        args.add(outputFilename);
        args.add("-d");
        args.add(helmHome);
        parseArgs(args.toArray(new String[args.size()]));
    }


    /**
     * Prints the Chart Map by creating a temp directory, loading the local
     * repo with charts, resolving the dependencies of the selected chart,
     * printing the Chart Map, then cleans up
     */
    public void print() throws IOException {
        createTempDir();
        loadLocalRepos();
        resolveChartDependencies();
        printMap();
        removeTempDir();
    }

    private ChartMap() {
        initialize();
    }

    /**
     * Initializes the instance variables
     *
     */
    private void initialize() {
        setChartName(null);
        setOutputFilename(getDefaultOutputFilename());
        setChartFileName(null);
        setChartUrl(null);
        setVerbose(false);
        setHelmHome(getDefaultHelmHome());
        setTempDirName(null);
        setPrintFormat(PrintFormat.TEXT);
        setRefreshLocalRepo(false);
        charts = new MultiKeyMap();
        chartsReferenced = new MultiKeyMap();
        containersReferenced = new HashMap<>();
        deploymentTemplatesReferenced = new HashMap<>();
    }

    /**
     * Parse the command line args
     *
     * @param args  command line args
     */
    private void parseArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("a", true, "The appr chart location");
        options.addOption("c", true, "The Chart Name");
        options.addOption("d", true, "Directory for Helm Home");
        options.addOption("f", true, "Helm Chart File Name");
        options.addOption("h", false, "Help");
        options.addOption("o", true, "The Output Filename");
        options.addOption("r", false, "Update the Helm Chart dependencies");
        options.addOption("u", true, "The Url of the Helm Chart ");
        options.addOption("v", false, "Verbose");
        CommandLineParser parser = new DefaultParser();
        int count=0;
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("a")) { // e.g. quay.io/alfresco/alfresco-dbp@0.2.0
                if (parseApprSpec(cmd.getOptionValue("a"))) {
                    count++;
                }
            }
            if (cmd.hasOption("c")) { // e.g. alfresco-dbp:0.2.0
                if (parseChartName(cmd.getOptionValue("c"))) {
                    count++;
                }
            }
            if (cmd.hasOption("f")) { // e.g. /Users/johndoe/alfresco-content-services-0.0.1.tgz
                setChartFileName(cmd.getOptionValue("f"));
                count++;
            }
            if (cmd.hasOption("u")) { // e.g. https://alfresco.github.io/charts/incubator/alfresco-content-services-0.0.1.tgz
                setChartUrl(cmd.getOptionValue("u"));
                count++;
            }
            if (cmd.hasOption("d")) {
                setHelmHome(cmd.getOptionValue("d"));
            }
            if (cmd.hasOption("o")) {
                setOutputFilename(cmd.getOptionValue("o"));
            }
            if (cmd.hasOption("r")) {
                setRefreshLocalRepo(true);
            }
            if (cmd.hasOption("v")) {
                setVerbose(true);
            }
            if (args.length == 0
                    || cmd.hasOption("h")
                    || count != 1 ) {
                System.out.println(ChartMap.getHelp());
                System.exit(0);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw (e);
        }
    }
    /**
     *
     * Parses a Appr Specification of the format <chart-repp>/<org>/<chart-name>@<chart-version>
     * and sets the values chartName and chartVersion
     *
     * @param   a the Appr Specification
     * @return  true if a valid Appr Specification was passed
     */
    private boolean parseApprSpec(String a) {
        String[] apprSpecParts = a.split("@");
        if (apprSpecParts.length == 2) {
            setChartName(apprSpecParts[0].substring(apprSpecParts[0].lastIndexOf('/') + 1, apprSpecParts[0].length()));
            setChartVersion(apprSpecParts[1]);
            return true;
        }
        return false;
    }

    /**
     *
     * Parses a Chart Name of the format <chart-name><chart version> and sets the values of
     * chartName and chartVersion
     *
     * @param   c the Chart Name
     * @return  true if a valid Chart Name was passed
     */
    private boolean parseChartName(String c) {
        // e.g. content-services:0.0.1
        String[] chartNameParts = c.split(":");
        if (chartNameParts.length == 2) {
            setChartName(chartNameParts[0]);
            setChartVersion(chartNameParts[1]);
            return true;
        }
        return false;
    }

    /**
     * Prints some help
     *
     * @return      a string containing some help
     */
    private static String getHelp() {
        String help = "Usage:\n";
        help += "\tjava ChartMap {-a <appr location> | -c <name of the chart> | -f <chart filename> | -u <chart url>} -d <helm home directory> -o <output file name> -r (update dependencies) -v (verbose) -h (help)\n";
        help += "\tNote 1:\tUse an output file extension of 'puml' to generate a map in PlantUML format.  Otherwise a map in plain text will be generated.\n";
        help += "\nExample:\n\tjava -jar chartmap-1.0-SNAPSHOT.jar -c \"alfresco-content-services:0.0.1\" -d \"/Users/myself/.helm\" -o  alfresco-content-services.puml -v";
        return help;
    }

    /**
     *
     * Finds all the local repositories the user has previously added (using for example
     * 'helm repo add') and constructs a HelmChartReposLocal from them.   Then calls a method
     * to load all the charts in that repo into the charts map where they are raw material for
     * being referenced later
     *
     */
    private void loadLocalRepos() throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            File reposYamlFile = new File(helmHome + "/repository/repositories.yaml");
            localRepos = mapper.readValue(reposYamlFile, HelmChartReposLocal.class);
            if (isVerbose()) {
                printLocalRepos();
            }
            loadLocalCharts();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw (e);
        }
    }

    /**
     *
     * Prints a summary of some local repo information, if the user wants verbosity
     *
     */
    private void printLocalRepos() {
        if (isVerbose()) {
            HelmChartRepoLocal[] repos = localRepos.getRepositories();
            System.out.println("Api Version: " + localRepos.getApiVersion());
            System.out.println("Generated: " + localRepos.getGenerated());
            System.out.println("Number of Repos: " + localRepos.getRepositories().length);
            for (HelmChartRepoLocal r : repos) {
                System.out.println("\tName: " + r.getName());
                System.out.println("\tCache: " + r.getCache());
                System.out.println("\tUrl: " + r.getUrl());
            }
        }
    }

    /**
     * For each of the local repos the user has previously added (using for example
     * 'helm repo add') calls the function to load the charts
     */
    private void loadLocalCharts() {
        HelmChartRepoLocal[] repos = localRepos.getRepositories();
        for (HelmChartRepoLocal r : repos) {
            File cache = new File(r.getCache());
            loadChartsFromCache(cache);
        }
    }


    /**
     * Takes a directory containing files in yaml form and constructs a HelmChart object from
     * each and adds that Helm Chart object to the charts map
     *
     * @param c     a Directory containing Helm Charts in yaml form
     */
    private void loadChartsFromCache(File c) {
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(c, HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            for (Map.Entry<String, HelmChart[]> entry : entries.entrySet()) {
                for (HelmChart h : entry.getValue()) {
                    charts.put(h.getName(), h.getVersion(), h);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * For each chart in the referenced charts map, print the chart.  Precede that with a summary
     * of how many charts were referenced
     *
     */
    private void printCharts() {
        MapIterator it = chartsReferenced.mapIterator();
        try {
            printer.printComment("There are " + chartsReferenced.size() + " referenced Helm Charts");
            while (it.hasNext()) {
                it.next();
                printer.printChart((HelmChart) it.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error printing charts: " + e.getMessage());
        }
    }

    /**
     * Resolves a charts dependencies by getting the chart and then finding the charts dependencies.
     *
     */
    private void resolveChartDependencies() {
        String chartDirName = getChart();
        if (chart != null) {
            collectDependencies(chartDirName, null);
            applyTemplates();
        } else {
            System.out.println("Chart " + chartName + " not found");
        }
    }

    /**
     * Gets a chart from a Helm repo in one of four ways ...
     *
     * 1.  If the user specified an appr spec, pull the chart using the helm command line
     * 2.  If the user specified the url of a chart package (a tgz file), download the file using http and unpack it
     * 3.  If the user specified the name of a local tgz file, there is no need to fetch a chart
     * 4.  If the user specified the chart by name, the chart is already in the charts map we create from the repo so find the download url from that entry and download it
     */
    private String getChart() {
        String chartDirName = "";
        try {
            if (getApprSpec() != null) {
                chartDirName = pullChart(getApprSpec());
            } else if (getChartUrl() != null) {
                chartDirName = downloadChart(getChartUrl());
            } else if (getChartFileName() != null) {
                try {
                    Path src = new File(getChartFileName()).toPath();
                    Path tgt = new File(getTempDirName()).toPath().resolve(new File(getChartFileName()).getName());
                    Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING);
                    String s = tgt.toAbsolutePath().toString();
                    chartDirName = unpackChart(s);
                } catch (IOException e) {
                    System.out.println("Exception copying " + getChartFileName() + " to " + getTempDirName() + " : " + e.getMessage());
                }
            } else {
                HelmChart h = (HelmChart) charts.get(chartName, chartVersion);
                if (h != null) {
                    chartDirName = downloadChart(h.getUrls()[0]);
                }
            }
            updateLocalRepo(chartDirName);
        } catch (Exception e) {
            System.out.println("Error getting chart: " + e.getMessage());
        }
        chart = (HelmChart) charts.get(chartName, chartVersion);
        return chartDirName.substring(0, chartDirName.lastIndexOf(File.separator)); // return the parent directory
    }

    /**
     * Downloads a chart using appr into the temp directory
     *
     * @param apprSpec    a string specifying the location of the chart
     * @return            the name of the directory where the chart was downloaded into
     *                    e.g. /temp/alfresco_alfresco-dbp_0.2.0/alfresco-dbp
     */
    private String pullChart (String apprSpec) {
        String chartDirName = null;
        try {
            // the chart name should be of the form <repo>/<org>/<chartname>@<version> e.g. quay.io/alfresco/alfresco-dbp@0.2.0
            if (apprSpec == null || (apprSpec.indexOf('/')==-1) || (apprSpec.indexOf('@')==-1)) {
                throw new Exception("appr specification invalid: " + apprSpec + " .  I was expecting something like quay.io/alfresco/alfresco-dbp@0.2.0");
            }
            String command = "helm registry pull ";
            command += apprSpec + " -t helm ";
            Process p = Runtime.getRuntime().exec(command, null, new File(getTempDirName()));
            p.waitFor(10000, TimeUnit.MILLISECONDS);
            int exitCode = p.exitValue();
            if (exitCode == 0) {
                chartDirName = getTempDirName() + apprSpec.substring(apprSpec.indexOf('/') + 1, apprSpec.length()).replace('@', '_').replace('/', '_') + File.separator + chartName;
                createChart(chartDirName);
                unpackEmbeddedCharts(chartDirName);
            }
            else {
                throw new Exception("Error Code: " + exitCode + " executing command \"" + command + "\"");
            }
            //updateLocalRepo(chartDirName);
        } catch (Exception e) {
            System.out.println("Exception pulling chart from appr using specification " + apprSpec + " : " + e.getMessage());
        }
        return chartDirName;
    }

    /**
     * Downloads a Helm Chart from a Helm Chart repository to a
     * a tgz file on disk.   Unpacks it and creates an entry for the chart in the
     * local charts map
     *
     * @param u         A string holding the url of the Helm Chart to be downloaded
     * @return          the name of the directory where the chart was pulled into
     *                  e.g. /temp/alfresco_alfresco-dbp_0.2.0/alfresco-dbp
     */

    private String downloadChart(String u) {
        String chartDirName = null;
        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(u);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            int rc = response.getStatusLine().getStatusCode();
            String tgzFileName = tempDirName + this.getClass().getCanonicalName() + "_chart.tgz";
            InputStream is = entity.getContent();
            FileOutputStream fos = new FileOutputStream(new File(tgzFileName));
            int b;
            while ((b = is.read()) != -1) {
                fos.write(b);
            }
            is.close();
            fos.close();
            client.close();
            chartDirName = unpackChart(tgzFileName);
            createChart(chartDirName);
            //updateLocalRepo(chartDirName);
        } catch (Exception e) {
            System.out.println("Error downloading chart " + chartDirName + " : " + e.getMessage());
        }
        return chartDirName;
    }

    /**
     * Updates the local chart cache using the Helm client.   This is only done
     * if the user has specified the refresh parameter on the command line or method call
     *
     * @param dirName   The name of the directory containing the chart
     * @throws Exception
     */
    private void updateLocalRepo(String dirName) throws Exception {
        // if the user wants us to update the Helm dependencies, do so
        if (this.isRefreshLocalRepo()) {
            String command = "helm dep update";
            Process p = Runtime.getRuntime().exec(command, null, new File(dirName));
            p.waitFor(10000, TimeUnit.MILLISECONDS);
            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new Exception("Exception updating chart repo.  Exit code: " +
                        exitCode + ".  Possibly you cannot cannot to one of your remote charts repos.");
            }
            else {
                if (this.isVerbose()) {
                    System.out.println("Updated Helm dependencies");
                }
            }
        }
    }

    /**
     * Creates a chart in the charts map from a Chart.yaml located in the provided directory
     *
     * @param chartDirName  the name of the directory in which the Chart.yaml file is to be found
     */
    private void createChart(String chartDirName) {
        String chartFileName = chartDirName + File.separator + "Chart.yaml";
        try {

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            HelmChart h = mapper.readValue(new File(chartFileName), HelmChart.class);
            chartName = h.getName();
            chartVersion = h.getVersion();
            charts.put(h.getName(), h.getVersion(), h);

        } catch (IOException e) {
            System.out.println("Error extracting Chart information from " + chartFileName);
        }
    }

    /**
     * Unpacks a Helm Chart tgz file.
     *
     * @param       chartFileName     The name of the tgz file containing the chart
     * @return      the name of the directory in which the chart was unpacked
     *              e.g. /temp/alfresco_alfresco-dbp_0.2.0/alfresco-dbp
     */
    private String unpackChart(String chartFileName) {
        int bufferSize = 1024;
        String unpackDirName = null;
        try {
            File in = new File(chartFileName);
            FileInputStream fis = new FileInputStream(chartFileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
            TarArchiveInputStream tis = new TarArchiveInputStream(gis);
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                String chartName = name.substring(0, name.lastIndexOf(File.separator));
                Path dir = new File(chartFileName.substring(0, chartFileName.lastIndexOf(File.separator)), chartName).toPath();
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
                int count;
                byte[] data = new byte[bufferSize];
                String fileName = chartFileName.substring(0, chartFileName.lastIndexOf(File.separator)) + File.separator + entry.getName();
                File file = new File(fileName);
                if (!file.createNewFile()) {
                    throw new Exception("Error creating file: " + file.getAbsolutePath());
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream dos = new BufferedOutputStream(fos, bufferSize);
                while ((count = tis.read(data, 0, bufferSize)) != -1) {
                    dos.write(data, 0, count);
                }
                dos.close();
                unpackDirName = tempDirName + File.separator + chartName;
            }
            tis.close();
            bis.close();
            fis.close();
            unpackEmbeddedCharts(unpackDirName);

            // If the Chart Name or Version were not yet extracted, such as would happen if the chart was provided as a local tgz file
            // then extract the chart name and version from the highest order Chart.yaml file and create the entry in the charts map
            // if it is not already there (such as would happen if the chart was in an appr repo)
            if (getChartName() == null || getChartVersion() == null) {
                String chartFileDir = chartFileName.substring(0, chartFileName.lastIndexOf(File.separator));
                String[] directories = new File(chartFileDir).list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        File file = new File(chartFileDir + File.separator + name);
                        return (file.isDirectory());
                    }
                });
                if (directories != null) {
                    try {
                        String chartYamlFilename = chartFileDir + File.separator + directories[0] + File.separator + "Chart.yaml";
                        File chartYamlFile = new File(chartYamlFilename);
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        HelmChart h = mapper.readValue(chartYamlFile, HelmChart.class);
                        chartName = h.getName();
                        chartVersion = h.getVersion();
                        if (charts.get(chartName, chartVersion) == null) {
                            charts.put(chartName, chartVersion, h);
                        }
                        unpackDirName = tempDirName + chartName;
                    } catch (IOException e) {
                        throw new Exception("Error extracting Chart Name and Chart Version");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception extracting Chart " + chartFileName + ":" + e.getMessage());
        }
        return unpackDirName;
    }

    /**
     * Recursively unpacks any tgz files found in the chart directory
     *
     * @param chartDirName      the name of the directory in which the chart can be found
     */
    private void unpackEmbeddedCharts(String chartDirName) {
        // todo: check recursion
        String[] directories = new File(chartDirName).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File file = new File(chartDirName + File.separator + name);
                return (file.isDirectory() && name.equals("charts"));
            }
        });
        if (directories != null) {
            for (String s : directories) {
                String[] tgzFiles = new File(chartDirName + File.separator + s).list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.endsWith(".tgz")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                if (tgzFiles != null) {
                    for (String t : tgzFiles) {
                        unpackChart(chartDirName + File.separator + "charts" + File.separator + t);   // recursion
                    }
                }
            }
        }
    }

    /**
     * Starting at the directory in chartDirName, recursively discovers all the dependents of
     * the Helm Chart and saves the resulting information in the Helm Charts.  Along the way, it
     * renders templates for the charts.
     *
     * These dependency relationships are later used to create links between the charts in the
     * printed map.
     *
     * @param chartDirName      the name of a directory containing a Helm Chart
     * @param h                 the Helm Chart on which dependencies will be collected
     */
    private void collectDependencies(String chartDirName, HelmChart h) {  // TODO add a test for a dependency cycle
        HelmChart parentHelmChart = null;
        try {
            File currentDirectory = new File(chartDirName);
            String[] directories = currentDirectory.list(new FilenameFilter() {
                @Override
                public boolean accept(File c, String n) {
                    return new File(c, n).isDirectory();
                }
            });
            if (directories != null) {
                for (String s : directories) {
                    if (h != null) {
                        parentHelmChart = (HelmChart) charts.get(h.getName(), h.getVersion());
                        chartsReferenced.put(parentHelmChart.getName(), parentHelmChart.getVersion(), parentHelmChart);
                    }
                    File chartFile = new File(chartDirName + File.separator + s + File.separator + "Chart.yaml");
                    if (chartFile.exists()) {
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        HelmChart currentHelmChartFromDisk = mapper.readValue(chartFile, HelmChart.class);   // this reference is not in the map
                        HelmChart currentHelmChart = (HelmChart) charts.get(currentHelmChartFromDisk.getName(), currentHelmChartFromDisk.getVersion());
                        if (currentHelmChart == null) {
                            // this is most likely because the local Helm charts are out of date and should be refreshed
                            throw new Exception("A dependency on " +
                                    currentHelmChartFromDisk.getName() + ":" + currentHelmChartFromDisk.getVersion() +
                                    " was found but that chart was not found in the local Helm charts cache.\n  " +
                                    " Try running the command again with the '-r' option");

                        }
                        if (parentHelmChart != null) {
                            parentHelmChart.getDependencies().add(currentHelmChart);   // add this chart as a dependent
                            chartsReferenced.put(currentHelmChart.getName(), currentHelmChart.getVersion(), currentHelmChart); // may be redundant given we added parent already in an earlier iteration
                            collectValues(chartDirName + File.separator + s, currentHelmChart);
                        }
                        renderTemplates(currentDirectory, currentHelmChart);
                        File chartsDirectory = new File(chartDirName + File.separator + s + File.separator + "charts");
                        if (chartsDirectory.exists()) {
                            collectDependencies(chartsDirectory.getAbsolutePath(), currentHelmChart);  // recursion
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception getting Dependencies: " + e.getMessage());
        }
    }

    /**
     * For each of the referenced charts, find the template that has the heighest
     * priority (the 'weighted template' which was previously determined from the
     * location of the hierarchy in the file system (parents having priority over
     * children) and set that one in the chart
     *
     */
    private void applyTemplates() {
        MapIterator i = chartsReferenced.mapIterator();
        while (i.hasNext()) {
            i.next();
            HelmChart h = (HelmChart)i.getValue();
            ArrayList<HelmDeploymentTemplate> a = new ArrayList<>();
            for (HelmDeploymentTemplate t : h.getDeploymentTemplates()) {
                // get the template from the weighted templates array
                WeightedDeploymentTemplate w = deploymentTemplatesReferenced.get(t._getFileName());
                if (w!= null) {
                    // and use that instead of what the Chart was using.  Usually they are not different
                    a.add(w.getTemplate());
                }
            }
            h.setDeploymentTemplates(a);
            collectContainers(h);
        }
    }
    /**
     * Collects the values of all the properties found in the Values.yaml file in a directory
     * and attaches the result to a Helm Chart object
     *
     * Note one cannot just load the values file into a known model because it is
     * file that has no model, hence the need for this more generic approach
     *
     * @param dirName       the name of the directory in which the values file exists
     * @param h             the Helm Chart object to which these values apply
     * @throws Exception    Exception
     */
    @SuppressWarnings("unchecked") private void collectValues(String dirName, HelmChart h) throws Exception {
        if (h==null || dirName == null) {return;}
        File valuesFile = new File(dirName + File.separator + "values.yaml");
        if (valuesFile.exists()) {
            FileInputStream fis = new FileInputStream(valuesFile);
            Yaml yaml = new Yaml();
            Object o = yaml.load(fis);
            if (o instanceof Map<?,?>) {
                Map<String, Object> values = (Map<String, Object>) o;
                h.setValues(values);
            }
            else {
                throw new Exception("The values.yaml file:" + valuesFile.getAbsolutePath() + " could not be parsed");
            }
        }
    }

    /**
     * Adds all the containers referenced in the current Helm Chart to the
     * collection of all referenced containers.   This collection is used later
     * to print a list of all the containers.
     *
     * @param h     The current Helm Chart
     */
    private void collectContainers(HelmChart h) {
        ArrayList<HelmDeploymentContainer> containers = h.getContainers();
        for (HelmDeploymentContainer c: containers) {
            containersReferenced.put(c.getImage(), c);
        }
    }

    /**
     *
     * Uses the helm command line to render the templates of a chart.  The
     * resulting rendered template is saved in the templates directory
     * of the chart with the name this.getClass().getCanonicalName()_renderedtemplates.yaml
     *
     * @param dir           The directory in which the chart directory exists
     * @param h             The Helm Chart containing the templates
     */
    private void renderTemplates(File dir, HelmChart h) {
        String command = "helm template " + h.getName(); // todo: note this command collects all the templates recursively
        try {
            Process p = Runtime.getRuntime().exec(command, null, dir);
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            File f = new File(
                    dir.getAbsolutePath() + File.separator + h.getName()
                            + File.separator  + "templates"
                            + File.separator  + this.getClass().getCanonicalName() + RENDERED_TEMPLATE_FILE);
            if (!f.createNewFile()) {
                throw new Exception("File: " + f.getAbsolutePath() + " could not be created.");
            }
            BufferedOutputStream out =
                    new BufferedOutputStream(
                            new FileOutputStream(f));
            byte[] bytes = new byte[16384];
            int len;
            while ((len = in.read(bytes)) > 0) {
                out.write(bytes, 0, len);
            }
            in.close();
            out.close();
            p.waitFor(1000,TimeUnit.MILLISECONDS);

            int exitCode = p.exitValue();
            if (exitCode == 0) {
                ArrayList<Boolean> a = getTemplateArray(f, h.getName());
                ArrayList<String> b = getTemplateArray(dir, f);
                int i = 0;
                Yaml yaml = new Yaml();
                InputStream input = new FileInputStream(f);
                for (Object data : yaml.loadAll(input)) {  // there may multiple yaml documents in this one document
                    // inspect the object to see if it is a deployment template
                    // if it is add to the deploymentTemplates array
                    if (data instanceof Map) { // todo is this needed?   should it not always be a Map?
                        Map m = (Map) data;
                        Object o = m.get("kind");
                        if (o instanceof String) {
                            String v = (String) o;
                            if (v.equals("Deployment")) {
                                String s = yaml.dump(m);
                                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                HelmDeploymentTemplate template = mapper.readValue(s, HelmDeploymentTemplate.class);
                                if (a.get(i)) {
                                    template._setFileName(b.get(i));
                                    h.getDeploymentTemplates().add(template);
                                }
                                // cases:
                                // 1.  The Chart has a dependency on this template and nothing supercedes it in some parent chart
                                // 2.  The Chart has a dependency on this template and a superceding version of this template has already been found
                                // 3.  The Chart has a dependency on this template and a superceding version of this template will be found later
                                WeightedDeploymentTemplate weightedTemplate = deploymentTemplatesReferenced.get(b.get(i));
                                if (weightedTemplate == null) {
                                    weightedTemplate = new WeightedDeploymentTemplate(dir.getAbsolutePath(), template);
                                    deploymentTemplatesReferenced.put(b.get(i), weightedTemplate);
                                } else {
                                    if (weightedTemplate.getWeight() > getWeight(dir.getAbsolutePath())) {
                                        weightedTemplate.setTemplate(template);
                                    }
                                }
                            }
                        }
                        i++; // index to the next element in the array that indicates whether the template is of interest for the current chart level
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception rendering template : " + e.getMessage());
        }
    }

    /**
     * Return a 'weight' of a String, calculated by the number of segments in the String separated by
     * the File separator
     *
     * @param s     a String whose weight is desired
     * @return      the calculated weight
     */
    private int getWeight(String s) {
        int weight=MAX_WEIGHT;
        if (s !=null) {
            String[] segments = s.split(File.separator);
            weight = segments.length;
        }
        return weight;
    }

    /**
     * Creates an array that can be used to filter out the templates from the rendered templates file
     * that don't pertain to the current chart.   Recall that the rendered templates file created in
     * renderTemplates contains all the descendent template files so we need a way to know which of the
     * templates mentioned in that file pertain to the chart at this level so we can draw the right
     * arrows later.
     *
     * @param f             the file containing all the rendered templates
     * @param chartName     the name of the chart we are interested in at the moment
     * @return              an array with True object if the corresponding template is one that
     *                      pertains to the chart
     */
    private ArrayList<Boolean> getTemplateArray(File f, String chartName) {
        ArrayList<Boolean> a = new ArrayList<>();
        String line=null;
        try {
            FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int lineCount = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineCount++;
                if (line.length() > ("# Source: " + chartName).length() && line.charAt(0) == '#') {
                    // a pattern like this  <chartName>/templates/... means that this is
                    // a template of immediate interest to the chart alfresco-content-services/templates
                    String[] s = line.split(File.separator, 3);
                    if (s.length > 1
                            && s[0].equals("# Source: " + chartName)
                            && s[1].equals("templates")
                            && !line.endsWith(RENDERED_TEMPLATE_FILE)) {  // ignore the template file we generate
                        a.add(Boolean.TRUE);
                    } else {
                        a.add(Boolean.FALSE);
                    }
                }
            }
            fileReader.close();
        } catch (Exception e) {
            System.out.println("Exception creating template array in " + f.getName() + " with line " + line);
        }
        return a;
    }

    /**
     * Parses a file containing multiple yaml files and returns a array of the file names
     * of those yaml files
     *
     * @param f     A yaml file containing multiple yaml files, each such file preceded by a comment of the form
     *              "# Source <filename>"
     *              e.g. # Source: alfresco-dbp/charts/alfresco-process-services/charts/postgresql/templates/deployment.yaml
     * @return     an array containing name fully qualified file names of all the deployment templates mentioned in the yaml file
     */
    private ArrayList<String> getTemplateArray(File d, File f) {
        ArrayList<String> a = new ArrayList<>();
        String line=null;
        try {
            FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int lineCount = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineCount++;
                if (line.startsWith("# Source: ")) {
                    String[] s = line.split("# Source: ", line.length());
                    a.add(d.getAbsolutePath() + File.separator + s[1]);
                }
            }
            fileReader.close();
        } catch (Exception e) {
            System.out.println("Exception creating template array in " + f.getName() + " with line " + line);
        }
        return a;
    }

    /**
     * Prints the Chart Map
     */
    private void printMap() throws IOException {
        try {
            if (chart != null) {
                detectPrintFormat(outputFilename);
                if (printFormat.equals(PrintFormat.PLANTUML)) {
                    printer = new PlantUmlChartMapPrinter(outputFilename, charts, chart);
                } else {
                    printer = new TextChartMapPrinter(outputFilename, charts, chart);
                }
                printer.printHeader();
                printCharts();
                printContainers();
                printChartDependencies(chart);
                printContainerDependencies();
                printer.printFooter();
                System.out.println("File " + outputFilename + " generated");
            }
        } catch (IOException e) {
            System.out.println("Exception printing Map : " + e.getMessage());
            throw (e);
        }
    }

    /**
     * Prints the dependencies of a Helm Chart
     *
     * @param parent    the parent helm chart from which recursion starts
     */
    private void printChartDependencies(HelmChart parent) {
        try {
            if (parent.getDependencies() != null) {
                // Print the chart to chart dependencies recursively
                for (HelmChart dependent : parent.getDependencies()) {
                    printer.printChartToChartDependency(parent, dependent);
                    printChartDependencies(dependent);   // recursion
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error printing chart dependencies: " + e.getMessage());
        }
    }

    /**
     *
     * For each of the referenced charts, prints the containers referenced by the
     * chart
     *
     */
    private void printContainerDependencies () {
        MapIterator it = chartsReferenced.mapIterator();
        try {
            while (it.hasNext()) {
                it.next();
                HelmChart h = (HelmChart) it.getValue();
                for (HelmDeploymentContainer container : h.getContainers()) {
                    printer.printChartToContainerDependency(h, container);
                }
            }
        } catch (IOException e) {
            System.out.println("Error printing image dependencies: " + e.getMessage());
        }
    }

    /**
     *
     * Prints all the referenced Containers
     *
     */
    private void printContainers() {
        try {
            printer.printComment("There are " + containersReferenced.size() + " referenced Images");
            for (Map.Entry<String, HelmDeploymentContainer> entry : containersReferenced.entrySet()) {
                printer.printContainer(entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error printing images: " + e.getMessage());
        }
    }

    /**
     *
     * Determines the print format to use based on the file extension
     * @param fileName      the name of the file to which the chart map will be printed
     *
     */
    private void detectPrintFormat(String fileName) {
        if (fileName != null && fileName.endsWith(".puml")) {
            printFormat = PrintFormat.PLANTUML;
        } else {
            printFormat = PrintFormat.TEXT;
        }
     }

    /**
     *
     * Creates a temporary used to download and expand the Helm Chart
     *
     */
    private void createTempDir() throws IOException {
        try {
            Path p = Files.createTempDirectory(this.getClass().getCanonicalName()+".");
            setTempDirName(p.toAbsolutePath().toString() + File.separator);
            if (isVerbose()) {
                System.out.println("Temporary Directory " + getTempDirName() + " will be used");
            }
        } catch (IOException e) {
            System.out.println("Error creating temp directory: " + e.getMessage());
            throw (e);
        }
    }

    /**
     *
     * Removes the temporary directory created by createTempDir()
     *
     */
    private void removeTempDir() throws IOException {
        Path directory = Paths.get(getTempDirName());
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            if (isVerbose()) {
                System.out.println("Temporary Directory " + getTempDirName() + " removed");
            }
        } catch (IOException e) {
            System.out.println("Error <" + e.getMessage() + "> removing temporary directory " + getTempDirName());
            throw (e);

        }
    }

    // Getters and Setters

    private String getDefaultHelmHome () {return  System.getenv("HELM_HOME"); }

    private String getApprSpec() {return apprSpec;}

    private void setApprSpec(String apprSpec) {this.apprSpec = apprSpec;}

    private String getDefaultOutputFilename () {return "chartmap.puml"; }

    private void setChartName(String chartName) {this.chartName = chartName; }

    private String getChartName() {
        return chartName;
    }

    private void setChartVersion(String chartVersion) {this.chartVersion = chartVersion; }

    private String getChartVersion() {
        return chartVersion;
    }

    private String getChartUrl() { return chartUrl;}

    private void setChartUrl(String chartUrl) { this.chartUrl = chartUrl;}

    private boolean isVerbose() {
        return verbose;
    }

    private void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private String getOutputFilename() {
        return outputFilename;
    }

    private void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    private void setPrinter(IChartMapPrinter printer) {this.printer = printer;}

    private IChartMapPrinter getPrinter() {
        return printer;
    }

    private void setHelmHome(String helmHome) {
        this.helmHome = helmHome;
    }

    private String getHelmHome() {
        return helmHome;
    }

    private String getTempDirName() {
        return tempDirName;
    }

    private void setTempDirName (String tempDirName) { this.tempDirName = tempDirName; } // keep private since this directory gets recursively removed

    private boolean isRefreshLocalRepo() {
        return refreshLocalRepo;
    }

    private void setRefreshLocalRepo(boolean refreshLocalRepo) {
        this.refreshLocalRepo = refreshLocalRepo;
    }

    public PrintFormat getPrintFormat() {return printFormat;}

    private void setPrintFormat(PrintFormat printFormat) {this.printFormat = printFormat;}

    private String getChartFileName() {return chartFileName;}

    private void setChartFileName(String chartFileName) {this.chartFileName = chartFileName;}
}