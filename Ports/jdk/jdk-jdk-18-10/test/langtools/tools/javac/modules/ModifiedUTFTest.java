/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test Surrogate Pair module name
 * @bug 8233829
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ModifiedUTFTest
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;

public class ModifiedUTFTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ModifiedUTFTest t = new ModifiedUTFTest();
        t.runTests();
    }

    @Test
    public void testAddModules(Path base) throws Exception {
        String moduleName = "\uD840\uDC00"; // U+20000
        Path src1 = base.resolve("src1");
        tb.writeJavaFiles(src1,
                "module " + moduleName + " { exports p1; }",
                "package p1; public class C1 { }");
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2,
                "package p2; public class C2 { }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .outdir(modules)
                .files(findJavaFiles(src1))
                .options("-encoding", "utf8")
                .run(Task.Expect.SUCCESS)
                .writeAll();

        new JavacTask(tb)
                .outdir(classes)
                .files(findJavaFiles(src2))
                .options("-encoding", "utf8",
                         "--module-path", modules.toString(),
                         "--add-modules", moduleName)
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }
}
