/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the --limit-modules option
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main LimitModulesTest
 */


import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;

public class LimitModulesTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        new LimitModulesTest().runTests();
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--limit-modules", "")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("error: no value for --limit-modules option"))
            throw new Exception("expected output not found");

        log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--limit-modules=")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("error: no value for --limit-modules option"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testEmptyItem(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--limit-modules", ",m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--limit-modules", "m1x,,m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--limit-modules", "m1x,")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testEmptyList(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--limit-modules", ",")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("error: bad value for --limit-modules option"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testInvalidName(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--limit-modules", "BadModule!")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("- compiler.err.bad.name.for.option: --limit-modules, BadModule!"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testLastOneWins(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package p; class C { com.sun.tools.javac.Main main; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        System.err.println("case 1:");
        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--limit-modules", "java.base",
                         "--limit-modules", "jdk.compiler")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        System.err.println("case 2:");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--limit-modules", "jdk.compiler",
                         "--limit-modules", "java.base")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C.java:1:35: compiler.err.package.not.visible: com.sun.tools.javac, (compiler.misc.not.def.access.does.not.read.from.unnamed: com.sun.tools.javac, jdk.compiler)"))
            throw new Exception("expected output not found");
    }
}

