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
 * @summary tests for module resolution
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ResolveTest
 */

import java.nio.file.*;

import toolbox.JavacTask;
import toolbox.Task;

public class ResolveTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ResolveTest t = new ResolveTest();
        t.runTests();
    }

    @Test
    public void testMissingSimpleTypeUnnamedModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class C { D d; }");

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C.java:1:11: compiler.err.cant.resolve.location: "
                + "kindname.class, D, , , (compiler.misc.location: kindname.class, C, null)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testMissingSimpleTypeNamedModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { }",
                "class C { D d; }");

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C.java:1:11: compiler.err.cant.resolve.location: "
                + "kindname.class, D, , , (compiler.misc.location: kindname.class, C, null)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testUnexportedTypeUnreadableModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { }",
                "package p2; public class C2 { p1.C1 c; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C2.java:1:31: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.does.not.read: m2x, p1, m1x)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testUnexportedTypeReadableModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { requires m1x; }",
                "package p2; public class C2 { p1.C1 c; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C2.java:1:31: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.not.exported: p1, m1x)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testQualifiedExportedTypeReadableModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { exports p1 to m3x; }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { requires m1x; }",
                "package p2; public class C2 { p1.C1 c; }");
        tb.writeJavaFiles(src.resolve("m3x"),
                "module m3x { requires m1x; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C2.java:1:31: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.not.exported.to.module: p1, m1x, m2x)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testExportedTypeUnreadableModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { exports p1; }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { }",
                "package p2; public class C2 { p1.C1 c; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C2.java:1:31: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.does.not.read: m2x, p1, m1x)"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testExportedTypeReadableModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { exports p1; }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { requires m1x; }",
                "package p2; public class C2 { p1.C1 c; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testExportedTypeReadableModule2(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                "module m1x { exports p1 to m2x; }",
                "package p1; public class C1 { }");
        tb.writeJavaFiles(src.resolve("m2x"),
                "module m2x { requires m1x; }",
                "package p2; public class C2 { p1.C1 c; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }
}
