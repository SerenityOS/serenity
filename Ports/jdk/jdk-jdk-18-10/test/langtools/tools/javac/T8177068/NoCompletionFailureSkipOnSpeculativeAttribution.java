/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177068 8233655
 * @summary CompletionFailures occurring during speculative attribution should
 *          not be lost forever.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox
 * @run main NoCompletionFailureSkipOnSpeculativeAttribution
 */

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class NoCompletionFailureSkipOnSpeculativeAttribution {

    private static final String TSrc =
        "import one.A;\n" +
        "class T {\n" +
        "  {\n" +
        "    System.err.println(two.C.D.g());\n" +
        "  }\n" +
        "}";

    private static final String CSrc =
        "package two;\n" +
        "public class C {\n" +
        "  public static class D {\n" +
        "    public static int g() {\n" +
        "      return 1;\n" +
        "    }\n" +
        "  }\n" +
        "}";

    private static final String ASrc =
        "package one;\n" +
        "public class A {\n" +
        "  public A(two.C.D x) {}\n" +
        "}";

    public static void main(String[] args) throws Exception {
        new NoCompletionFailureSkipOnSpeculativeAttribution().test();
        new NoCompletionFailureSkipOnSpeculativeAttribution().test8233655();
    }

    public void test() throws Exception {
        ToolBox tb = new ToolBox();
        tb.writeJavaFiles(Paths.get("."), ASrc, CSrc, TSrc);

        new JavacTask(tb)
                .classpath(".")
                .files("T.java")
                .run();

        tb.deleteFiles("two/C.class", "two/C$D.class");

        List<String> output = new JavacTask(tb)
                .sourcepath(File.pathSeparator)
                .options("-XDrawDiagnostics", "-XDshould-stop.ifError=FLOW")
                .classpath(".")
                .files("T.java")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOutput = List.of(
                "T.java:4:29: compiler.err.cant.access: two.C.D, (compiler.misc.class.file.not.found: two.C$D)",
                "1 error"
        );

        Assert.check(output.equals(expectedOutput));
    }

    public void test8233655() throws Exception {
        ToolBox tb = new ToolBox();
        tb.writeJavaFiles(Paths.get("."),
                          "public class Test {" +
                          "    private <T> T test(Class<?> c) {\n" +
                          "        Class<?> c2 = test(test(Helper.class));\n" +
                          "        return null;\n" +
                          "    }\n" +
                          "}",
                          "public class Helper extends Unknown {}");

        List<String> output = new JavacTask(tb)
                .sourcepath(".")
                .options("-XDrawDiagnostics")
                .classpath(".")
                .files("Test.java")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOutput = List.of(
                "Helper.java:1:29: compiler.err.cant.resolve: kindname.class, Unknown, , ",
                "1 error"
        );

        Assert.check(output.equals(expectedOutput));
    }
}
