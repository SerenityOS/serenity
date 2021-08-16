/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6901992
 * @summary InvalidJarIndexException due to bug in sun.misc.JarIndex.merge()
 * @modules java.base/jdk.internal.util.jar
 * @compile -XDignore.symbol.file JarIndexMergeTest.java
 * @run main JarIndexMergeTest
 * @author  Diego Belfer
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
// implementation specific API
import jdk.internal.util.jar.JarIndex;

public class JarIndexMergeTest {
    static final String slash = File.separator;
    static final String testClassesDir = System.getProperty("test.classes", ".");
    static final File tmpFolder = new File(testClassesDir);

    public static void main(String[] args) throws Exception {
        File jar1 = buildJar1();
        File jar2 = buildJar2();

        JarIndex jarIndex1 = new JarIndex(new String[] { jar1.getAbsolutePath() });
        JarIndex jarIndex2 = new JarIndex(new String[] { jar2.getAbsolutePath() });

        jarIndex1.merge(jarIndex2, null);

        assertFileResolved(jarIndex2, "com/test1/resource1.file",
                           jar1.getAbsolutePath());
        assertFileResolved(jarIndex2, "com/test2/resource2.file",
                           jar2.getAbsolutePath());
    }

    static void assertFileResolved(JarIndex jarIndex2, String file,
                                   String jarName) {
        @SuppressWarnings("unchecked")
        List<String> jarLists = (List<String>)jarIndex2.get(file);
        if (jarLists == null || jarLists.size() == 0 ||
            !jarName.equals(jarLists.get(0))) {
            throw new RuntimeException(
                "Unexpected result: the merged index must resolve file: "
                + file);
        }
    }

    private static File buildJar1() throws FileNotFoundException, IOException {
        JarBuilder jar1Builder = new JarBuilder(tmpFolder, "jar1-merge.jar");
        jar1Builder.addResourceFile("com/test1/resource1.file", "resource1");
        return jar1Builder.build();
    }

    private static File buildJar2() throws FileNotFoundException, IOException {
        JarBuilder jar2Builder = new JarBuilder(tmpFolder, "jar2-merge.jar");
        jar2Builder.addResourceFile("com/test2/resource2.file", "resource2");
        return jar2Builder.build();
    }

    /*
     * Helper class for building jar files
     */
    public static class JarBuilder {
        private JarOutputStream os;
        private File jarFile;

        public JarBuilder(File tmpFolder, String jarName)
            throws FileNotFoundException, IOException
        {
            this.jarFile = new File(tmpFolder, jarName);
            this.os = new JarOutputStream(new FileOutputStream(jarFile));
        }

        public void addResourceFile(String pathFromRoot, String content)
            throws IOException
        {
            JarEntry entry = new JarEntry(pathFromRoot);
            os.putNextEntry(entry);
            os.write(content.getBytes("ASCII"));
            os.closeEntry();
        }

        public File build() throws IOException {
            os.close();
            return jarFile;
        }
    }
}

