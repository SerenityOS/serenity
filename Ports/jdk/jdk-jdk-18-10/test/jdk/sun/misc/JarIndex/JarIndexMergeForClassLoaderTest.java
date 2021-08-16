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
 * @bug     6901992
 * @summary InvalidJarIndexException due to bug in sun.misc.JarIndex.merge()
 *          Test URLClassLoader usage of the merge method when using indexes
 * @author  Diego Belfer
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarIndexMergeForClassLoaderTest {
    static final String slash = File.separator;
    static final String testClassesDir = System.getProperty("test.classes", ".");
    static final String jar;
    static final boolean debug = true;
    static final File tmpFolder = new File(testClassesDir);

    static {
        jar = System.getProperty("java.home") + slash + "bin" + slash + "jar";
    }

    public static void main(String[] args) throws Exception {
        // Create the jars file
        File jar1 = buildJar1();
        File jar2 = buildJar2();
        File jar3 = buildJar3();

        // Index jar files in two levels: jar1 -> jar2 -> jar3
        createIndex(jar2.getName(), jar3.getName());
        createIndex(jar1.getName(), jar2.getName());

        // Get root jar of the URLClassLoader
        URL url = jar1.toURI().toURL();

        URLClassLoader classLoader = new URLClassLoader(new URL[] { url });

        assertResource(classLoader, "com/jar1/resource.file", "jar1");
        assertResource(classLoader, "com/test/resource1.file", "resource1");
        assertResource(classLoader, "com/jar2/resource.file", "jar2");
        assertResource(classLoader, "com/test/resource2.file", "resource2");
        assertResource(classLoader, "com/test/resource3.file", "resource3");

        /*
         * The following two asserts failed before the fix of the bug 6901992
         */
        // Check that an existing file is found using the merged index
        assertResource(classLoader, "com/missing/jar3/resource.file", "jar3");
        // Check that a non existent file in directory which does not contain
        // any file is not found and it does not throw InvalidJarIndexException
        assertResource(classLoader, "com/missing/nofile", null);
    }

    private static File buildJar3() throws FileNotFoundException, IOException {
        JarBuilder jar3Builder = new JarBuilder(tmpFolder, "jar3.jar");
        jar3Builder.addResourceFile("com/test/resource3.file", "resource3");
        jar3Builder.addResourceFile("com/missing/jar3/resource.file", "jar3");
        return jar3Builder.build();
    }

    private static File buildJar2() throws FileNotFoundException, IOException {
        JarBuilder jar2Builder = new JarBuilder(tmpFolder, "jar2.jar");
        jar2Builder.addResourceFile("com/jar2/resource.file", "jar2");
        jar2Builder.addResourceFile("com/test/resource2.file", "resource2");
        return jar2Builder.build();
    }

    private static File buildJar1() throws FileNotFoundException, IOException {
        JarBuilder jar1Builder = new JarBuilder(tmpFolder, "jar1.jar");
        jar1Builder.addResourceFile("com/jar1/resource.file", "jar1");
        jar1Builder.addResourceFile("com/test/resource1.file", "resource1");
        return jar1Builder.build();
    }

    /* create the index */
    static void createIndex(String parentJar, String childJar) {
        // ProcessBuilder is used so that the current directory can be set
        // to the directory that directly contains the jars.
        debug("Running jar to create the index for: " + parentJar + " and "
                + childJar);
        ProcessBuilder pb = new ProcessBuilder(jar, "-i", parentJar, childJar);

        pb.directory(tmpFolder);
        // pd.inheritIO();
        try {
            Process p = pb.start();
            if (p.waitFor() != 0)
                throw new RuntimeException("jar indexing failed");

            if (debug && p != null) {
                debugStream(p.getInputStream());
                debugStream(p.getErrorStream());
            }
        } catch (InterruptedException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    private static void debugStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                debug(line);
            }
        }
    }

    private static void assertResource(URLClassLoader classLoader, String file,
            String expectedContent) throws IOException {
        InputStream fileStream = classLoader.getResourceAsStream(file);

        if (fileStream == null && expectedContent == null) {
            return;
        }
        if (fileStream == null && expectedContent != null) {
            throw new RuntimeException(
                    buildMessage(file, expectedContent, null));
        }
        try {
            String actualContent = readAsString(fileStream);

            if (fileStream != null && expectedContent == null) {
                throw new RuntimeException(buildMessage(file, null,
                        actualContent));
            }
            if (!expectedContent.equals(actualContent)) {
                throw new RuntimeException(buildMessage(file, expectedContent,
                        actualContent));
            }
        } finally {
            fileStream.close();
        }
    }

    private static String buildMessage(String file, String expectedContent,
            String actualContent) {
        return "Expected: " + expectedContent + " for: " + file + " was: "
                + actualContent;
    }

    private static String readAsString(InputStream fileStream)
            throws IOException {
        byte[] buffer = new byte[1024];
        int count, len = 0;
        while ((count = fileStream.read(buffer, len, buffer.length-len)) != -1)
                len += count;
        return new String(buffer, 0, len, "ASCII");
    }

    static void debug(Object message) {
        if (debug)
            System.out.println(message);
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

