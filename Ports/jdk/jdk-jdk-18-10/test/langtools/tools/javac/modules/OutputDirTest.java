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
 * @summary tests for output directory
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main OutputDirTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class OutputDirTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        new OutputDirTest().run();
    }

    Path src;

    void run() throws Exception {
        tb = new ToolBox();

        src = Paths.get("src");
        tb.writeJavaFiles(src.resolve("m"),
                "module m { }",
                "package p; class C { }");

        runTests();
    }

    @Test
    public void testError(Path base) throws Exception {
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("- compiler.err.no.output.dir"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testProcOnly(Path base) throws IOException {
        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "-proc:only",
                        "--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }

    @Test
    public void testClassOutDir(Path base) throws IOException {
        Path classes = base.resolve("classes");
        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "-d", classes.toString(),
                        "--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }

    @Test
    public void testExplodedOutDir(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        tb.writeJavaFiles(modSrc,
                "module m1x { exports p; }",
                "package p; public class CC { }");
        Path modClasses = base.resolve("modClasses");
        Files.createDirectories(modClasses);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(modClasses)
                .files(findJavaFiles(modSrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");
        Path src_m = src.resolve("m");
        tb.writeJavaFiles(src_m,
                "module m { requires m1x ; }",
                "class C { }");

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(modClasses) // an exploded module
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("- compiler.err.multi-module.outdir.cannot.be.exploded.module: " + modClasses.toString()))
            throw new Exception("expected output not found");
    }

    @Test
    public void testInExplodedOutDir(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        tb.writeJavaFiles(modSrc,
                "module m1x { exports p; }",
                "package p; public class CC { }");
        Path modClasses = base.resolve("modClasses");
        Files.createDirectories(modClasses);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(modClasses)
                .files(findJavaFiles(modSrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { requires m1x ; }",
                "class C { }");

        Path classes = modClasses.resolve("m");
        Files.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(classes) // within an exploded module
                .options("-XDrawDiagnostics",
                        "-Xlint", "-Werror",
                        "--module-path", modClasses.toString())
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("- compiler.warn.outdir.is.in.exploded.module: " + classes.toString()))
            throw new Exception("expected output not found");
    }
}
