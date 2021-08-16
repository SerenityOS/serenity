/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8007052
 * @summary javap should include the descriptor for a method in verbose mode
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

public class DescriptorTest {
    public static void main(String... args) throws Exception {
        new DescriptorTest().run();
    }

    void run() throws Exception {
        File srcDir = new File("src");
        srcDir.mkdirs();
        File classesDir = new File("classes");
        classesDir.mkdirs();

        File f = writeFile(new File(srcDir, "E.java"), "enum E { A, B }");
        javac("-d", classesDir.getPath(), f.getPath());
        String out = javap("-p", "-v", new File(classesDir, "E.class").getPath());
        Pattern expect = Pattern.compile("\\Qprivate E();\\E\\s+\\Qdescriptor: (Ljava/lang/String;I)V\\E");
        checkContains(out, expect);
    }

    File writeFile(File f, String body) throws IOException {
        try (FileWriter out = new FileWriter(f)) {
            out.write(body);
        }
        return f;
    }

    void javac(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.flush();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("compilation failed");
    }

    String javap(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.flush();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("javap failed");
        return out;
    }

    void checkContains(String s, Pattern p) throws Exception {
        if (!p.matcher(s).find())
            throw new Exception("expected pattern not found: " + p);
    }
}
