/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156962
 * @summary Tests use of JDK_JAVAC_OPTIONS env variable
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main EnvVarTest
 */

import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task.Expect;
import toolbox.Task.Mode;

public class EnvVarTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new EnvVarTest().runTests();
    }

    @Test
    public void testAddExports(Path base) throws Exception {
        Path src = base.resolve("src");
        // the package in the following call should be one that is not
        // available by default, including via @modules
        tb.writeJavaFiles(src,
            "package p; public class C { com.sun.tools.javac.jvm.Target t; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        tb.out.println("test that compilation fails if addExports is not provided");
        new JavacTask(tb)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll();

        tb.out.println("test that addExports can be given to javac");
        new JavacTask(tb)
                .options("--add-exports", "jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll();

        tb.out.println("test that addExports can be provided with env variable");
        new JavacTask(tb, Mode.EXEC)
                .envVar("JDK_JAVAC_OPTIONS", "--add-exports jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll();

        tb.out.println("test that addExports can be provided with env variable using @file");
        Path atFile = src.resolve("at-file.txt");
        tb.writeFile(atFile,
                "--add-exports jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED");

        new JavacTask(tb, Mode.EXEC)
                .envVar("JDK_JAVAC_OPTIONS", "@" + atFile)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll();
    }

}

