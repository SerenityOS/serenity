/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

import org.testng.annotations.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8230870
 * @summary Test ZIP Filesystem behavior with ~64k entries
 * @modules jdk.zipfs
 * @run testng LargeEntriesTest
 */
public class LargeEntriesTest {

    private static final Path HERE = Path.of(".");

    /**
     * Number of ZIP entries which results in the use of ZIP64
     */
    private static final int ZIP64_ENTRIES = 65535;

    /**
     * Classes and MANIFEST attribute used for invoking Main via java -jar
     */
    private static final String MANIFEST_MAIN_CLASS = "LargeEntriesTest$Main";
    private static final String MAIN_CLASS = "LargeEntriesTest$Main.class";
    private static final String THIS_CLASS = "LargeEntriesTest.class";

    /**
     * Number of entries included in the JAR file including  META-INF,
     * MANIFEST.MF, and the classes associated with this test
     */
    private static final int ADDITIONAL_JAR_ENTRIES = 4;

    /**
     * Value used for creating the required entries in a ZIP or JAR file
     */
    private static final String ZIP_FILE_VALUE = "US Open 2019";
    private static final byte[] ZIP_FILE_ENTRY =
            ZIP_FILE_VALUE.getBytes(StandardCharsets.UTF_8);

    /**
     * Location of the classes to be added to the JAR file
     */
    static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes", "."));

    private static final SecureRandom random = new SecureRandom();

    /**
     * Fields used for timing runs
     */
    private static int testNumberRunning;
    private static long runningTestTime;
    private static long startTestRunTime;
    private static final double NANOS_IN_SECOND = 1_000_000_000.0;

    @BeforeTest(enabled = false)
    public void beforeTest() {
        startTestRunTime = System.nanoTime();
    }

    @AfterTest(enabled = false)
    public void afterTest() {
        long endTestRunTime = System.nanoTime();
        long duration = endTestRunTime - startTestRunTime;
        System.out.printf("#### Completed test run, total running time: %.4f in seconds%n",
                duration / NANOS_IN_SECOND);
    }

    @BeforeMethod(enabled = false)
    public static void beforeMethod() {
        runningTestTime = System.nanoTime();
        System.out.printf("**** Starting test number: %s%n", testNumberRunning);
    }

    @AfterMethod(enabled = false)
    public void afterMethod() {
        long endRunningTestTime = System.nanoTime();
        long duration = endRunningTestTime - runningTestTime;
        System.out.printf("**** Completed test number: %s, Time: %.4f%n",
                testNumberRunning, duration / NANOS_IN_SECOND);
        testNumberRunning++;
    }

    /**
     * Validate that you can create a ZIP file with and without compression
     * and that the ZIP file is created using ZIP64 if there are 65535 or
     * more entries.
     *
     * @param env         Properties used for creating the ZIP Filesystem
     * @param compression Indicates whether the files are DEFLATED(default)
     *                    or STORED
     * @throws Exception If an error occurs during the creation, verification or
     *                   deletion of the ZIP file
     */
    @Test(dataProvider = "zipfsMap", enabled = true)
    public void testZip(Map<String, String> env, int compression) throws Exception {

        System.out.printf("ZIP FS Map = %s, Compression mode= %s%n ",
                formatMap(env), compression);

        for (int entries = ZIP64_ENTRIES - 1; entries < ZIP64_ENTRIES + 2; entries++) {
            Path zipfile = generatePath(HERE, "test", ".zip");
            Files.deleteIfExists(zipfile);
            createZipFile(zipfile, env, entries);
            verify(zipfile, compression, entries,
                    isTrue(env, "forceZIP64End"), 0);
            Files.deleteIfExists(zipfile);
        }
    }

