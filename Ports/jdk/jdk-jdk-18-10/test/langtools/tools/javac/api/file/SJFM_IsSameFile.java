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
 *          Test isSameFile method.
 * @modules java.compiler
 *          jdk.compiler
 * @build SJFM_TestBase
 * @run main SJFM_IsSameFile
 */

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * For those paths which are supported by a file manager, such that
 * a file object can encapsulate the path, verify that the underlying
 * paths can be compared.
 */
public class SJFM_IsSameFile extends SJFM_TestBase {
    public static void main(String... args) throws Exception {
        new SJFM_IsSameFile().run();
    }

    @Test
    void test_isSameFile(StandardJavaFileManager fm) throws Exception {
        test_isSameFile(fm, () -> getTestFilePaths());
        test_isSameFile(fm, () -> getTestZipPaths());
    }

    /**
     * Tests the isSameFile method for a specific file manager
     * and a series of paths.
     *
     * Note: instances of MyStandardJavaFileManager only support
     * encapsulating paths for files in the default file system.
     *
     * @param fm  the file manager to be tested
     * @param paths  a generator for the paths to be tested
     * @throws IOException
     */
    void test_isSameFile(StandardJavaFileManager fm, Callable<List<Path>> paths) throws Exception {
        if (!isGetFileObjectsSupported(fm, paths.call()))
            return;

        // use distinct paths and file objects in the following two sets
        Iterable<? extends JavaFileObject> setA = fm.getJavaFileObjectsFromPaths(paths.call());
        Iterable<? extends JavaFileObject> setB = fm.getJavaFileObjectsFromPaths(paths.call());
        for (JavaFileObject a : setA) {
            for (JavaFileObject b : setB) {
                System.err.println("compare: a: " + a);
                System.err.println("         b: " + b);
                // Use the fileObject getName method to determine the expected result.
                // For the files being tested, getName is the absolute path.
                boolean expect = a.getName().equals(b.getName());
                boolean actual = fm.isSameFile(a, b);
                if (actual != expect) {
                    error("mismatch: actual:" + (actual ? "same" : "not same")
                            + ", expect:" + (expect ? "same" : "not same"));
                }
            }
        }
    }
}
