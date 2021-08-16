/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032814
 * @summary LineNumberTable/LocalVariableTable tables duplication for the
 *          "-v -l" combination of options
 * @modules jdk.jdeps/com.sun.tools.javap
 * @compile -g T8032814.java
 * @run main T8032814
 */

import java.io.*;
import java.util.*;

public class T8032814 {
    public static void main(String... args) throws Exception {
        new T8032814().run();
    }

    void run() throws Exception {
        Class<?> clazz = T8032814.class;
        int count = clazz.getDeclaredConstructors().length
                + clazz.getDeclaredMethods().length;
        test(clazz, 0);
        test(clazz, count, "-v");
        test(clazz, count, "-l");
        test(clazz, count, "-v", "-l");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(Class<?> clazz, int expectedCount, String... opts) throws Exception {
        System.err.println("test class " + clazz.getName() + " " + Arrays.asList(opts) + ": expect: " + expectedCount);
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(opts));
        args.addAll(Arrays.asList("-classpath", System.getProperty("test.classes")));
        args.add(clazz.getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args.toArray(new String[args.size()]), pw);
        pw.close();
        String out = sw.toString();
        if (rc != 0)
            throw new Exception("javap failed unexpectedly: rc=" + rc);

        int lntCount = 0, lvtCount = 0;
        for (String line: out.split("[\r\n]+")) {
            if (line.matches("^ *LineNumberTable:$"))
                lntCount++;
            if (line.matches("^ *LocalVariableTable:$"))
                lvtCount++;
        }
        checkEqual("LineNumberTable", lntCount, expectedCount);
        checkEqual("LocalVariableTable", lvtCount, expectedCount);
    }

    void checkEqual(String attr, int found, int expect) {
        if (found != expect) {
            error("Unexpected number of occurrences of " + attr + "\n" +
                "found: " + found + ", expected: " + expect);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors = 0;
}

