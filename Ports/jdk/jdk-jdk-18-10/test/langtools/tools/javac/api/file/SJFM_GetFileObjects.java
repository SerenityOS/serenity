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
 * @bug 8059977 8220687
 * @summary StandardJavaFileManager should support java.nio.file.Path.
 *          Test getFileObject methods.
 * @modules java.compiler
 *          jdk.compiler
 * @build SJFM_TestBase
 * @run main SJFM_GetFileObjects
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/**
 * For those paths supported by a file manager, verify that the paths
 * can be encapsulated by file objects, such that the file objects can
 * be used by a tool such as javac.
 */
public class SJFM_GetFileObjects extends SJFM_TestBase {
    public static void main(String... args) throws Exception {
        new SJFM_GetFileObjects().run();
    }

    @Test
    void test_getJavaFileObjects(StandardJavaFileManager fm) throws IOException {
        test_getJavaFileObjects(fm, getTestFilePaths());
        test_getJavaFileObjects(fm, getTestZipPaths());
    }

    /**
     * Tests the getJavaFileObjects method for a specific file manager
     * and a series of paths.
     *
     * Note: instances of MyStandardJavaFileManager only support
     * encapsulating paths for files in the default file system.
     *
     * @param fm  the file manager to be tested
     * @param paths  the paths to be tested
     * @throws IOException
     */
    void test_getJavaFileObjects(StandardJavaFileManager fm, List<Path> paths) throws IOException {
        boolean expectException = !isGetFileObjectsSupported(fm, paths);
        try {
            compile(fm.getJavaFileObjects(paths.toArray(new Path[paths.size()])));
            if (expectException)
                error("expected exception not thrown");
        } catch (RuntimeException e) {
            if (expectException && e instanceof IllegalArgumentException)
                return;
            error("unexpected exception thrown: " + e);
        }
    }

    //----------------------------------------------------------------------------------------------

    @Test
    void test_getJavaFileObjectsFromPaths(StandardJavaFileManager fm) throws IOException {
        test_getJavaFileObjectsFromPaths(fm, getTestFilePaths());
        test_getJavaFileObjectsFromPaths(fm, getTestZipPaths());
    }

    /**
     * Tests the getJavaFileObjectsFromPaths method for a specific file manager
     * and a series of paths.
     *
     * Note: instances of MyStandardJavaFileManager only support
     * encapsulating paths for files in the default file system.
     *
     * @param fm  the file manager to be tested
     * @param paths  the paths to be tested
     * @throws IOException
     */
    void test_getJavaFileObjectsFromPaths(StandardJavaFileManager fm, List<Path> paths)
            throws IOException {
        boolean expectException = !isGetFileObjectsSupported(fm, paths);
        try {
            compile(fm.getJavaFileObjectsFromPaths(paths));
            if (expectException)
                error("expected exception not thrown: " + IllegalArgumentException.class.getName());
        } catch (RuntimeException e) {
            if (expectException && e instanceof IllegalArgumentException)
                return;
            error("unexpected exception thrown: " + e);
        }
    }


    //----------------------------------------------------------------------------------------------

    @Test
    void test_getJavaFileObjectsFromPaths_Iterable(StandardJavaFileManager fm) throws IOException {
        test_getJavaFileObjectsFromPaths_Iterable(fm, getTestFilePaths());
        test_getJavaFileObjectsFromPaths_Iterable(fm, getTestZipPaths());
    }

    /**
     * Tests the {@code getJavaFileObjectsFromPaths(Iterable)} method for a specific file
     * manager and a series of paths.
     *
     * Note: instances of MyStandardJavaFileManager only support
     * encapsulating paths for files in the default file system.
     *
     * @param fm  the file manager to be tested
     * @param paths  the paths to be tested
     * @throws IOException
     */
    void test_getJavaFileObjectsFromPaths_Iterable(StandardJavaFileManager fm, List<Path> paths)
            throws IOException {
        boolean expectException = !isGetFileObjectsSupported(fm, paths);
        try {
            compile(fm.getJavaFileObjectsFromPaths((Iterable<Path>) paths));
            if (expectException)
                error("expected exception not thrown: " + IllegalArgumentException.class.getName());
        } catch (RuntimeException e) {
            if (expectException && e instanceof IllegalArgumentException)
                return;
            error("unexpected exception thrown: " + e);
        }
    }


    //----------------------------------------------------------------------------------------------

    /**
     * Compiles a set of files.
     *
     * @param files the files to be compiled.
     * @throws IOException
     */
    void compile(Iterable<? extends JavaFileObject> files) throws IOException {
        String name = "compile" + (compileCount++);
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            File f = new File(name);
            f.mkdirs();
            // use setLocation(Iterable<File>) to avoid relying on setLocationFromPaths
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(f));
            boolean ok = comp.getTask(null, fm, null, null, null, files).call();
            if (!ok)
                error(name + ": compilation failed");
        }
    }

    int compileCount;
}
