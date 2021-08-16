/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165102 8175560
 * @summary incorrect message from javac
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ModulesAndModuleSourcePathTest
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ModulesAndModuleSourcePathTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ModulesAndModuleSourcePathTest t = new ModulesAndModuleSourcePathTest();
        t.runTests();
    }

    @Test
    public void testModuleNotInModuleSrcPath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        Files.createDirectories(m);
        Path extra = base.resolve("m");
        tb.writeJavaFiles(extra, "module m {}");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(extra))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:1: compiler.err.module.not.found.on.module.source.path"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testModuleNotInPackageHierarchy(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        Path extra = m.resolve("extra");
        tb.writeJavaFiles(extra, "module m {}");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:1: compiler.err.module.not.found.on.module.source.path"))
            throw new Exception("expected output not found");
    }
}
