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
 * @summary tests for module finder
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main ModuleFinderTest
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;

public class ModuleFinderTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        ModuleFinderTest t = new ModuleFinderTest();
        t.runTests();
    }

    @Test
    public void testDuplicateModulesOnPath(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module m1x { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        new JarTask(tb, modules.resolve("m1x-1.jar"))
                .baseDir(classes)
                .files(".")
                .run();

        new JarTask(tb, modules.resolve("m1x-2.jar"))
                .baseDir(classes)
                .files(".")
                .run();

        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2, "module m2x { requires m1x; }");


        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics", "--module-path", modules.toString())
                .outdir(classes)
                .files(findJavaFiles(src2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("- compiler.err.duplicate.module.on.path: (compiler.misc.locn.module_path), m1x"))
            throw new Exception("expected output not found");
    }
}
