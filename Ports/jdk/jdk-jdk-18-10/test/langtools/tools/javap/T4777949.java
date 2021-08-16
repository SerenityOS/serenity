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
 * @bug 4777949
 * @summary Warn javap usage on package with simple name
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.tools.javap.*;

public class T4777949 {
    public static void main(String... args) throws Exception {
        new T4777949().run();
    }

    void run() throws Exception {
        File javaFile = writeTestFile();
        File classFile = compileTestFile(javaFile);

        test(".", "p.q.r.Test", false);
        test("p", "q.r.Test", true);
        test("p/q", "r.Test", true);
        test("p/q/r", "Test", true);
        test(".", "p.q.r.Test.Inner", false);
        test(".", "p.q.r.Test$Inner", false);
        test("p", "q.r.Test.Inner", true);
        test("p", "q.r.Test$Inner", true);

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void test(String classPath, String className, boolean expectWarnings) {
        List<Diagnostic<? extends JavaFileObject>> diags =
            javap(Arrays.asList("-classpath", classPath), Arrays.asList(className));
        boolean foundWarnings = false;
        for (Diagnostic<? extends JavaFileObject> d: diags) {
            if (d.getKind() == Diagnostic.Kind.WARNING)
                foundWarnings = true;
        }
    }


    File writeTestFile() throws IOException {
        File f = new File("Test.java");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println("package p.q.r;");
        out.println("class Test { class Inner { } }");
        out.close();
        return f;
    }

    File compileTestFile(File f) {
        int rc = com.sun.tools.javac.Main.compile(new String[] { "-d", ".", f.getPath() });
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getPath();
        return new File(path.substring(0, path.length() - 5) + ".class");
    }

    List<Diagnostic<? extends JavaFileObject>> javap(List<String> args, List<String> classes) {
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        JavaFileManager fm = JavapFileManager.create(dc, pw);
        JavapTask t = new JavapTask(pw, fm, dc, args, classes);
        int ok = t.run();

        List<Diagnostic<? extends JavaFileObject>> diags = dc.getDiagnostics();

        if (ok != 0)
            error("javap failed unexpectedly");

        System.err.println("args=" + args + " classes=" + classes + "\n"
                           + diags + "\n"
                           + sw);

        return diags;
    }

    void error(String msg) {
        System.err.println("error: " + msg);
        errors++;
    }

    int errors;
}

