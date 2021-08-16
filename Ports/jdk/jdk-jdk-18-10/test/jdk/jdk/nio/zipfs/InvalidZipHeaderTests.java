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
 */

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static java.nio.file.Files.walk;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8222807
 * @summary Validate that you can iterate a ZIP file with invalid ZIP header entries
 * @modules jdk.zipfs
 * @compile InvalidZipHeaderTests.java
 * @run testng InvalidZipHeaderTests
 * @run testng/othervm/java.security.policy=test.policy  InvalidZipHeaderTests
 */
public class InvalidZipHeaderTests {


    // Name of Jar file used in tests
    private static final String INVALID_JAR_FILE = "invalid.jar";

    /**
     * Create the JAR files used by the tests
     */
    @BeforeClass
    public void setUp() throws Exception {
        createInvalidJarFile();
    }

    /**
     * Remove JAR files used by test as part of clean-up
     */
    @AfterClass
    public void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(INVALID_JAR_FILE));
    }


    /**
     * Validate that you can walk a ZIP archive with header entries
     * such as "foo//"
     */
    @Test(dataProvider = "startPaths")
    public void walkInvalidHeaderTest(String startPath, List<String> expectedPaths)
            throws IOException {
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(Path.of(INVALID_JAR_FILE))) {
            List<String> result = walk(zipfs.getPath(startPath))
                    .map(f -> f.toString()).collect(Collectors.toList());
            assertTrue(result.equals(expectedPaths),
                    String.format("Error: Expected paths not found when walking"
                                    + "%s,  starting at %s%n", INVALID_JAR_FILE,
                            startPath));
        }
    }


    /**
     * Starting Path for walking the ZIP archive and the expected paths to be returned
     * when traversing the archive
     */
    @DataProvider(name = "startPaths")
    public static Object[][] Name() {
        return new Object[][]{

                {"luckydog", List.of("luckydog", "luckydog/outfile.txt")},
                {"/luckydog", List.of("/luckydog", "/luckydog/outfile.txt")},
                {"./luckydog", List.of("./luckydog", "./luckydog/outfile.txt")},
                {"", List.of( "", "luckydog", "luckydog/outfile.txt")},
                {"/", List.of("/", "/luckydog", "/luckydog/outfile.txt")},
                {".", List.of(".", "./luckydog", "./luckydog/outfile.txt")},
                {"./", List.of(".", "./luckydog", "./luckydog/outfile.txt")}
        };
    }

    /**
     * Create a jar file with invalid CEN and LOC headers
     * @throws IOException
     */
    static void createInvalidJarFile() throws IOException {

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(INVALID_JAR_FILE))) {
            JarEntry je = new JarEntry("luckydog//");
            jos.putNextEntry(je);
            jos.closeEntry();
            je = new JarEntry("luckydog//outfile.txt");
            jos.putNextEntry(je);
            jos.write("Tennis Anyone!!".getBytes());
            jos.closeEntry();
        }
    }

}
