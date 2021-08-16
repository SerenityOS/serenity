/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6899919
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.util.JarUtils
 *        jdk.test.lib.Platform
 * @run main/othervm GetResourceAsStream
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;

public class GetResourceAsStream extends Common {
    private static  final String WORK_DIR = System.getProperty("user.dir");

/*
 * We simply test various scenarios with class/resource files
 * and make sure the files can be deleted after closing
 * the loader. Therefore, the test will only really be verified
 * on Windows. It will still run correctly on other platforms
 */
    public static void main (String args[]) throws Exception {
        setup();

        /* the jar we copy for each test */
        File srcfile = new File(WORK_DIR, "foo.jar");

        /* the jar we use for the test */
        File testfile = new File(WORK_DIR, "test.jar");

        copyFile(srcfile, testfile);
        test(testfile, false, false);

        copyFile(srcfile, testfile);
        test(testfile, true, false);

        copyFile(srcfile, testfile);
        test(testfile, true, true);

        // repeat test using a directory of files

        File testdir = new File(WORK_DIR, "testdir");
        File srcdir = new File(WORK_DIR, "test3");

        copyDir(srcdir, testdir);
        test(testdir, true, false);
    }

    // create a loader on jarfile (or directory)
    // load a class , then look for a resource
    // then close the loader
    // check further new classes/resources cannot be loaded
    // check jar (or dir) can be deleted

    static void test (File file, boolean loadclass, boolean readall)
        throws Exception
    {
        URL[] urls = new URL[] {file.toURI().toURL()};
        System.out.println ("Doing tests with URL: " + urls[0]);
        URLClassLoader loader = new URLClassLoader (urls);
        if (loadclass) {
            Class testclass = loadClass ("com.foo.TestClass", loader, true);
        }
        InputStream s = loader.getResourceAsStream ("hello.txt");
        s.read();
        if (readall) {
            while (s.read() != -1) ;
            s.close();
        }

        loader.close ();

        // should not find bye.txt now
        InputStream s1 = loader.getResourceAsStream("bye.txt");
        if (s1 != null) {
            throw new RuntimeException ("closed loader returned resource");
        }

        // now check we can delete the path
        rm_minus_rf (file);
        System.out.println (" ... OK");
    }

    /**
     * Prepare jars files for the tests
     */
    private static void setup () throws IOException {
        Path classes = Paths.get(WORK_DIR);
        Path testSrc = Paths.get(System.getProperty("test.src"),
                "test1", "com", "foo", "TestClass.java");
        Path targetDir = classes.resolve("test3");
        Path testTarget = targetDir.resolve("TestClass.java");
        Files.createDirectories(targetDir);
        Files.copy(testSrc, testTarget, StandardCopyOption.REPLACE_EXISTING);
        // Compile sources for corresponding test
        CompilerUtils.compile(targetDir, targetDir);
        // Prepare txt files
        Files.write(targetDir.resolve("hello.txt"), "Hello world".getBytes(),
                    StandardOpenOption.CREATE);
        Files.write(targetDir.resolve("bye.txt"), "Bye world".getBytes(),
                    StandardOpenOption.CREATE);
        // Create jar
        JarUtils.createJarFile(classes.resolve("foo.jar"), targetDir);
    }
}
