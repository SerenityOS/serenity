/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8164130
 * @summary test IOException handling
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestIOException
 */

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import javadoc.tester.JavadocTester;

/**
 * Tests IO Exception handling.
 *
 * Update: Windows does not permit setting folder to be readonly.
 * https://support.microsoft.com/en-us/help/326549/you-cannot-view-or-change-the-read-only-or-the-system-attributes-of-fo
 */
public class TestIOException extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestIOException tester = new TestIOException();
        tester.runTests();
    }

    /**
     * Tests a read-only directory.
     * On Windows, this test may be skipped.
     */
    @Test
    public void testReadOnlyDirectory() {
        File outDir = new File("out1");
        if (!outDir.mkdir()) {
            throw error(outDir, "Cannot create directory");
        }
        if (!outDir.setReadOnly()) {
            if (skip(outDir)) {
                return;
            }
            throw error(outDir, "could not set directory read-only");
        }
        if (outDir.canWrite()) {
            throw error(outDir, "directory is writable");
        }

        try {
            javadoc("-d", outDir.toString(),
                    "-Xdoclint:-missing",
                    new File(testSrc, "TestIOException.java").getPath());
            checkExit(Exit.ERROR);
            checkOutput(Output.OUT, true,
                "Destination directory not writable: " + outDir);
        } finally {
            outDir.setWritable(true);
        }
    }

    /**
     * Tests a read-only file.
     * @throws Exception if an error occurred
     */
    @Test
    public void testReadOnlyFile() throws Exception {
        File outDir = new File("out2");
        if (!outDir.mkdir()) {
            throw error(outDir, "Cannot create directory");
        }
        File index = new File(outDir, "index.html");
        try (FileWriter fw = new FileWriter(index)) { }
        if (!index.setReadOnly()) {
            throw error(index, "could not set index read-only");
        }
        if (index.canWrite()) {
            throw error(index, "index is writable");
        }

        try {
            setOutputDirectoryCheck(DirectoryCheck.NONE);
            javadoc("-d", outDir.toString(),
                    "-Xdoclint:-missing",
                    new File(testSrc, "TestIOException.java").getPath());

            checkExit(Exit.ERROR);
            checkOutput(Output.OUT, true,
                "Error writing file: " + index);
        } finally {
            setOutputDirectoryCheck(DirectoryCheck.EMPTY);
            index.setWritable(true);
        }
    }

    /**
     * Tests a read-only subdirectory.
     * On Windows, this test may be skipped.
     * @throws Exception if an error occurred
     */
    @Test
    public void testReadOnlySubdirectory() throws Exception {
        // init source file
        File srcDir = new File("src4");
        File src_p = new File(srcDir, "p");
        src_p.mkdirs();
        File src_p_C = new File(src_p, "C.java");
        try (FileWriter fw = new FileWriter(src_p_C)) {
            fw.write("package p; public class C { }");
        }

        // create an unwritable package output directory
        File outDir = new File("out3");
        File pkgOutDir = new File(outDir, "p");
        if (!pkgOutDir.mkdirs()) {
            throw error(pkgOutDir, "Cannot create directory");
        }
        if (!pkgOutDir.setReadOnly()) {
            if (skip(pkgOutDir)) {
                return;
            }
            throw error(pkgOutDir, "could not set directory read-only");
        }
        if (pkgOutDir.canWrite()) {
            throw error(pkgOutDir, "directory is writable");
        }

        // run javadoc and check results
        try {
            setOutputDirectoryCheck(DirectoryCheck.NONE);
            javadoc("-d", outDir.toString(),
                    "-Xdoclint:-missing",
                    src_p_C.getPath());
            checkExit(Exit.ERROR);
            checkOutput(Output.OUT, true,
                "Error writing file: " + new File(pkgOutDir, "C.html"));
        } finally {
            setOutputDirectoryCheck(DirectoryCheck.EMPTY);
            pkgOutDir.setWritable(true);
        }
    }

    /**
     * Tests a read-only doc-files directory.
     * On Windows, this test may be skipped.
     * @throws Exception if an error occurred
     */
    @Test
    public void testReadOnlyDocFilesDir() throws Exception {
        // init source files
        File srcDir = new File("src4");
        File src_p = new File(srcDir, "p");
        src_p.mkdirs();
        File src_p_C = new File(src_p, "C.java");
        try (FileWriter fw = new FileWriter(src_p_C)) {
            fw.write("package p; public class C { }");
        }
        File src_p_docfiles = new File(src_p, "doc-files");
        src_p_docfiles.mkdir();
        try (FileWriter fw = new FileWriter(new File(src_p_docfiles, "info.txt"))) {
            fw.write("info");
        }

        // create an unwritable doc-files output directory
        File outDir = new File("out4");
        File pkgOutDir = new File(outDir, "p");
        File docFilesOutDir = new File(pkgOutDir, "doc-files");
        if (!docFilesOutDir.mkdirs()) {
            throw error(docFilesOutDir, "Cannot create directory");
        }
        if (!docFilesOutDir.setReadOnly()) {
            if (skip(docFilesOutDir)) {
                return;
            }
            throw error(docFilesOutDir, "could not set directory read-only");
        }
        if (docFilesOutDir.canWrite()) {
            throw error(docFilesOutDir, "directory is writable");
        }

        try {
            setOutputDirectoryCheck(DirectoryCheck.NONE);
            javadoc("-d", outDir.toString(),
                    "-Xdoclint:-missing",
                    "-sourcepath", srcDir.getPath(),
                    "p");
            checkExit(Exit.ERROR);
            checkOutput(Output.OUT, true,
                "Error writing file: " + new File(docFilesOutDir, "info.txt"));
        } finally {
            setOutputDirectoryCheck(DirectoryCheck.EMPTY);
            docFilesOutDir.setWritable(true);
        }
    }

    private Error error(File f, String message) {
        out.println(f + ": " + message);
        showAllAttributes(f.toPath());
        throw new Error(f + ": " + message);
    }

    private void showAllAttributes(Path p) {
        showAttributes(p, "*");
        showAttributes(p, "posix:*");
        showAttributes(p, "dos:*");
    }

    private void showAttributes(Path p, String attributes) {
        out.println("Attributes: " + attributes);
        try {
            Map<String, Object> map = Files.readAttributes(p, attributes);
            map.forEach((n, v) -> out.format("  %-10s: %s%n", n, v));
        } catch (UnsupportedOperationException e) {
            out.println("Attributes not available " + attributes);
        } catch (Throwable t) {
            out.println("Error accessing attributes " + attributes + ": " + t);
        }
    }

    private boolean skip(File dir) {
        if (isWindows()) {
            showAllAttributes(dir.toPath());
            out.println("Windows: cannot set directory read only:" + dir);
            out.println("TEST CASE SKIPPED");
            return true;
        } else {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
    }
}

