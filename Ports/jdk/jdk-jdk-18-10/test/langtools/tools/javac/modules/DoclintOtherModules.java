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

/**
 * @test
 * @summary Verify that DocLint does not cause unnecessary (and potentially dangerous) implicit compilation
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main DoclintOtherModules
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class DoclintOtherModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        DoclintOtherModules t = new DoclintOtherModules();
        t.runTests();
    }

    @Test
    public void testSimple(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1x");
        Path m2 = src.resolve("m2x");
        tb.writeJavaFiles(m1,
                          "module m1x {}",
                          "package m1x; /** @see m2x.B */ @Deprecated public class A {}");
        tb.writeJavaFiles(m2,
                          "module m2x { requires m1x; exports m2x; }",
                          "package m2x; public class B extends Foo {} @Deprecated class Foo {}");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString(), "-Xlint:deprecation", "-Xdoclint:-reference", "-Werror")
                .outdir(classes)
                .files(findJavaFiles(m1))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);
    }

}
