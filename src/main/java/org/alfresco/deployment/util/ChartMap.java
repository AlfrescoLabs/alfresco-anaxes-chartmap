
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Map;

import org.alfresco.deployment.util.model.*;

public class ChartMap {

    private HelmChart chart;
    private MultiKeyMap charts;
    private String chartName;
    private MultiKeyMap chartsReferenced;
    private String helmHome;
    private HelmChartReposLocal localRepos;
    private String outputFilename;
    private PrintFormat printFormat;
    private IChartMapPrinter printer;
    private HelmChartRepo repo;
    private String tempDirName;
    private boolean verbose;

    public static void main(String[] arg) {
        ChartMap chartMap = new ChartMap();
        chartMap.parseArgs(arg);
        chartMap.print();
        return;
    }

    public ChartMap(String chartName, String outputFilename, String helmHome, boolean verbose) {
        initialize();
        this.setChartName(chartName);
        this.setOutputFilename(outputFilename);
        this.setHelmHome(helmHome);
        this.setVerbose(verbose);
    }

    public void print() {
        createTempDir();
        loadLocalRepos();
        resolveChartDependencies();
        printMap();
        removeTempDir();
    }

    public ChartMap() {
        initialize();
    }

    private void initialize() {
        chartName = null;
        outputFilename = getDefaultOutputFilename();
        verbose = false;
        helmHome = getDefaultHelmHome();
        tempDirName = null;
        charts = new MultiKeyMap();
        chartsReferenced = new MultiKeyMap();
        repo = new HelmChartRepo();
        printer = null;
        printFormat = PrintFormat.TEXT;
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
    }

