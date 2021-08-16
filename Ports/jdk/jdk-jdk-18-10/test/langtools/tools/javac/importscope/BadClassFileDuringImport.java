/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198378
 * @summary Verify that BadClassFile related to imports are handled properly.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main BadClassFileDuringImport
 */

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class BadClassFileDuringImport {
    public static void main(String... args) throws Exception {
        new BadClassFileDuringImport().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        new JavacTask(tb)
          .outdir(".")
          .sources("package p; public class A { }",
                   "package p; public class B { public static class I { } }")
          .run()
          .writeAll();

        try (OutputStream out = Files.newOutputStream(Paths.get(".", "p", "A.class"))) {
            out.write("broken".getBytes("UTF-8"));
        }

        try (OutputStream out = Files.newOutputStream(Paths.get(".", "p", "B$I.class"))) {
            out.write("broken".getBytes("UTF-8"));
        }

        doTest("import p.A;",
               "",
               "Test.java:2:9: compiler.err.cant.access: p.A, (compiler.misc.bad.class.file.header: A.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.A;",
               "A a;",
               "Test.java:2:9: compiler.err.cant.access: p.A, (compiler.misc.bad.class.file.header: A.class, (compiler.misc.illegal.start.of.class.file))",
               "Test.java:2:33: compiler.err.cant.resolve.location: kindname.class, A, , , (compiler.misc.location: kindname.class, Test, null)",
               "2 errors");
        doTest("import p.A;",
               "void test() { A a; }",
               "Test.java:2:9: compiler.err.cant.access: p.A, (compiler.misc.bad.class.file.header: A.class, (compiler.misc.illegal.start.of.class.file))",
               "Test.java:2:47: compiler.err.cant.resolve.location: kindname.class, A, , , (compiler.misc.location: kindname.class, Test, null)",
               "2 errors");
        doTest("import p.*;",
               "",
               (String[]) null);
        doTest("import p.*;",
               "A a;",
               "Test.java:2:33: compiler.err.cant.access: p.A, (compiler.misc.bad.class.file.header: A.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.*;",
               "void test() { A a; }",
               "Test.java:2:47: compiler.err.cant.access: p.A, (compiler.misc.bad.class.file.header: A.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");

        doTest("import p.B.I;",
               "",
               "Test.java:2:11: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.B.I;",
               "I i;",
               "Test.java:2:11: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.B.I;",
               "void test() { I i; }",
               "Test.java:2:11: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.B.*;",
               "",
               (String[]) null);
        doTest("import p.B.*;",
               "I i;",
               "Test.java:2:35: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import p.B.*;",
               "void test() { I i; }",
               "Test.java:2:49: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");

        doTest("import static p.B.I;",
               "",
               "Test.java:2:1: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import static p.B.I;",
               "I i;",
               "Test.java:2:42: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import static p.B.I;",
               "void test() { I i; }",
               "Test.java:2:1: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import static p.B.*;",
               "",
               "Test.java:2:1: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import static p.B.*;",
               "I i;",
               "Test.java:2:42: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
        doTest("import static p.B.*;",
               "void test() { I i; }",
               "Test.java:2:1: compiler.err.cant.access: p.B.I, (compiler.misc.bad.class.file.header: B$I.class, (compiler.misc.illegal.start.of.class.file))",
               "1 error");
    }

    void doTest(String importText, String useText, String... expectedOutput) {
        List<String> log = new JavacTask(tb)
                .classpath(".")
                .sources("\n" + importText + " public class Test { " + useText + " }")
                .options("-XDrawDiagnostics")
                .run(expectedOutput != null ? Task.Expect.FAIL : Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (expectedOutput != null && !log.equals(Arrays.asList(expectedOutput))) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }
}
