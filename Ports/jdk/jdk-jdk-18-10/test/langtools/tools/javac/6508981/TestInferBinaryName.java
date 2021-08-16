/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6508981
 * @summary cleanup file separator handling in JavacFileManager
 * (This test is specifically to test the new impl of inferBinaryName)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask p.A
 * @run main TestInferBinaryName
 */

import java.io.*;
import java.util.*;
import javax.tools.*;

import static javax.tools.JavaFileObject.Kind.*;
import static javax.tools.StandardLocation.*;

import toolbox.JarTask;
import toolbox.ToolBox;

/**
 * Verify the various implementations of inferBinaryName, but configuring
 * different instances of a file manager, getting a file object, and checking
 * the impl of inferBinaryName for that file object.
 */
public class TestInferBinaryName {
    public static void main(String... args) throws Exception {
        new TestInferBinaryName().run();
    }

    void run() throws Exception {
        testDirectory();

        File testJar = createJar();
        testZipArchive(testJar);

        if (errors > 0)
            throw new Exception(errors + " error found");
    }

    File createJar() throws IOException {
        File f = new File("test.jar");
        try (JavaFileManager fm = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            ToolBox tb = new ToolBox();
            new JarTask(tb, f.getPath())
                .files(fm, StandardLocation.PLATFORM_CLASS_PATH, "java.lang.*")
                .run();
        }
        return f;
    }

    void testDirectory() throws IOException {
        String testClassName = "p.A";
        List<File> testClasses = Arrays.asList(new File(System.getProperty("test.classes")));
        try (JavaFileManager fm = getFileManager(testClasses)) {
            test("testDirectory",
                fm, testClassName, "SimpleFileObject");
        }
    }

    void testZipArchive(File testJar) throws IOException {
        String testClassName = "java.lang.String";
        List<File> path = Arrays.asList(testJar);
        try (JavaFileManager fm = getFileManager(path)) {
            test("testZipArchive",
                 fm, testClassName, "JarFileObject");
        }
    }

    /**
     * @param testName for debugging
     * @param fm suitably configured file manager
     * @param testClassName the classname to test
     * @param implClassName the expected classname of the JavaFileObject impl,
     *     used for checking that we are checking the expected impl of
     *     inferBinaryName
     */
    void test(String testName,
              JavaFileManager fm, String testClassName, String implClassName) throws IOException {
        JavaFileObject fo = fm.getJavaFileForInput(CLASS_PATH, testClassName, CLASS);
        if (fo == null) {
            System.err.println("Can't find " + testClassName);
            errors++;
            return;
        }

        String cn = fo.getClass().getSimpleName();
        String bn = fm.inferBinaryName(CLASS_PATH, fo);
        System.err.println(testName + " " + cn + " " + bn);
        checkEqual(cn, implClassName);
        checkEqual(bn, testClassName);
        System.err.println("OK");
    }

    JavaFileManager getFileManager(List<File> path)
            throws IOException {
        StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null);
        fm.setLocation(CLASS_PATH, path);
        return fm;
    }

    List<File> getPath(String s) {
        List<File> path = new ArrayList<>();
        for (String f: s.split(File.pathSeparator)) {
            if (f.length() > 0)
                path.add(new File(f));
        }
        //System.err.println("path: " + path);
        return path;
    }

    void checkEqual(String found, String expect) {
        if (!found.equals(expect)) {
            System.err.println("Expected: " + expect);
            System.err.println("   Found: " + found);
            errors++;
        }
    }

    private int errors;
}

class A { }

