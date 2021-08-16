/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests for multi-module mode compilation
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main MultiModuleModeTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class MultiModuleModeTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new MultiModuleModeTest().runTests();
    }

    @Test
    public void testDuplicateModules(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1, "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2, "module m1x { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:1: compiler.err.duplicate.module: m1x"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testCantFindModule(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1, "module m1x { }");
        Path misc = base.resolve("misc");
        tb.writeJavaFiles(misc, "package p; class C { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .outdir(classes)
                .files(join(findJavaFiles(src), findJavaFiles(misc)))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("C.java:1:1: compiler.err.not.in.module.on.module.source.path"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testModuleNameMismatch(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1, "module m2x { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:8: compiler.err.module.name.mismatch: m2x, m1x"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testImplicitModuleSource(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"), "module m1x { }");
        tb.writeJavaFiles(src.resolve("m2x"), "module m2x { requires m1x; }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(src.resolve("m2x/module-info.java"))
                .run()
                .writeAll();
    }

    @Test
    public void testImplicitModuleClass(Path base) throws Exception {
        Path src1 = base.resolve("src1");
        tb.writeJavaFiles(src1.resolve("m1x"), "module m1x { }");
        Path modules1 = base.resolve("modules1");
        Files.createDirectories(modules1);

        new JavacTask(tb)
                .options("--module-source-path", src1.toString())
                .outdir(modules1)
                .files(src1.resolve("m1x/module-info.java"))
                .run()
                .writeAll();

        Path src2= base.resolve("src2");
        tb.writeJavaFiles(src2.resolve("m2x"), "module m2x { requires m1x; }");
        Path modules2 = base.resolve("modules2");
        Files.createDirectories(modules2);

        new JavacTask(tb)
                .options("--module-path", modules1.toString(),
                        "--module-source-path", src2.toString())
                .outdir(modules2)
                .files(src2.resolve("m2x/module-info.java"))
                .run()
                .writeAll();
    }

    Path[] join(Path[] a, Path[] b) {
        List<Path> result = new ArrayList<>();
        result.addAll(Arrays.asList(a));
        result.addAll(Arrays.asList(b));
        return result.toArray(new Path[result.size()]);
    }
}
