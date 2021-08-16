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

/*
 * @test
 * @bug 8174073
 * @summary NPE caused by link reference to class
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main NPEDuplicateClassNamesTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class NPEDuplicateClassNamesTest extends TestRunner {

    public static void main(String... args) throws Exception {
        NPEDuplicateClassNamesTest t = new NPEDuplicateClassNamesTest();
        t.runTests();
    }

    private final ToolBox tb = new ToolBox();
    private final String class1 =
            "package com;\n" +
            "/***/\n" +
            "public class MyClass {}";
    private final String class2 =
            "package com;\n" +
            "/**\n" +
            " * The following link tag causes a NullPointerException: {@link Requirements}. \n" +
            " */\n" +
            "public class MyClass {}";

    NPEDuplicateClassNamesTest() throws IOException {
        super(System.err);
    }

    @Test
    public void testDuplicateClassNames() throws IOException {
        Path src = Paths.get("src");
        Path one = src.resolve("one");
        Path two = src.resolve("two");
        Path classes = Paths.get("classes");
        Files.createDirectories(classes);
        tb.writeJavaFiles(one, class1);
        tb.writeJavaFiles(two, class2);

        List<String> expected = Arrays.asList(
                "MyClass.java:5:8: compiler.err.duplicate.class: com.MyClass",
                "MyClass.java:3:65: compiler.err.proc.messager: reference not found",
                "2 errors");
        List<String> output = new JavacTask(tb)
                  .outdir(classes)
                  .options("-XDrawDiagnostics", "-Xdoclint:all", "-XDdev")
                  .files(tb.findJavaFiles(src))
                  .run(Task.Expect.FAIL)
                  .writeAll()
                  .getOutputLines(Task.OutputKind.DIRECT);

        if (!Objects.equals(output, expected)) {
            throw new IllegalStateException("incorrect output; actual=" + output + "; expected=" + expected);
        }
    }
}
