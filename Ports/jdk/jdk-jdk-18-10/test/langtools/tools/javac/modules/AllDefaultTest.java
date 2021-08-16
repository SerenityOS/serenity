/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164590 8170691
 * @summary Test use of ALL-DEFAULT token
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavaTask ModuleTestBase
 * @run main AllDefaultTest
 */

import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;

public class AllDefaultTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        AllDefaultTest t = new AllDefaultTest();
        t.runTests();
    }

    @Test
    public void testCompileTime_notAllowed(Path base) throws Exception {
        tb.writeJavaFiles(base, "class C { }");
        String out = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--add-modules=ALL-DEFAULT")
                .files(tb.findJavaFiles(base))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!out.contains("- compiler.err.bad.name.for.option: --add-modules, ALL-DEFAULT")) {
            error("expected text not found");
        }
    }

    @Test
    public void testRuntimeTime_ignored_1(Path base) throws Exception {
        tb.writeJavaFiles(base, "class C { }");
        new JavacTask(tb, Task.Mode.EXEC)
                .options("-XDrawDiagnostics",
                        "-J--add-modules=ALL-DEFAULT",
                        "--inherit-runtime-environment")
                .files(tb.findJavaFiles(base))
                .run()
                .writeAll();
    }

    @Test
    public void testRuntimeTime_ignored_2(Path base) throws Exception {
        tb.writeJavaFiles(base, "class C { }");
        new JavacTask(tb, Task.Mode.EXEC)
                .options("-XDrawDiagnostics",
                        "-J--add-modules=jdk.compiler",
                        "--inherit-runtime-environment")
                .files(tb.findJavaFiles(base))
                .run()
                .writeAll();
    }
}
