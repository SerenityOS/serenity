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
 * @bug 4870651 6715757
 * @summary javap should recognize generics, varargs, enum;
 *          javap prints "extends java.lang.Object"
 * @modules jdk.jdeps/com.sun.tools.javap
 * @build T4870651 Test
 * @run main T4870651
 */

import java.io.*;

public class T4870651 {
    public static void main(String[] args) throws Exception {
        new T4870651().run();
    }

    public void run() throws IOException {
        verify("Test",
               "class Test<T extends java.lang.Object, " +
                   "E extends java.lang.Exception & java.lang.Comparable<T>, " +
                   "U extends java.lang.Comparable>",
               "v1(java.lang.String...)");

        verify("Test$Enum",
               "flags: (0x4030) ACC_FINAL, ACC_SUPER, ACC_ENUM",
               "flags: (0x4019) ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_ENUM");

        if (errors > 0)
            throw new Error(errors + " found.");
    }

    String javap(String className) {
        String testClasses = System.getProperty("test.classes", ".");
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        String[] args = { "-classpath", testClasses, "-v", className };
        int rc = com.sun.tools.javap.Main.run(args, out);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        out.close();
        String output = sw.toString();
        System.out.println("class " + className);
        System.out.println(output);
        return output;
    }

    void verify(String className, String... expects) {
        String output = javap(className);
        for (String expect: expects) {
            if (output.indexOf(expect)< 0)
                error(expect + " not found");
        }
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}