    public String getChartName() {
        return chartName;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public void setPrinter(IChartMapPrinter printer) {
        this.printer = printer;
    }

    public IChartMapPrinter getPrinter() {
        return printer;
    }

    public void setHelmHome(String helmHome) {
        this.helmHome = helmHome;
    }

    public String getHelmHome() {
        return helmHome;
    }

    public String getTempDirName() {
        return tempDirName;
    }

    private void setTempDirName (String tempDirName) { this.tempDirName = tempDirName; } // keep private since this directory gets recursively removed

    private void parseArgs(String[] args) {
        Options options = new Options();
        options.addOption("c", true, "The Chart Name");
        options.addOption("o", true, "The Output Filename");
        options.addOption("v", false, "Verbose");
        options.addOption("d", true, "Directory for Helm Home");
        options.addOption("h", false, "Help");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("c")) {
                setChartName(cmd.getOptionValue("c"));
            }
            if (cmd.hasOption("d")) {
                setHelmHome(cmd.getOptionValue("d"));
            }
            if (cmd.hasOption("o")) {
                setOutputFilename(cmd.getOptionValue("o"));
            }
            if (cmd.hasOption("v")) {
                setVerbose(true);
            }
            if (args.length == 0 || cmd.hasOption("h") || getChartName() == null || getOutputFilename() == null || getHelmHome() == null) {
                System.out.println(ChartMap.getHelp());
                System.exit(0);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static String getHelp() {
        String help = "Usage:\n";
        help += "\tjava ChartMap -c <name of the chart> -d <helm home directory> -o <output file name> -v (verbose) -h (help)\n";
        help += "\tNote:\tUse an output file extension of 'puml' to generate a map in PlantUML format.  Otherwise a map in plain text fill be generated.";
        help += "\nExample:\n\tjava -jar chartmap-1.0-SNAPSHOT.jar -c \"alfresco-content-services:0.0.1\" -d \"/Users/myself/.helm\" -o  alfresco-content-services.puml -v";
        return help;
    }

    private void loadLocalRepos() {
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
        }
    }

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

    private void loadLocalCharts() {
        HelmChartRepoLocal[] repos = localRepos.getRepositories();
        for (HelmChartRepoLocal r : repos) {
            File cache = new File(r.getCache());
            loadChartsFromCache(cache);
        }
    }

    private void loadChartsFromCache(File c) {
        HelmChartLocalCache cache = null;
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

    private void printCharts(MultiKeyMap charts) {
        MapIterator it = charts.mapIterator();
        try {
            printer.printComment("There are " + charts.size() + " referenced Helm Charts");
            while (it.hasNext()) {
                it.next();
                printer.printChart((HelmChart) it.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error printing charts: " + e.getMessage());
        }
    }

    private void resolveChartDependencies() {
        String[] chartKeyParts = chartName.split(":");
        chart = (HelmChart) charts.get(chartKeyParts[0], chartKeyParts[1]);
        if (chart != null) {
            downloadChart(chart);
        } else {
            System.out.println("Chart " + chartName + " not found");
        }
    }

    private void downloadChart(HelmChart h) {
        String filePath = null;
        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(h.getUrls()[0]);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            int rc = response.getStatusLine().getStatusCode();
            filePath = tempDirName + h.getName() + "-" + h.getVersion() + ".tgz";
            InputStream is = entity.getContent();
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            int b;
            while ((b = is.read()) != -1) {
                fos.write(b);
            }
            is.close();
            fos.close();
            client.close();
            unpackChart(filePath);
        } catch (java.io.IOException e) {
            System.out.println("Error downloading chart " + filePath + " : " + e.getMessage());
        }
    }

    private void unpackChart(String chartFileName) {  //chartFileName is the file name /temp/foo-a-b-c.tgz
        int bufferSize = 1024;
        String chartDir = null;
        try {
            File in = new File(chartFileName);
            FileInputStream fis = new FileInputStream(chartFileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
            TarArchiveInputStream tis = new TarArchiveInputStream(gis);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                String name = entry.getName();
                chartDir = name.substring(0, name.lastIndexOf(File.separator));
                File p = new File(tempDirName, chartDir);
                p.mkdirs();
                int count;
                byte[] data = new byte[bufferSize];
                FileOutputStream fos = new FileOutputStream(new File(tempDirName, entry.getName()));
                BufferedOutputStream dos = new BufferedOutputStream(fos, bufferSize);
                while ((count = tis.read(data, 0, bufferSize)) != -1) {
                    dos.write(data, 0, count);
                }
                dos.close();
            }
            tis.close();
            bis.close();
            fis.close();
        } catch (Exception e) {
            System.out.println("Exception extracting Chart " + chartFileName + ":" + e.getMessage());
        }
        collectDependencies(tempDirName, null);
    }

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
            for (String s : directories) {
                File chartFile = new File(chartDirName + File.separator + s + File.separator + "Chart.yaml");
                if (h != null) {
                    parentHelmChart = (HelmChart) charts.get(h.getName(), h.getVersion());
                    chartsReferenced.put(parentHelmChart.getName(), parentHelmChart.getVersion(), parentHelmChart);
                    if (parentHelmChart.getDependencies() == null) {
                        parentHelmChart.setDependencies(new ArrayList<HelmChart>());
                    }
                }
                if (chartFile.exists()) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    HelmChart currentHelmChartFromDisk = mapper.readValue(chartFile, HelmChart.class);   // this reference is not in the map
                    HelmChart currentHelmChart = (HelmChart) charts.get(currentHelmChartFromDisk.getName(),currentHelmChartFromDisk.getVersion());
                    if (parentHelmChart != null) {
                        parentHelmChart.getDependencies().add(currentHelmChart);   // add this chart as a dependent
                        chartsReferenced.put(currentHelmChart.getName(), currentHelmChart.getVersion(), currentHelmChart);
                    }
                    File chartsDirectory = new File(chartDirName + File.separator + s + File.separator + "charts");
                    if (chartsDirectory.exists()) {
                        collectDependencies(chartsDirectory.getAbsolutePath(), currentHelmChart);  // recursion
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception getting Dependencies: " + e.getMessage());
        }
    }

    private void printMap() {
        try {
            if (chart != null) {
                detectPrintFormat(outputFilename);
                if (printFormat.equals(PrintFormat.PLANTUML)) {
                    printer = new PlantUmlChartMapPrinter(outputFilename, charts, chart);
                } else {
                    printer = new TextChartMapPrinter(outputFilename, charts, chart);
                }
                printer.printHeader();
                printCharts(chartsReferenced);
                printDependencies(chart);
                printer.printFooter();
                System.out.println("File " + outputFilename + " generated");
            }
        } catch (IOException e) {
            System.out.println("Exception printing Map : " + e.getMessage());
        }
    }

    private void printDependencies(HelmChart parent) {
        try {
            if (parent.getDependencies() != null) {
                for (HelmChart dependent : parent.getDependencies()) {
                    printer.printDependency(parent, dependent);
                    printDependencies(dependent);   // recursion
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error printing dependencies: " + e.getMessage());
        }
    }

    private void detectPrintFormat(String fileName) {
        if (fileName.contains(".") && fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length()).equals("puml")) {
            printFormat = PrintFormat.PLANTUML;
        } else {
            printFormat = PrintFormat.TEXT;
        }
    }

    private void createTempDir() {
        try {
            Path p = Files.createTempDirectory(this.getClass().getCanonicalName()+".");
            setTempDirName(p.toAbsolutePath().toString() + File.separator);
            if (isVerbose()) {
                System.out.println("Temporary Directory " + getTempDirName() + " will be used");
            }
        } catch (java.io.IOException e) {
            System.out.println("Error creating temp directory: " + e.getMessage());
        }
    }

    private void removeTempDir() {
        //System.out.println("Remove temp dir " + getTempDirName());
        Path directory = Paths.get(getTempDirName());
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    //System.out.println("Remove file "+ file.toAbsolutePath());
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    //System.out.println("Remove directory "+ dir.toAbsolutePath());
                    return FileVisitResult.CONTINUE;
                }
            });
            if (isVerbose()) {
                System.out.println("Temporary Directory " + getTempDirName() + " removed");
            }
        } catch (IOException e) {
            System.out.println("Error <" + e.getMessage() + "> removing temporary directory " + getTempDirName());
        }
    }

    private String getDefaultHelmHome () {
        return  System.getenv("HELM_HOME");
    }

    private String getDefaultOutputFilename () {
        return "chartmap.puml";
    }
}