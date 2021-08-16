/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6627364 6627366
 * @summary Synthesize important classes if they are missing from the (boot)classpath
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class Main
{
    File testSrc = new File(System.getProperty("test.src"));

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    public void run() throws Exception {

        // compile with standard bootclasspath
        compile(true, "Test.java");

        // compile with various missing system classes

        List<String> base_files = Arrays.asList(
            "Boolean.java",
            "Byte.java",
            "Character.java",
            "Integer.java",
            "Long.java",
            "Number.java",
            "Object.java",
            "Short.java",
            "Void.java"
        );

        List<String> extra_files = Arrays.asList(
            "Double.java",
            "Float.java",
            "Cloneable.java",
            "Serializable.java"
        );

        List<String> files = new ArrayList<String>();
        files.addAll(base_files);
        files.add("Test.java");

        compile(false, files);

        for (String f: extra_files) {
            files = new ArrayList<String>();
            files.addAll(base_files);
            files.addAll(extra_files);
            files.remove(f);
            files.add("Test.java");
            compile(false, files);
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void compile(boolean stdBootClassPath, String... files) {
        compile(stdBootClassPath, Arrays.asList(files));
    }

    void compile(boolean stdBootClassPath, List<String> files) {
        File empty = new File("empty");
        empty.mkdirs();

        // files to compile are in a separate directory from test to avoid
        // confusing jtreg
        File src = new File(testSrc, "src");

        List<String> args = new ArrayList<String>();
        args.add("-classpath");
        args.add("empty");

        if (stdBootClassPath) {
            args.add("--patch-module");
            args.add("java.base=" + testSrc.getAbsolutePath());
        } else {
            args.add("--system");
            args.add("none");
            files.add("module-info.java");
        }


        args.add("-d");
        args.add(".");

        for (String f: files)
            args.add(new File(src, f).getPath());

        System.out.println("Compile: " + args);
        StringWriter out = new StringWriter();
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]),
                                                  new PrintWriter(out));
        System.out.println(out.toString());
        System.out.println("result: " + rc);
        System.out.println();

        if (rc != 0)
            errors++;
    }

    private int errors;
}


