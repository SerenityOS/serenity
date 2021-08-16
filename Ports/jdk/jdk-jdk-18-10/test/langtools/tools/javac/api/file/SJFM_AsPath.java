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
 *          Test asPath method.
 * @modules java.compiler
 *          jdk.compiler
 * @build SJFM_TestBase
 * @run main SJFM_AsPath
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * For those paths which are supported by a file manager, such that
 * a file object can encapsulate the path, verify that the underlying
 * path can be recovered from the file object.
 */
public class SJFM_AsPath extends SJFM_TestBase {
    public static void main(String... args) throws Exception {
        new SJFM_AsPath().run();
    }

    @Test
    void test_asPath(StandardJavaFileManager fm) throws IOException {
        test_asPath(fm, getTestFilePaths());
        test_asPath(fm, getTestZipPaths());
    }

    /**
     * Tests the asPath method for a specific file manager and a series
     * of paths.
     *
     * Note: instances of MyStandardJavaFileManager only support
     * encapsulating paths for files in the default file system,
     * and throw UnsupportedOperationException for asPath.
     *
     * @param fm  the file manager to be tested
     * @param paths  the paths to be tested
     * @throws IOException
     */
    void test_asPath(StandardJavaFileManager fm, List<Path> paths) throws IOException {
        if (!isGetFileObjectsSupported(fm, paths))
            return;
        boolean expectException = (fm instanceof MyStandardJavaFileManager);

        Set<Path> ref = new HashSet<>(paths);
        for (JavaFileObject fo : fm.getJavaFileObjectsFromPaths(paths)) {
            try {
                Path path = fm.asPath(fo);
                if (expectException)
                    error("expected exception not thrown: " + UnsupportedOperationException.class.getName());
                boolean found = ref.remove(path);
                if (!found) {
                    error("Unexpected path found: " + path + "; expected one of " + ref);
                }
            } catch (Exception e) {
                if (expectException && e instanceof UnsupportedOperationException)
                    continue;
                error("unexpected exception thrown: " + e);
            }
        }
    }
}
