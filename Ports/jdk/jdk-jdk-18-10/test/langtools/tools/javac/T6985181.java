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
 * @bug 6985181
 * @summary Annotations lost from classfile
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class T6985181 {
    public static void main(String... args) throws Exception{
        new T6985181().run();
    }

    public void run() throws Exception {
        String code = "@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_PARAMETER)\n" +
                "@interface Simple { }\n" +
                "interface Test<@Simple T> { }";

        File srcFile = writeFile("Test.java", code);
        File classesDir = new File("classes");
        classesDir.mkdirs();
        compile("-d", classesDir.getPath(), srcFile.getPath());
        String out = javap(new File(classesDir, srcFile.getName().replace(".java", ".class")));
        if (!out.contains("RuntimeInvisibleTypeAnnotations"))
            throw new Exception("RuntimeInvisibleTypeAnnotations not found");
    }

    void compile(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        if (rc != 0)
            throw new Exception("Compilation failed: rc=" + rc);
    }

    String javap(File classFile) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "-v", classFile.getPath() };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        if (rc != 0)
            throw new Exception("javap failed: rc=" + rc);
        return out;
    }

    File writeFile(String path, String body) throws IOException {
        File f = new File(path);
        FileWriter out = new FileWriter(f);
        try {
            out.write(body);
        } finally {
            out.close();
        }
        return f;
    }
}