    /**
     * Validate that when the forceZIP64End property is set to true,
     * that ZIP64 is used.
     *
     * @param env         Properties used for creating the ZIP Filesystem
     * @param compression Indicates whether the files are DEFLATED(default)
     *                    or STORED
     * @throws Exception If an error occurs during the creation, verification or
     *                   deletion of the ZIP file
     */
    @Test(dataProvider = "zip64Map", enabled = true)
    public void testForceZIP64End(Map<String, String> env, int compression) throws Exception {

        System.out.printf("ZIP FS Map = %s, Compression mode= %s%n ",
                formatMap(env), compression);

        // Generate a ZIP file path
        Path zipfile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipfile);
        createZipFile(zipfile, env, 1);
        verify(zipfile, compression, 1, isTrue(env, "forceZIP64End"), 0);
        Files.deleteIfExists(zipfile);
    }

    /**
     * Validate that you can create a JAR file with and without compression
     * and that the JAR file is created using ZIP64 if there are 65535 or
     * more entries.
     *
     * @param env         Properties used for creating the ZIP Filesystem
     * @param compression Indicates whether the files are DEFLATED(default)
     *                    or STORED
     * @throws Exception If an error occurs during the creation, verification or
     *                   deletion of the JAR file
     */
    @Test(dataProvider = "zipfsMap", enabled = true)
    public void testJar(Map<String, String> env, int compression) throws Exception {
        for (int entries = ZIP64_ENTRIES - 1; entries < ZIP64_ENTRIES + 2; entries++) {
            Path jar = generatePath(HERE, "test", ".jar");

            Files.deleteIfExists(jar);
            createJarFile(jar, env, entries);

            // Now run the Main-Class specified the Manifest
            runJar(jar.getFileName().toString()).assertSuccess()
                    .validate(r -> assertTrue(r.output.matches("\\AMain\\Z")));

            verify(jar, compression, entries, isTrue(env, "forceZIP64End"),
                    ADDITIONAL_JAR_ENTRIES);
            Files.deleteIfExists(jar);
        }
    }

    /**
     * Create a ZIP File System using the specified properties and a ZIP file
     * with the specified number of entries
     *
     * @param zipFile Path to the ZIP File to create
     * @param env     Properties used for creating the ZIP Filesystem
     * @param entries Number of entries to add to the ZIP File
     * @throws IOException If an error occurs while creating the ZIP file
     */
    private void createZipFile(Path zipFile, Map<String, String> env,
                               int entries) throws IOException {
        System.out.printf("Creating file = %s%n", zipFile);
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env)) {

            for (int i = 0; i < entries; i++) {
                Files.writeString(zipfs.getPath("Entry-" + i), ZIP_FILE_VALUE);
            }
        }
    }

    /**
     * Create a ZIP File System using the specified properties and a JAR file
     * with the specified number of entries
     *
     * @param zipFile Path to the JAR File to create
     * @param env     Properties used for creating the ZIP Filesystem
     * @param entries Number of entries to add to the JAR File
     * @throws IOException If an error occurs while creating the JAR file
     */
    private void createJarFile(Path zipFile, Map<String, String> env,
                               int entries) throws IOException {
        System.out.printf("Creating file = %s%n", zipFile);
        String jdkVendor = System.getProperty("java.vendor");
        String jdkVersion = System.getProperty("java.version");
        String manifest = "Manifest-Version: 1.0"
                + System.lineSeparator()
                + "Main-Class: " + MANIFEST_MAIN_CLASS
                + System.lineSeparator()
                + "Created-By: " + jdkVersion + " (" + jdkVendor + ")";

        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env);
             InputStream in = new ByteArrayInputStream(manifest.getBytes())) {

            // Get ZIP FS path to META-INF/MANIFEST.MF
            Path metadir = zipfs.getPath("/", "META-INF");
            Path manifestFile = metadir.resolve("MANIFEST.MF");

            // Create META-INF directory if it does not already exist and
            // add the MANIFEST.MF file
            if (!Files.exists(metadir))
                Files.createDirectory(zipfs.getPath("/", "META-INF"));
            Files.copy(in, manifestFile);

            // Add the needed test classes
            Path target = zipfs.getPath("/");
            Files.copy(TEST_CLASSES.resolve(MAIN_CLASS),
                    target.resolve(MAIN_CLASS));
            Files.copy(TEST_CLASSES.resolve(THIS_CLASS),
                    target.resolve(THIS_CLASS));

            // Add the remaining entries that are required
            for (int i = ADDITIONAL_JAR_ENTRIES; i < entries; i++) {
                Files.writeString(zipfs.getPath("Entry-" + i), ZIP_FILE_VALUE);
            }
        }
    }

    /*
     * DataProvider used to validate that you can create a ZIP file with and
     * without compression.
     */
    @DataProvider(name = "zipfsMap")
    private Object[][] zipfsMap() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED}
        };
    }

    /*
     * DataProvider used to validate that you can create a ZIP file with/without
     * ZIP64 format extensions
     */
    @DataProvider(name = "zip64Map")
    private Object[][] zip64Map() {
        return new Object[][]{
                {Map.of("create", "true", "forceZIP64End", "true"),
                        ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true",
                        "forceZIP64End", "true"), ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "false",
                        "forceZIP64End", "false"), ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true",
                        "forceZIP64End", "false"), ZipEntry.STORED}
        };
    }

    /**
     * Verify that the given path is a ZIP file containing the
     * expected entries.
     *
     * @param zipfile       ZIP file to be validated
     * @param method        Expected Compression method: STORED or DEFLATED
     * @param entries       Number of expected entries
     * @param isZip64Forced true if ZIP64 use is being forced; false otherwise
     * @param start         Starting number for verifying entries
     * @throws Exception If an error occurs while examining the ZIP file
     */
    private static void verify(Path zipfile, int method, int entries,
                               boolean isZip64Forced, int start) throws Exception {
        // check entries with ZIP API
        try (ZipFile zf = new ZipFile(zipfile.toFile())) {
            // check entry count
            assertEquals(entries, zf.size());

            // check compression method and content of each entry
            for (int i = start; i < entries; i++) {
                ZipEntry ze = zf.getEntry("Entry-" + i);
                assertNotNull(ze);
                assertEquals(method, ze.getMethod());
                try (InputStream is = zf.getInputStream(ze)) {
                    byte[] bytes = is.readAllBytes();
                    assertTrue(Arrays.equals(bytes, ZIP_FILE_ENTRY));
                }
            }
        }
        // check entries with FileSystem API
        try (FileSystem fs = FileSystems.newFileSystem(zipfile)) {

            // check entry count
            Path top = fs.getPath("/");
            long count = Files.find(top, Integer.MAX_VALUE, (path, attrs) ->
                    attrs.isRegularFile() || (attrs.isDirectory() &&
                            path.getFileName() != null &&
                            path.getFileName().toString().equals("META-INF")))
                    .count();
            assertEquals(entries, count);

            // check content of each entry
            for (int i = start; i < entries; i++) {
                Path file = fs.getPath("Entry-" + i);
                byte[] bytes = Files.readAllBytes(file);
                assertTrue(Arrays.equals(bytes, ZIP_FILE_ENTRY));
            }
        }

        // Check for a ZIP64 End of Central Directory Locator
        boolean foundZip64 = usesZip64(zipfile.toFile());

        // Is ZIP64 required?
        boolean requireZip64 = entries >= ZIP64_ENTRIES || isZip64Forced;
        System.out.printf(" isZip64Forced = %s, foundZip64= %s, requireZip64= %s%n",
                isZip64Forced, foundZip64, requireZip64);
        assertEquals(requireZip64, foundZip64);


    }

    /**
     * Determine if the specified property name=true/"true"
     *
     * @param env  ZIP Filesystem Map
     * @param name property to validate
     * @return true if the property value is set to true/"true"; false otherwise
     */
    private static boolean isTrue(Map<String, ?> env, String name) {
        return "true".equals(env.get(name)) || TRUE.equals(env.get(name));
    }

    /**
     * Check to see if the ZIP64 End of Central Directory Locator has been found
     *
     * @param b byte array to check for the locator in
     * @param n starting offset for the search
     * @return true if the Zip64 End of Central Directory Locator is found; false
     * otherwise
     */
    private static boolean end64SigAt(byte[] b, int n) {
        return b[n] == 'P' & b[n + 1] == 'K' & b[n + 2] == 6 & b[n + 3] == 6;
    }

    /**
     * Utility method that checks the ZIP file for the use of the ZIP64
     * End of Central Directory Locator
     *
     * @param zipFile ZIP file to check
     * @return true if the ZIP64 End of Central Directory Locator is found; false
     * otherwise
     * * @throws Exception If an error occurs while traversing the file
     */
    private static boolean usesZip64(File zipFile) throws Exception {

        try (RandomAccessFile raf = new RandomAccessFile(zipFile, "r")) {
            byte[] buf = new byte[4096];
            long seeklen = raf.length() - buf.length;

            if (seeklen < 0)
                seeklen = 0;
            raf.seek(seeklen);
            raf.read(buf);
            for (int i = 0; i < buf.length - 4; i++) {
                // Is there a ZIP64 End of Central Directory Locator?
                if (end64SigAt(buf, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generate a temporary file Path
     *
     * @param dir    Directory used to create the path
     * @param prefix The prefix string used to create the path
     * @param suffix The suffix string used to create the path
     * @return Path that was generated
     */
    private static Path generatePath(Path dir, String prefix, String suffix) {
        long n = random.nextLong();
        String s = prefix + Long.toUnsignedString(n) + suffix;
        Path name = dir.getFileSystem().getPath(s);
        // the generated name should be a simple file name
        if (name.getParent() != null)
            throw new IllegalArgumentException("Invalid prefix or suffix");
        return dir.resolve(name);
    }

    /**
     * Utility method to return a formatted String of the key:value entries for
     * a Map
     *
     * @param env Map to format
     * @return Formatted string of the Map entries
     */
    private static String formatMap(Map<String, String> env) {
        return env.entrySet().stream()
                .map(e -> format("(%s:%s)", e.getKey(), e.getValue()))
                .collect(joining(", "));
    }

    /**
     * Validates that a jar created using ZIP FS can be used by the java
     * tool to run a program specified in the Main-Class Manifest attribute
     *
     * @param jarFile Name of the JAR file to specify to the -jar option
     * @return A Result object representing the return code and output from the
     * program that was invoked
     */
    private static Result runJar(String jarFile) {
        String javaHome = System.getProperty("java.home");
        String java = Paths.get(javaHome, "bin", "java").toString();
        String[] cmd = {java, "-jar", jarFile};
        String output;
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p;
        try {
            p = pb.start();
            output = toString(p.getInputStream(), p.getErrorStream());
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(
                    format("Error invoking: '%s', Exception= %s", pb.command(), e));
        }

        return new Result(p.exitValue(), output);
    }

    /**
     * Utility method to combine the output and error streams for the Process
     * started by ProcessBuilder
     *
     * @param is  Process Outputstream
     * @param is2 Process ErrorStream
     * @return String representing the combination of the OutputStream & ErrorStream
     * @throws IOException If an error occurs while combining the streams
     */
    private static String toString(InputStream is, InputStream is2) throws IOException {
        try (ByteArrayOutputStream dst = new ByteArrayOutputStream();
             InputStream concatenated = new SequenceInputStream(is, is2)) {
            concatenated.transferTo(dst);
            return new String(dst.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Wrapper class used to verify the results from a ProcessBuilder invocation
     */
    private static class Result {
        final int ec;         // Return code for command that was executed
        final String output;  // Output from the command that was executed

        /**
         * Constructor
         *
         * @param ec     Return code from the ProcessBuilder invocation
         * @param output ProcessBuilder output to be validated
         */
        private Result(int ec, String output) {
            this.ec = ec;
            this.output = output;
        }

        /**
         * Validate that the command that was executed completed successfully
         *
         * @return This Result object
         */
        Result assertSuccess() {
            assertEquals(ec, 0, format("Expected ec 0, received: %s, output [%s]", ec, output));
            return this;
        }

        /**
         * Validate that the expected result is received
         *
         * @param r The operation to perform
         * @return This Result object
         */
        Result validate(Consumer<Result> r) {
            r.accept(this);
            return this;
        }
    }

    /**
     * Trivial class used to validate that a JAR created using ZIP FS
     * can be successfully executed
     */
    public static class Main {
        public static void main(String[] args) {
            System.out.print("Main");
        }
    }
}
