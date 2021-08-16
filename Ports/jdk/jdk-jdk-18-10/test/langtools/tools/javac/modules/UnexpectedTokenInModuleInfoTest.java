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
 * @bug 8166420
 * @summary Confusing error message when reading bad module declaration
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main UnexpectedTokenInModuleInfoTest
 */

import java.nio.file.*;
import java.util.List;
import java.util.Arrays;

import toolbox.JavacTask;
import toolbox.Task;

public class UnexpectedTokenInModuleInfoTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        UnexpectedTokenInModuleInfoTest t = new UnexpectedTokenInModuleInfoTest();
        t.runTests();
    }

    @Test
    public void testSingleModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "weak module m { }");

         List<String> output = new JavacTask(tb)
            .options("-XDrawDiagnostics")
            .files(src.resolve("module-info.java"))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

         List<String> expected = Arrays.asList("module-info.java:1:1: compiler.err.expected.module.or.open",
                "1 error");
        if (!output.containsAll(expected)) {
            throw new Exception("Expected output not found");
        }
    }
}