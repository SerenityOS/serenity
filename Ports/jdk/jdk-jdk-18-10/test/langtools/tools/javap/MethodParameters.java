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
 * @bug 8004727 8005647
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class MethodParameters {

    static final String Foo_name = "Foo";
    static final String Foo_contents =
        ("public class Foo {\n" +
         "  Foo() {}\n" +
         "  Foo(int i) {}\n" +
         "  void foo0() {}\n" +
         "  void foo2(int j, int k) {}\n" +
         "}").replaceAll(" +", " ");

    static final String Init0_expected =
        ("  Foo();\n" +
         "    descriptor: ()V\n" +
         "    flags: (0x0000)\n" +
         "    Code:\n" +
         "      stack=1, locals=1, args_size=1\n" +
         "         0: aload_0\n" +
         "         1: invokespecial #1                  // Method java/lang/Object.\"<init>\":()V\n" +
         "         4: return\n" +
         "      LineNumberTable:\n" +
         "        line 2: 0").replaceAll(" +", " ");

    static final String Init1_expected =
        ("  Foo(int);\n" +
         "    descriptor: (I)V\n" +
         "    flags: (0x0000)\n" +
         "    Code:\n" +
         "      stack=1, locals=2, args_size=2\n" +
         "         0: aload_0\n" +
         "         1: invokespecial #1                  // Method java/lang/Object.\"<init>\":()V\n" +
         "         4: return\n" +
         "      LineNumberTable:\n" +
         "        line 3: 0\n" +
         "    MethodParameters:\n" +
         "      Name                                Flags\n" +
         "      i").replaceAll(" +", " ");

    static final String foo0_expected =
        ("  void foo0();\n" +
         "    descriptor: ()V\n" +
         "    flags: (0x0000)\n" +
         "    Code:\n" +
         "      stack=0, locals=1, args_size=1\n" +
         "         0: return\n" +
         "      LineNumberTable:\n" +
         "        line 4: 0").replaceAll(" +", " ");

    static final String foo2_expected =
        ("  void foo2(int, int);\n" +
         "    descriptor: (II)V\n" +
         "    flags: (0x0000)\n" +
         "    Code:\n" +
         "      stack=0, locals=3, args_size=3\n" +
         "         0: return\n" +
         "      LineNumberTable:\n" +
         "        line 5: 0\n" +
         "    MethodParameters:\n" +
         "      Name                                Flags\n" +
         "      j\n" +
         "      k").replaceAll(" +", " ");

    static final File classesdir = new File("methodparameters");
    static final String separator = System.getProperty("line.separator");

    public static void main(String... args) throws Exception {
        new MethodParameters().run();
    }

    void run() throws Exception {
        classesdir.mkdir();
        final File Foo_java =
            writeFile(classesdir, Foo_name + ".java", Foo_contents);
        compile("-parameters", "-d", classesdir.getPath(), Foo_java.getPath());
        System.out.println("Run javap");
        String output =
            javap("-c", "-verbose", "-classpath",
                  classesdir.getPath(), Foo_name);
        String normalized =
            output.replaceAll(separator, "\n").replaceAll(" +", " ");

        if (!normalized.contains(Init0_expected))
            error("Bad output for zero-parameter constructor.  Expected\n" +
                  Init0_expected + "\n" + "but got\n" + normalized);
        if (!normalized.contains(Init1_expected))
           error("Bad output for one-parameter constructor.  Expected\n" +
                 Init1_expected + "\n" + "but got\n" + normalized);
        if (!normalized.contains(foo0_expected))
           error("Bad output for zero-parameter method.  Expected\n" +
                 foo0_expected + "\n" + "but got\n" + normalized);
        if (!normalized.contains(foo2_expected))
           error("Bad output for two-parameter method  Expected\n" +
                 foo2_expected + "\n" + "but got\n" + normalized);

        if (0 != errors)
            throw new Exception("MethodParameters test failed with " +
                                errors + " errors");
    }

    String javap(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        //sun.tools.javap.Main.entry(args);
        int rc = com.sun.tools.javap.Main.run(args, out);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        out.close();
        System.out.println(sw);
        return sw.toString();
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
            error("compilation failed, rc=" + rc);
        return out;
    }

    File writeFile(File dir, String path, String body) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

}
