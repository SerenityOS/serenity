/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests for "requires transitive"
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main RequiresTransitiveTest
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class RequiresTransitiveTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        RequiresTransitiveTest t = new RequiresTransitiveTest();
        t.runTests();
    }

    @Test
    public void testJavaSE_OK(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { requires java.se; }",
                // use class in java.se
                """
                    import java.awt.Frame;
                    class Test {
                        Frame f;
                    }""");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .files(findJavaFiles(src))
                .outdir(classes)
                .run()
                .writeAll();
    }

    @Test
    public void testJavaSE_Fail(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { requires java.se; }",
                // use class not in java.se (in jdk.compiler)
                """
                    import com.sun.source.tree.Tree;
                    class Test {
                        Tree t;
                    }""");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .outdir(classes)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("Test.java:1:22: compiler.err.package.not.visible: com.sun.source.tree, (compiler.misc.not.def.access.does.not.read: m, com.sun.source.tree, jdk.compiler)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testComplex_OK(Path base) throws Exception {
        Path src = getComplexSrc(base, "", "");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .outdir(classes)
                .run()
                .writeAll();
    }

    @Test
    public void testComplex_Fail(Path base) throws Exception {
        Path src = getComplexSrc(base,
                "import p5.C5; import p6.C6; import p7.C7;\n",
                "C5 c5; C6 c6; C7 c7;\n");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .outdir(classes)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String[] expect = {
            "C1.java:5:8: compiler.err.package.not.visible: p5, (compiler.misc.not.def.access.does.not.read: m1x, p5, m5x)",
            "C1.java:5:22: compiler.err.package.not.visible: p6, (compiler.misc.not.def.access.does.not.read: m1x, p6, m6x)",
            "C1.java:5:36: compiler.err.package.not.visible: p7, (compiler.misc.not.def.access.does.not.read: m1x, p7, m7x)"
        };

        for (String e: expect) {
            if (!log.contains(e))
                throw new Exception("expected output not found: " + e);
        }
    }

    /*
     * Set up the following module graph
     *     m1x -> m2x => m3x => m4x -> m5x
     *              -> m6x => m7x
     * where -> is requires, => is requires transitive
     */
    Path getComplexSrc(Path base, String m1_extraImports, String m1_extraUses) throws Exception {
        Path src = base.resolve("src");

        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                "module m1x { requires m2x; }",
                """
                    package p1;
                    import p2.C2;
                    import p3.C3;
                    import p4.C4;
                    """
                + m1_extraImports
                + """
                    class C1 {
                      C2 c2; C3 c3; C4 c4;
                    """
                + m1_extraUses
                + "}\n");

        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                """
                    module m2x {
                      requires transitive m3x;
                      requires        m6x;
                      exports p2;
                    }""",
                """
                    package p2;
                    public class C2 { }
                    """);

        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                """
                    module m3x { requires transitive m4x; exports p3; }
                    """,
                """
                    package p3;
                    public class C3 { }
                    """);

        Path src_m4 = src.resolve("m4x");
        tb.writeJavaFiles(src_m4,
                """
                    module m4x { requires m5x; exports p4; }
                    """,
                """
                    package p4;
                    public class C4 { }
                    """);

        Path src_m5 = src.resolve("m5x");
        tb.writeJavaFiles(src_m5,
                """
                    module m5x { exports p5; }
                    """,
                """
                    package p5;
                    public class C5 { }
                    """);

        Path src_m6 = src.resolve("m6x");
        tb.writeJavaFiles(src_m6,
                "module m6x { requires transitive m7x; exports p6; }",
                """
                    package p6;
                    public class C6 { }""");

        Path src_m7 = src.resolve("m7x");
        tb.writeJavaFiles(src_m7,
                "module m7x { exports p7; }",
                """
                    package p7;
                    public class C7 { }""");

        return src;
    }
}
