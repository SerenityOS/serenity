/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8059977
 * @summary StandardJavaFileManager should support java.nio.file.Path.
 *          Test get/setLocation methods.
 * @modules java.compiler
 *          jdk.compiler
 * @build SJFM_TestBase
 * @run main SJFM_Locations
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;

/**
 * For those paths which are supported by a file manager, verify
 * that setLocation can accept such paths, and that getLocation
 * can subsequently return the same paths.
 *
 * In addition, for files in the default file system, verify
 * the combinations of setting a location using files or paths
 * and then subsequently getting the location as files or paths.
 */
public class SJFM_Locations extends SJFM_TestBase {
    public static void main(String... args) throws Exception {
        new SJFM_Locations().run();
    }

    @Test
    void test_locations(StandardJavaFileManager fm) throws IOException {
        test_setFiles_getFiles(fm, getTestFileDirs());
        test_setFiles_getPaths(fm, getTestFileDirs());
        test_setPaths_getFiles(fm, getTestFilePathDirs());
        test_setPaths_getPaths(fm, getTestFilePathDirs());
//        test_setPaths_getPaths(fm, getTestZipPathDirs());
    }

    void test_setFiles_getFiles(StandardJavaFileManager fm, List<File> inFiles) throws IOException {
        System.err.println("test_setFiles_getFiles");
        JavaFileManager.Location l = newLocation();
        fm.setLocation(l, inFiles);
        Iterable<? extends File> outFiles = fm.getLocation(l);
        compare(inFiles, outFiles);
    }

    void test_setFiles_getPaths(StandardJavaFileManager fm, List<File> inFiles) throws IOException {
        System.err.println("test_setFiles_getPaths");
        JavaFileManager.Location l = newLocation();
        fm.setLocation(l, inFiles);
        Iterable<? extends Path> outPaths = fm.getLocationAsPaths(l);
        compare(inFiles, outPaths);
    }

    void test_setPaths_getFiles(StandardJavaFileManager fm, List<Path> inPaths) throws IOException {
        System.err.println("test_setPaths_getFiles");
        JavaFileManager.Location l = newLocation();
        fm.setLocationFromPaths(l, inPaths);
        Iterable<? extends File> outFiles = fm.getLocation(l);
        compare(inPaths, outFiles);
    }

    void test_setPaths_getPaths(StandardJavaFileManager fm, List<Path> inPaths) throws IOException {
        System.err.println("test_setPaths_getPaths");
        JavaFileManager.Location l = newLocation();
        fm.setLocationFromPaths(l, inPaths);
        Iterable<? extends Path> outPaths = fm.getLocationAsPaths(l);
        compare(inPaths, outPaths);
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Gets a representative series of directories in the default file system,
     * derived from the test.src directory and test.classes path.
     *
     * @return a list of directories, represented with {@code File}
     * @throws IOException
     */
    List<File> getTestFileDirs() throws IOException {
        return Stream.of("test.src", "test.classes")
                .map(s -> System.getProperty(s))
                .flatMap(s -> Stream.of(s.split(File.pathSeparator, 0)))
                .filter(s -> !s.isEmpty())
                .map(s -> new File(s))
                .collect(Collectors.toList());
    }

    /**
     * Gets a representative series of directories in the default file system,
     * derived from the test.src directory and test.classes path.
     *
     * @return a list of directories, represented with {@code Path}
     * @throws IOException
     */
    List<Path> getTestFilePathDirs() throws IOException {
        return Stream.of("test.src", "test.classes")
                .map(s -> System.getProperty(s))
                .flatMap(s -> Stream.of(s.split(File.pathSeparator, 0)))
                .filter(s -> !s.isEmpty())
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
    }


    /**
     * Compares two lists of items by comparing their individual string representations.
     *
     * @param in   the first set of items to be compared
     * @param out  the second set of items to be compared
     */
    void compare(Iterable<?> in, Iterable<?> out) {
        List<String> ins = toString(in);
        List<String> outs = toString(out);
        if (!ins.equals(outs)) {
            error("mismatch in comparison");
            System.err.println("in:");
            for (String s: ins) System.err.println(s);
            System.err.println("out:");
            for (String s: outs) System.err.println(s);
        }
    }

    List<String> toString(Iterable<?> iter) {
        List<String> strings = new ArrayList<>();
        for (Object item: iter)
            strings.add(item.toString());
        return strings;
    }

    /**
     * Create an instance of a location.
     * @return a location
     */
    JavaFileManager.Location newLocation() {
        final String name = "locn" + (count++);
        return new JavaFileManager.Location() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isOutputLocation() {
                return false;
            }
        };
    }

    int count = 0;
}
