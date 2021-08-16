/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

/*
 * @test
 * @bug 6824493
 * @summary experimental support for additional info for instructions
 * @modules jdk.jdeps/com.sun.tools.javap
 * @compile -g T6824493.java
 * @run main T6824493
 */
public class T6824493 {
    public static void main(String... args) {
        new T6824493().run();
    }

    void run() {
        // for each of the options, we run javap and check for some
        // marker strings in the output that generally indicate the
        // presence of the expected output, without being as specific
        // as a full golden file test.
        test("-XDdetails:source",
            "for (int i = 0; i < 10; i++) {",
            "System.out.println(s + i);");

        test("-XDdetails:tryBlocks",
                "try[0]",
                "end try[0]",
                "catch[0]");

        test("-XDdetails:stackMaps",
                "StackMap locals:  this java/lang/String int",
                "StackMap stack:  java/lang/Throwable");

        test("-XDdetails:localVariables",
                "start local 3 // java.util.List list",
                "end local 3 // java.util.List list");

        test("-XDdetails:localVariableTypes",
                "start generic local 3 // java.util.List<java.lang.String> list",
                "end generic local 3 // java.util.List<java.lang.String> list");

        if (errors > 0)
            throw new Error(errors + " errors found");
    }

    void test(String option, String... expect) {
        String[] args = {
            "-c",
            "-classpath",
            testSrc + File.pathSeparator + testClasses,
            option,
            "Test"
        };
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        if (rc != 0) {
            error("unexpected return code from javap: " + rc);
            return;
        }

        String out = sw.toString();
        System.out.println(out);
        for (String e: expect) {
            if (!out.contains(e))
                error("Not found: " + e);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    private int errors;
    private String testSrc = System.getProperty("test.src", ".");
    private String testClasses = System.getProperty("test.classes", ".");
}

class Test {
    void m(String s) {
        for (int i = 0; i < 10; i++) {
            try {
                List<String> list = null;
                System.out.println(s + i);
            } catch (NullPointerException e) {
                System.out.println("catch NPE");
            } finally {
                System.out.println("finally");
            }
        }
    }
}
