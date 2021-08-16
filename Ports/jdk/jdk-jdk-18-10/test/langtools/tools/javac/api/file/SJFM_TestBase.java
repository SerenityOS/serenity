/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Base class for unit tests for StandardJavaFileManager.
 */
class SJFM_TestBase {

    /** Shared compiler instance. */
    JavaCompiler comp;

    /** A list of items to be closed when the test is complete. */
    List<AutoCloseable> closeables;

    /**
     * Runs a test. This is the primary entry point and should generally be
     * called from each test's main method.
     * It calls all methods annotated with {@code @Test} with the instances
     * of StandardJavaFileManager to be tested.
     *
     * @throws Exception if the test fails.
     */
    void run() throws Exception {
        comp = ToolProvider.getSystemJavaCompiler();
        closeables = new ArrayList<>();

        try (StandardJavaFileManager systemFileManager = comp.getStandardFileManager(null, null, null);
                StandardJavaFileManager customFileManager = new MyStandardJavaFileManager(systemFileManager)) {
            test(systemFileManager);
            test(customFileManager);
        } finally {
            for (AutoCloseable c: closeables) {
                try {
                    c.close();
                } catch (IOException e) {
                    error("Exception closing " + c + ": " + e);
                }
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    /**
     * Get the file managers to be tested.
     *
     * Currently, two are provided:
     * <ol>
     * <li>the system-provided file manager
     * <li>a custom file manager, which relies on the default methods provided in the
     *     StandardJavaFileManager interface
     * </li>
     *
     * @return the file managers to be tested
     */
    List<StandardJavaFileManager> getTestFileManagers() {
        StandardJavaFileManager systemFileManager = comp.getStandardFileManager(null, null, null);
        StandardJavaFileManager customFileManager = new MyStandardJavaFileManager(systemFileManager);
        return Arrays.asList(systemFileManager, customFileManager);
    }

    /**
     * Tests a specific file manager, by calling all methods annotated
     * with {@code @Test} passing this file manager as an argument.
     *
     * @param fm the file manager to be tested
     * @throws Exception if the test fails
     */
    void test(StandardJavaFileManager fm) throws Exception {
        System.err.println("Testing " + fm);
        for (Method m: getClass().getDeclaredMethods()) {
            Annotation a = m.getAnnotation(Test.class);
            if (a != null) {
                try {
                    System.err.println("Test " + m.getName());
                    m.invoke(this, new Object[] { fm });
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    throw (cause instanceof Exception) ? ((Exception) cause) : e;
                }
                System.err.println();
            }
        }
    }

    /** Marker annotation for test cases. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Test { }

    /**
     * Returns a series of paths for artifacts in the default file system.
     * The paths are for the .java files in the test.src directory.
     *
     * @return a list of paths
     * @throws IOException
     */
    List<Path> getTestFilePaths() throws IOException {
        String testSrc = System.getProperty("test.src");
        return Files.list(Paths.get(testSrc))
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
    }

    private FileSystem zipfs;
    private List<Path> zipPaths;

    /**
     * Returns a series of paths for artifacts in a non-default file system.
     * A zip file is created containing copies of the .java files in the
     * test.src directory. The paths that are returned refer to these files.
     *
     * @return a list of paths
     * @throws IOException
     */
    List<Path> getTestZipPaths() throws IOException {
        if (zipfs == null) {
            Path testZip = createSourceZip();
            zipfs = FileSystems.newFileSystem(testZip);
            closeables.add(zipfs);
            zipPaths = Files.list(zipfs.getRootDirectories().iterator().next())
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
        }
        return zipPaths;
    }

    /**
     * Create a zip file containing the contents of the test.src directory.
     *
     * @return a path for the zip file.
     * @throws IOException if there is a problem creating the file
     */
    private Path createSourceZip() throws IOException {
        Path testSrc = Paths.get(System.getProperty("test.src"));
        Path testZip = Paths.get("test.zip");
        try (OutputStream os = Files.newOutputStream(testZip)) {
            try (ZipOutputStream zos = new ZipOutputStream(os)) {
                Files.list(testSrc)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            zos.putNextEntry(new ZipEntry(p.getFileName().toString()));
                            zos.write(Files.readAllBytes(p));
                            zos.closeEntry();
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
            }
        }
        return testZip;
    }

    /**
     * Tests whether it is expected that a file manager will be able
     * to create a series of file objects from a series of paths.
     *
     * MyStandardJavaFileManager does not support paths referring to
     * non-default file systems.
     *
     * @param fm  the file manager to be tested
     * @param paths  the paths to be tested
     * @return
     */
    boolean isGetFileObjectsSupported(StandardJavaFileManager fm, List<Path> paths) {
        return !(fm instanceof MyStandardJavaFileManager
                && (paths.get(0).getFileSystem() != FileSystems.getDefault()));
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    /** Count of errors reported. */
    int errors;

}
