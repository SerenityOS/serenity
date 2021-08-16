/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static java.lang.String.format;

/**
 * @test
 * @bug 8255380 8257445
 * @summary Test that Zip FS can access the LOC offset from the Zip64 extra field
 * @modules jdk.zipfs
 * @requires (os.family == "linux") | (os.family == "mac")
 * @run testng/manual TestLocOffsetFromZip64EF
 */
public class TestLocOffsetFromZip64EF {

    private static final String ZIP_FILE_NAME = "LargeZipTest.zip";
    // File that will be created with a size greater than 0xFFFFFFFF
    private static final String LARGE_FILE_NAME = "LargeZipEntry.txt";
    // File that will be created with a size less than 0xFFFFFFFF
    private static final String SMALL_FILE_NAME = "SmallZipEntry.txt";
    // The size (4GB) of the large file to be created
    private static final long LARGE_FILE_SIZE = 4L * 1024L * 1024L * 1024L;

    /**
     * Create the files used by this test
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public void setUp() throws IOException {
        System.out.println("In setup");
        cleanup();
        createFiles();
        createZipWithZip64Ext();
    }

    /**
     * Delete files used by this test
     * @throws IOException if an error occurs
     */
    @AfterClass
    public void cleanup() throws IOException {
        System.out.println("In cleanup");
        Files.deleteIfExists(Path.of(ZIP_FILE_NAME));
        Files.deleteIfExists(Path.of(LARGE_FILE_NAME));
        Files.deleteIfExists(Path.of(SMALL_FILE_NAME));
    }

    /**
     * Create a Zip file that will result in the an Zip64 Extra (EXT) header
     * being added to the CEN entry in order to find the LOC offset for
     * SMALL_FILE_NAME.
     */
    public static void createZipWithZip64Ext() {
        System.out.println("Executing zip...");
        List<String> commands = List.of("zip", "-0", ZIP_FILE_NAME,
                LARGE_FILE_NAME, SMALL_FILE_NAME);
        Result rc = run(new ProcessBuilder(commands));
        rc.assertSuccess();
    }

    /*
     * DataProvider used to verify that a Zip file that contains a Zip64 Extra
     * (EXT) header can be traversed
     */
    @DataProvider(name = "zipInfoTimeMap")
    protected Object[][] zipInfoTimeMap() {
        return new Object[][]{
                {Map.of()},
                {Map.of("zipinfo-time", "False")},
                {Map.of("zipinfo-time", "true")},
                {Map.of("zipinfo-time", "false")}
        };
    }

    /**
     * Navigate through the Zip file entries using Zip FS
     * @param env Zip FS properties to use when accessing the Zip file
     * @throws IOException if an error occurs
     */
    @Test(dataProvider = "zipInfoTimeMap")
    public void walkZipFSTest(final Map<String, String> env) throws IOException {
        try (FileSystem fs =
                     FileSystems.newFileSystem(Paths.get(ZIP_FILE_NAME), env)) {
            for (Path root : fs.getRootDirectories()) {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes
                            attrs) throws IOException {
                        System.out.println(Files.readAttributes(file,
                                BasicFileAttributes.class).toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    /**
     * Navigate through the Zip file entries using ZipFile
     * @throws IOException if an error occurs
     */
    @Test
    public void walkZipFileTest() throws IOException {
        try (ZipFile zip = new ZipFile(ZIP_FILE_NAME)) {
            zip.stream().forEach(z -> System.out.printf("%s, %s, %s%n",
                    z.getName(), z.getMethod(), z.getLastModifiedTime()));
        }
    }

    /**
     * Create the files that will be added to the ZIP file
     * @throws IOException if there is a problem  creating the files
     */
    private static void createFiles() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(LARGE_FILE_NAME, "rw")
        ) {
            System.out.printf("Creating %s%n", LARGE_FILE_NAME);
            file.setLength(LARGE_FILE_SIZE);
            System.out.printf("Creating %s%n", SMALL_FILE_NAME);
            Files.writeString(Path.of(SMALL_FILE_NAME), "Hello");
        }
    }

    /**
     * Utility method to execute a ProcessBuilder command
     * @param pb ProcessBuilder to execute
     * @return The Result(s) from the ProcessBuilder execution
     */
    private static Result run(ProcessBuilder pb) {
        Process p;
        System.out.printf("Running: %s%n", pb.command());
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't start process '%s'", pb.command()), e);
        }

        String output;
        try {
            output = toString(p.getInputStream(), p.getErrorStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't read process output '%s'", pb.command()), e);
        }

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    format("Process hasn't finished '%s'", pb.command()), e);
        }
        return new Result(p.exitValue(), output);
    }

    /**
     * Utility Method for combining the output from a ProcessBuilder invocation
     * @param in1 ProccessBuilder.getInputStream
     * @param in2 ProcessBuilder.getErrorStream
     * @return The ProcessBuilder output
     * @throws IOException if an error occurs
     */
    static String toString(InputStream in1, InputStream in2) throws IOException {
        try (ByteArrayOutputStream dst = new ByteArrayOutputStream();
             InputStream concatenated = new SequenceInputStream(in1, in2)) {
            concatenated.transferTo(dst);
            return new String(dst.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Utility class used to hold the results from  a ProcessBuilder execution
     */
    static class Result {
        final int ec;
        final String output;

        private Result(int ec, String output) {
            this.ec = ec;
            this.output = output;
        }
        Result assertSuccess() {
            assertTrue(ec == 0, "Expected ec 0, got: ", ec, " , output [", output, "]");
            return this;
        }
    }
    static void assertTrue(boolean cond, Object ... failedArgs) {
        if (cond)
            return;
        StringBuilder sb = new StringBuilder();
        for (Object o : failedArgs)
            sb.append(o);
        Assert.fail(sb.toString());
    }
}
