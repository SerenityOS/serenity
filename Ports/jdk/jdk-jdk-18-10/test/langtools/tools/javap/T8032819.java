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
 * @bug 8032819
 * @summary Extra empty line between field declarations for the "-v -c" and "-v -l" combination of options
 * @modules jdk.jdeps/com.sun.tools.javap
 * @compile -g T8032819.java
 * @run main T8032819
 */

import java.io.*;
import java.util.*;

public class T8032819 {
    static class Fields {
        int f1;
        int f2;
    }

    public static void main(String... args) throws Exception {
        new T8032819().run();
    }

    void run() throws Exception {
        Class<?> clazz = Fields.class;
        test(clazz);
        test(clazz, "-c");
        test(clazz, "-l");
        test(clazz, "-l", "-c");
        test(clazz, "-v");
        test(clazz, "-v", "-c");
        test(clazz, "-v", "-l");
        test(clazz, "-v", "-l", "-c");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    static final String sep = System.getProperty("line.separator");
    static final String doubleBlankLine = sep + sep + sep;

    void test(Class<?> clazz, String... opts) throws Exception {
        System.err.println("test " + Arrays.asList(opts));
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

        int count = 0;
        int i = out.indexOf(doubleBlankLine, 0);
        while (i != -1) {
            count++;
            i = out.indexOf(doubleBlankLine, i + doubleBlankLine.length());
        }

        if (count > 0)
            error(count + " double blank lines found");
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors = 0;
}

