/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7150368 8003412 8000407
 * @summary javac should include basic ability to generate native headers
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

public class NativeHeaderTest {
    public static void main(String... args) throws Exception {
        new NativeHeaderTest().run();
    }

    /** How to invoke javac. */
    enum RunKind {
        /** Use the command line entry point. */
        CMD,
        /** Use the JavaCompiler API. */
        API
    };

    /** Which classes for which to generate headers. */
    enum GenKind {
        /** Just classes with native methods or the marker annotation. */
        SIMPLE,
        /** All appropriate classes within the top level class. */
        FULL
    };

    // ---------- Test cases, invoked reflectively via run. ----------

    @Test
    void simpleTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C { native void m(); }"));

        Set<String> expect = createSet("C.h");

        test(rk, gk, files, expect);
    }

    @Test
    void nestedClassTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C { static class Inner { native void m(); } }"));

        Set<String> expect = createSet("C_Inner.h");
        if (gk == GenKind.FULL) expect.add("C.h");

        test(rk, gk, files, expect);
    }

    @Test
    void localClassTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C { native void m(); void m2() { class Local { } } }"));

        Set<String> expect = createSet("C.h");

        test(rk, gk, files, expect);
    }

    @Test
    void syntheticClassTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C {\n"
                + "    private C() { }\n"
                + "    class Inner extends C { native void m(); }\n"
                + "}"));

        Set<String> expect = createSet("C_Inner.h");
        if (gk == GenKind.FULL) expect.add("C.h");

        test(rk, gk, files, expect);

        // double check the synthetic class was generated
        checkEqual("generatedClasses",
                createSet("C.class", "C$Inner.class"),
                createSet(classesDir.list()));
    }

    @Test
    void annoTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C { @java.lang.annotation.Native public static final int i = 1907; }"));

        Set<String> expect = createSet("C.h");

        test(rk, gk, files, expect);
    }

    @Test
    void annoNestedClassTest(RunKind rk, GenKind gk) throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(createFile("p/C.java",
                "class C { class Inner { @java.lang.annotation.Native public static final int i = 1907; } }"));

        Set<String> expect = createSet("C_Inner.h");
        if (gk == GenKind.FULL) expect.add("C.h");

        test(rk, gk, files, expect);
    }

    /**
     * The worker method for each test case.
     * Compile the files and verify that exactly the expected set of header files
     * is generated.
     */
    void test(RunKind rk, GenKind gk, List<File> files, Set<String> expect) throws Exception {
        List<String> args = new ArrayList<String>();
        if (gk == GenKind.FULL)
            args.add("-XDjavah:full");

        switch (rk) {
            case CMD:
                args.add("-d");
                args.add(classesDir.getPath());
                args.add("-h");
                args.add(headersDir.getPath());
                for (File f: files)
                    args.add(f.getPath());
                int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]));
                if (rc != 0)
                    throw new Exception("compilation failed, rc=" + rc);
                break;

            case API:
                fm.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(srcDir));
                fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(classesDir));
                fm.setLocation(StandardLocation.NATIVE_HEADER_OUTPUT, Arrays.asList(headersDir));
                JavacTask task = javac.getTask(null, fm, null, args, null,
                        fm.getJavaFileObjectsFromFiles(files));
                if (!task.call())
                    throw new Exception("compilation failed");
                break;
        }

        Set<String> found = createSet(headersDir.list());
        checkEqual("header files", expect, found);
    }

    /** Marker annotation for test cases. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Test { }

    /** Combo test to run all test cases in all modes. */
    void run() throws Exception {
        javac = JavacTool.create();
        fm = javac.getStandardFileManager(null, null, null);
        try {
            for (RunKind rk: RunKind.values()) {
                for (GenKind gk: GenKind.values()) {
                    for (Method m: getClass().getDeclaredMethods()) {
                        Annotation a = m.getAnnotation(Test.class);
                        if (a != null) {
                            init(rk, gk, m.getName());
                            try {
                                m.invoke(this, new Object[] { rk, gk });
                            } catch (InvocationTargetException e) {
                                Throwable cause = e.getCause();
                                throw (cause instanceof Exception) ? ((Exception) cause) : e;
                            }
                            System.err.println();
                        }
                    }
                }
            }
            System.err.println(testCount + " tests" + ((errorCount == 0) ? "" : ", " + errorCount + " errors"));
            if (errorCount > 0)
                throw new Exception(errorCount + " errors found");
        } finally {
            fm.close();
        }
    }

    /**
     * Init directories for a test case.
     */
    void init(RunKind rk, GenKind gk, String name) throws IOException {
        System.err.println("Test " + rk + " " + gk + " " + name);
        testCount++;

        testDir = new File(rk.toString().toLowerCase() + "_" + gk.toString().toLowerCase() + "-" + name);
        srcDir = new File(testDir, "src");
        srcDir.mkdirs();
        classesDir = new File(testDir, "classes");
        classesDir.mkdirs();
        headersDir = new File(testDir, "headers");
        headersDir.mkdirs();
    }

    /** Create a source file with given body text. */
    File createFile(String path, final String body) throws IOException {
        File f = new File(srcDir, path);
        f.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(f)) {
            out.write(body);
        }
        return f;
    }

    /** Convenience method to create a set of items. */
    <T> Set<T> createSet(T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }

    /** Convenience method to check two values are equal, and report an error if not. */
    <T> void checkEqual(String label, T expect, T found) {
        if ((found == null) ? (expect == null) : found.equals(expect))
            return;
        System.err.println("Error: mismatch");
        System.err.println("  expected: " + expect);
        System.err.println("     found: " + found);
        errorCount++;
    }

    // Shared across API test cases
    JavacTool javac;
    StandardJavaFileManager fm;

    // Directories set up by init
    File testDir;
    File srcDir;
    File classesDir;
    File headersDir;

    // Statistics
    int testCount;
    int errorCount;
}

