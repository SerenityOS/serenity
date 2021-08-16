/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6627362
 * @summary javac generates code that uses array.clone,
 *          which is not available on JavaCard
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class T6627362 {
    static String testSrc = System.getProperty("test.src", ".");

    public static void main(String... args) throws Exception {
        new T6627362().run();
    }

    public void run() throws Exception {
        testStandard();
        testNoClone();
        if (errors > 0)
            throw new Error(errors + " test cases failed");
    }

    void testStandard() throws Exception {
        // compile and disassemble E.java, check for reference to Object.clone()
        File x = new File(testSrc, "x");
        String[] jcArgs = { "-d", ".",
                            new File(x, "E.java").getPath() };
        compile(jcArgs);

        String[] jpArgs = { "-classpath", ".", "-c", "E" };

        StringWriter sw = new StringWriter();
        javap(new PrintWriter(sw, true), jpArgs);
        check(sw.toString(), "Method \"[LE;\".clone:()Ljava/lang/Object;");
        callValues();
    }

    void testNoClone() throws Exception {
        // compile and disassemble E.java, using modified Object.java,
        // check for reference to System.arraycopy
        File x = new File(testSrc, "x");
        String[] jcArgs = { "-d", ".", "--patch-module", "java.base=" + x.getAbsolutePath(),
                            new File(x, "E.java").getPath(),
                            new File(new File(new File(x, "java"), "lang"), "Object.java").getPath()};
        compile(jcArgs);

        String[] jpArgs = { "-classpath", ".", "-c", "E" };

        StringWriter sw = new StringWriter();
        javap(new PrintWriter(sw, true), jpArgs);
        check(sw.toString(), "// Method java/lang/System.arraycopy:(Ljava/lang/Object;ILjava/lang/Object;II)V");
        callValues();
    }

    void compile(String... args) {
        int rc = com.sun.tools.javac.Main.compile(args);
        if (rc != 0)
            throw new Error("javac failed: " + Arrays.asList(args) + ": " + rc);
    }

    void javap(PrintWriter out, String... args) throws Exception {
        int rc = com.sun.tools.javap.Main.run(args, out);
        if (rc != 0)
            throw new Error("javap failed: " + Arrays.asList(args) + ": " + rc);
    }

    void check(String s, String require) {
        System.out.println("Checking:\n" + s);
        if (s.indexOf(require) == -1) {
            System.err.println("Can't find " + require);
            errors++;
        }
    }

    void callValues() {
        try {
            File dot = new File(System.getProperty("user.dir"));
            ClassLoader cl = new URLClassLoader(new URL[] { dot.toURL() });
            Class<?> e_class = cl.loadClass("E");
            Method m = e_class.getMethod("values", new Class[] { });
            //System.err.println(m);
            Object o = m.invoke(null, (Object[]) null);
            List<Object> v = Arrays.asList((Object[]) o);
            if (!v.toString().equals("[a, b, c]"))
                throw new Error("unexpected result for E.values(): " + v);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    int errors;
}

