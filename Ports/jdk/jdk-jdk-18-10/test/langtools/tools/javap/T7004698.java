/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7004698
 * @summary javap does not output CharacterRangeTable attributes correctly
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class T7004698 {
    public static void main(String... args) throws Exception {
        new T7004698().run();
    }

    void run() throws Exception {
        File srcDir = new File(System.getProperty("test.src"));
        File srcFile = new File(srcDir, T7004698.class.getSimpleName() + ".java");
        File classesDir = new File(".");
        compile("-Xjcov",
                "--add-modules", "jdk.jdeps",
                "--add-exports", "jdk.jdeps/com.sun.tools.javap=ALL-UNNAMED",
                "-d", classesDir.getPath(),
                srcFile.getPath());

        File classFile = new File(classesDir, T7004698.class.getSimpleName() + ".class");
        String out = javap("-v", classFile.getPath());

        Pattern attrBody = Pattern.compile("[0-9a-f, ]+//[-0-9a-z:, ]+");
        Pattern endOfAttr = Pattern.compile("(^$|[A-Z][A-Za-z0-9_]+:.*|})");
        boolean inAttr = false;
        int count = 0;
        int errors = 0;
        for (String line: out.split(System.getProperty("line.separator"))) {
            line = line.trim();
            if (line.equals("CharacterRangeTable:")) {
                inAttr = true;
                count++;
            } else if (inAttr) {
                if (endOfAttr.matcher(line).matches()) {
                    inAttr = false;
                } else if (!attrBody.matcher(line).matches()) {
                    System.err.println("unexpected line found: " + line);
                    errors++;
                }
            }
        }
        if (count == 0)
            throw new Exception("no attribute instances found");

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void compile(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("javac failed unexpectedly; rc=" + rc);
    }

    String javap(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("javap failed unexpectedly; rc=" + rc);
        return out;
    }
}
