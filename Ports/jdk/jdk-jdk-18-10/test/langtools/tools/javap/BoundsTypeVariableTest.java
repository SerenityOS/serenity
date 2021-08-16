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
 * @bug 8003537
 * @summary javap should not use / in Bounds Type Variables
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static java.nio.file.StandardOpenOption.*;

public class BoundsTypeVariableTest {
    public static void main(String... args) throws Exception {
        new BoundsTypeVariableTest().run();
    }

    void run() throws Exception {
        File srcDir = new File("src");
        srcDir.mkdirs();
        File classesDir = new File("classes");
        classesDir.mkdirs();
        final String expect = "public abstract <U extends java.lang.Object> U doit();";
        List<String> contents = new ArrayList<>();
        contents.add("abstract class X {");
        contents.add(expect);
        contents.add("}");

        File f = writeFile(new File(srcDir, "X.java"), contents);
        javac("-d", classesDir.getPath(), f.getPath());
        String out = javap("-p", "-v", new File(classesDir, "X.class").getPath());
        if (!out.contains(expect)) {
            throw new Exception("expected pattern not found: " + expect);
        }
    }

    File writeFile(File f, List<String> body) throws IOException {
        Files.write(f.toPath(), body, Charset.defaultCharset(),
                CREATE, TRUNCATE_EXISTING);
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
}
