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
 * @bug 6999210
 * @summary javac should be able to warn of anomalous conditions in classfiles
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class T6999210 {
    public static void main(String... args) throws Exception {
        new T6999210().run();
    }

    void run() throws Exception {
        File srcDir = new File("src");
        File classesDir = new File("classes");
        classesDir.mkdirs();

        File c_java = writeFile(srcDir, "C.java", "class C<T> { }");
        compile("-d", classesDir.getPath(), c_java.getPath());
        File c_class = new File(classesDir, "C.class");
        setMajorVersion(c_class, 48);
        File d_java = writeFile(srcDir, "D.java", "class D { C c; }");

        // verify no warning if -Xlint:classfile not enabled
        String out1 = compile(
            "-d", classesDir.getPath(),
            "-classpath", classesDir.getPath(),
            d_java.getPath());
        if (out1.length() > 0)
            error("unexpected output from javac");

        // sanity check of warning when -XDrawDiagnostics not used
        String out2 = compile(
            "-d", classesDir.getPath(),
            "-classpath", classesDir.getPath(),
            "-Xlint:classfile",
            d_java.getPath());
        if (!out2.contains("[classfile]"))
            error("expected output \"[classfile]\" not found");

        // check specific details, using -XDrawDiagnostics
        String out3 = compile(
            "-d", classesDir.getPath(),
            "-classpath", classesDir.getPath(),
            "-Xlint:classfile", "-XDrawDiagnostics",
            d_java.getPath());
        String expect = "C.class:-:-: compiler.warn.future.attr: Signature, 49, 0, 48, 0";
        if (!out3.contains(expect))
            error("expected output \"" + expect + "\" not found");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    String compile(String... args) throws Exception {
        System.err.println("compile: " + Arrays.asList(args));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        if (rc != 0)
            throw new Exception("compilation failed, rc=" + rc);
        return out;
    }

    void setMajorVersion(File f, int major) throws IOException {
        int len = (int) f.length();
        byte[] data = new byte[len];
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            in.readFully(data);
        }
        // u4 magic
        // u2 minor
        data[6] = (byte) (major >> 8);
        data[7] = (byte) (major & 0xff);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }

    File writeFile(File dir, String path, String body) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(f)) {
            out.write(body);
        }
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
