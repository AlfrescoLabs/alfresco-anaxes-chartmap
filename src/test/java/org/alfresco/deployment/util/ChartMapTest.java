package org.alfresco.deployment.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.fail;

public class ChartMapTest {

    private static Path testOutputPumlFilePathRV = Paths.get("target/test/testChartFileRV.puml");
    private static Path testOutputPumlFilePathNRV = Paths.get("target/test/testChartFileNRV.puml");
    private static Path testOutputPumlFilePathRNV = Paths.get("target/test/testChartFileRNV.puml");
    private static Path testOutputPumlFilePathNRNV = Paths.get("target/test/testChartFileNRNV.puml");
    private static Path testOutputTextFilePathRV = Paths.get("target/test/testChartFileRV.txt");
    private static Path testOutputTextFilePathNRV = Paths.get("target/test/testChartFileNRV.txt");
    private static Path testOutputTextFilePathRNV = Paths.get("target/test/testChartFileRNV.txt");
    private static Path testOutputTextFilePathNRNV = Paths.get("target/test/testChartFileNRNV.txt");
    private static Path testOutputImageRV = Paths.get("target/test/testChartFileRV.png");
    private static Path testOutputImageNRV = Paths.get("target/test/testChartFileNRV.png");
    private static Path testOutputImageRNV = Paths.get("target/test/testChartFileRNV.png");
    private static Path testOutputImageNRNV = Paths.get("target/test/testChartFileNRNV.png");
    private static Path testInputFilePath = Paths.get("src/test/resource/testChartFile.tgz");

    @Test
    public void printTestPumlChartRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputPumlFilePathRV,
                    true, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputPumlFilePathRV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartNoRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputPumlFilePathNRV,
                    false, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputPumlFilePathNRV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputPumlFilePathRNV,
                    true, false);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputPumlFilePathRNV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }


    @Test
    public void printTestPumlChartNoRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputPumlFilePathNRNV,
                    false, false);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputPumlFilePathNRNV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputTextFilePathRV,
                    true, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputTextFilePathRV));
        } catch (Exception e) {
            fail("printTestTextChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartNoRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputTextFilePathNRV,
                    false, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputTextFilePathNRV));
        } catch (Exception e) {
            fail("printTestTextChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputTextFilePathRNV,
                    false, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputTextFilePathRNV));
        } catch (Exception e) {
            fail("printTestTextChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartNRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME,testInputFilePath,testOutputTextFilePathNRNV,
                    false, true);
            testMap.print();
            Assert.assertTrue(Files.exists(testOutputTextFilePathNRNV));
        } catch (Exception e) {
            fail("printTestTextChartNRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @AfterClass
    public static void cleanUp() {
        /**
         * No cleanup to do after test.  I don't delete the generated files
         * because they might be handy to have around to diagnose issues in
         * test failures.   They are deleted anyway when the test is next run.
         */
        System.out.println("Test complete.  Any generated file can be found in " +
                testOutputPumlFilePathRV.getParent().toAbsolutePath().toString());
    }

    @BeforeClass
    public static void setUp() {
        try {
            if (!Files.exists(testInputFilePath)) {
                throw new Exception("test Input File " + testInputFilePath.toAbsolutePath() + " does not exist");
            }
            deleteCreatedFiles();
            Files.createDirectories(testOutputPumlFilePathRV.getParent());
        } catch (Exception e) {
            System.out.println("Test setup failed: " + e.getMessage());
        }
    }

    private static void deleteCreatedFiles() {
        try {
            Files.deleteIfExists(testOutputPumlFilePathRV);
            Files.deleteIfExists(testOutputPumlFilePathNRV);
            Files.deleteIfExists(testOutputPumlFilePathRNV);
            Files.deleteIfExists(testOutputPumlFilePathNRNV);
            Files.deleteIfExists(testOutputTextFilePathRV);
            Files.deleteIfExists(testOutputTextFilePathNRV);
            Files.deleteIfExists(testOutputTextFilePathRNV);
            Files.deleteIfExists(testOutputTextFilePathNRNV);

            Files.deleteIfExists(testOutputImageRV);
            Files.deleteIfExists(testOutputImageNRV);
            Files.deleteIfExists(testOutputImageRNV);
            Files.deleteIfExists(testOutputImageNRNV);
            Files.deleteIfExists(testOutputPumlFilePathRV.getParent());
        } catch (IOException e) {
            System.out.println("Error deleting created files: " + e.getMessage());
        }
    }

    private ChartMap createTestMap(ChartOption option, Path inputPath, Path outputPath,
                               boolean refresh, boolean verbose) throws Exception {
        ChartMap testMap = new ChartMap(
                option,
                inputPath.toAbsolutePath().toString(),
                outputPath.toAbsolutePath().toString(),
                System.getenv("HELM_HOME"),
                refresh,
                verbose);
        return testMap;
    }
}