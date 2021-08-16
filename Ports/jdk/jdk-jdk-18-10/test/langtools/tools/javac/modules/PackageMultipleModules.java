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

/**
 * @test
 * @summary Verify modules can contain packages of the same name, unless these meet.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main PackageMultipleModules
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;

public class PackageMultipleModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        PackageMultipleModules t = new PackageMultipleModules();
        t.runTests();
    }

    @Test
    public void testSimple(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        Path m2 = base.resolve("m2x");
        tb.writeJavaFiles(m1,
                          "module m1x {}",
                          "package test; import test.B; public class A {}",
                          "package test; public class A1 extends A {}");
        tb.writeJavaFiles(m2,
                          "module m2x {}",
                          "package test; import test.A; public class B {}",
                          "package test; public class B1 extends B {}");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", base.toString())
                .outdir(classes)
                .files(findJavaFiles(base))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "A.java:1:26: compiler.err.cant.resolve.location: kindname.class, B, , , (compiler.misc.location: kindname.package, test, null)",
                "B.java:1:26: compiler.err.cant.resolve.location: kindname.class, A, , , (compiler.misc.location: kindname.package, test, null)",
                "2 errors");
        if (!log.equals(expected))
            throw new Exception("expected output not found");
    }

}
