/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8268896
 * @summary Verify source level checks are performed properly
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main SourceLevelChecks
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class SourceLevelChecks extends TestRunner {

    private static final String JAVA_VERSION = System.getProperty("java.specification.version");

    ToolBox tb;

    public static void main(String... args) throws Exception {
        new SourceLevelChecks().runTests();
    }

    SourceLevelChecks() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testPattern(Path base) throws Exception {
        doTest(base,
               """
               package test;
               public class Test {
                   private void test(Integer i) {
                       switch (i) {
                           case Integer d:
                       }
                   }
               }
               """,
               "Test.java:5:18: compiler.err.preview.feature.disabled.plural: (compiler.misc.feature.pattern.switch)",
               "1 error");
    }

    @Test
    public void testParenthesizedPatternIf(Path base) throws Exception {
        doTest(base,
               """
               package test;
               public class Test {
                   private void test(Object o) {
                       if (o instanceof (Integer d)) {
                       }
                   }
               }
               """,
               "Test.java:4:26: compiler.err.preview.feature.disabled.plural: (compiler.misc.feature.pattern.switch)",
               "1 error");
    }

    @Test
    public void testParenthesizedPatternSwitch(Path base) throws Exception {
        doTest(base,
               """
               package test;
               public class Test {
                   private void test(Integer i) {
                       switch (i) {
                           case (Integer d):
                       }
                   }
               }
               """,
               "Test.java:5:18: compiler.err.preview.feature.disabled.plural: (compiler.misc.feature.pattern.switch)",
               "1 error");
    }

    @Test
    public void testCaseDefault(Path base) throws Exception {
        doTest(base,
               """
               package test;
               public class Test {
                   private void test(Integer i) {
                       switch (i) {
                           case default:
                       }
                   }
               }
               """,
               "Test.java:5:18: compiler.err.preview.feature.disabled.plural: (compiler.misc.feature.pattern.switch)",
               "1 error");
    }

    private void doTest(Path base, String testCode, String... expectedErrors) throws IOException {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");
        tb.writeJavaFiles(src, testCode);

        Files.createDirectories(classes);

        var log =
                new JavacTask(tb)
                    .options("-source", "11",
                             "-Xlint:-options",
                             "-XDrawDiagnostics")
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
        if (!List.of(expectedErrors).equals(log)) {
            throw new AssertionError("Incorrect errors, expected: " + List.of(expectedErrors) +
                                      ", actual: " + log);
        }
    }

}
