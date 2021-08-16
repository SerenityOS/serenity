/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6960424 8022161
 * @summary new option -Xpkginfo for better control of when package-info.class
 *          is generated, also ensures no failures if package-info.java is
 *          not available.
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class TestPkgInfo {
    enum OptKind {
        NONE(null),
        ALWAYS("-Xpkginfo:always"),
        NONEMPTY("-Xpkginfo:nonempty"),
        LEGACY("-Xpkginfo:legacy");
        OptKind(String opt) { this.opt = opt; }
        final String opt;
    };

    public static void main(String... args) throws Exception {
        new TestPkgInfo().run(args);
    }
    public void run(String... args) throws Exception {
        testPositive();
        testNoExceptions();
    }
    public void testPositive(String... args) throws Exception {
        boolean[] booleanValues = { false, true };
        for (OptKind ok: OptKind.values()) {
            for (boolean sr: booleanValues) {
                for (boolean cr: booleanValues) {
                    for (boolean rr: booleanValues) {
                        try {
                            test(ok, sr, cr, rr);
                        } catch (Exception e) {
                            error("Exception: " + e);
                        }
                        if (errors > 0) throw new AssertionError();
                    }
                }
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    /** this should throw no exceptions **/
    void testNoExceptions() throws Exception {
        count++;
        System.err.println("Test " + count + ": ALWAYS nofile");

        StringBuilder sb = new StringBuilder();
        sb.append("package test; class Hello{}");

        // test specific tmp directory
        File tmpDir = new File("tmp.test" + count);
        File classesDir = new File(tmpDir, "classes");
        classesDir.mkdirs();
        File javafile = new File(new File(tmpDir, "src"), "Hello.java");
        writeFile(javafile, sb.toString());
        // build up list of options and files to be compiled
        List<String> opts = new ArrayList<>();
        List<File> files = new ArrayList<>();

        opts.add("-d");
        opts.add(classesDir.getPath());
        opts.add("-Xpkginfo:always");
        files.add(javafile);

        compile(opts, files);
    }

    void test(OptKind ok, boolean sr, boolean cr, boolean rr) throws Exception {
        count++;
        System.err.println("Test " + count + ": ok:" + ok + " sr:" + sr + " cr:" + cr + " rr:" + rr);

        StringBuilder sb = new StringBuilder();

        // create annotated package statement with all combinations of retention policy
        if (sr) sb.append("@SR\n");
        if (cr) sb.append("@CR\n");
        if (rr) sb.append("@RR\n");
        sb.append("package p;\n");
        sb.append("\n");

        sb.append("import java.lang.annotation.*;\n");
        sb.append("@Retention(RetentionPolicy.SOURCE)  @interface SR { }\n");
        sb.append("@Retention(RetentionPolicy.CLASS)   @interface CR { }\n");
        sb.append("@Retention(RetentionPolicy.RUNTIME) @interface RR { }\n");

        // test specific tmp directory
        File tmpDir = new File("tmp.test" + count);
        File classesDir = new File(tmpDir, "classes");
        classesDir.mkdirs();
        File pkginfo_java = new File(new File(tmpDir, "src"), "package-info.java");
        writeFile(pkginfo_java, sb.toString());

        // build up list of options and files to be compiled
        List<String> opts = new ArrayList<>();
        List<File> files = new ArrayList<>();

        opts.add("-d");
        opts.add(classesDir.getPath());
        if (ok.opt != null)
            opts.add(ok.opt);
        //opts.add("-verbose");
            files.add(pkginfo_java);

        compile(opts, files);

        File pkginfo_class = new File(new File(classesDir, "p"), "package-info.class");
        boolean exists = pkginfo_class.exists();

        boolean expected;
        switch (ok) {
            case ALWAYS:
                expected = true;
                break;

            case LEGACY:
            case NONE:
                expected = (sr || cr || rr ); // any annotation
                break;

            case NONEMPTY:
                expected = (cr || rr ); // any annotation in class file
                break;

            default:
                throw new IllegalStateException();
        }

        if (exists && !expected)
            error("package-info.class found but not expected");
        if (!exists && expected)
            error("package-info.class expected but not found");
    }

    /** Compile files with options provided. */
    void compile(List<String> opts, List<File> files) throws Exception {
        System.err.println("javac: " + opts + " " + files);
        List<String> args = new ArrayList<>();
        args.addAll(opts);
        for (File f: files)
            args.add(f.getPath());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.flush();
        if (sw.getBuffer().length() > 0)
            System.err.println(sw.toString());
        if (rc != 0)
            throw new Exception("compilation failed: rc=" + rc);
    }

    /** Write a file with a given body. */
    void writeFile(File f, String body) throws Exception {
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        Writer out = new FileWriter(f);
        try {
            out.write(body);
        } finally {
            out.close();
        }
    }

    /** Report an error. */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    /** Test case counter. */
    int count;

    /** Number of errors found. */
    int errors;
}
