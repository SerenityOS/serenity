/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8132562
 * @summary javac fails with CLASSPATH with double-quotes as an environment variable
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ClassPathWithDoubleQuotesTest
*/

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ClassPathWithDoubleQuotesTest extends TestRunner {

    ToolBox tb;

    private static final String ASrc = "public class A { J j; B b;}";
    private static final String BSrc = "public class B {}";
    private static final String JarSrc = "public class J {}";
    private static final String[] jarArgs = {"cf", "test/jarOut/J.jar", "-C", "test/jarSrc", "J.java"};
    public static final String NEW_LINE = System.getProperty("line.separator");
    private static final List<String> expectedFailureOutput1 = List.of(
            "A.java:1:18: compiler.err.cant.resolve.location: kindname.class, J, , , (compiler.misc.location: kindname.class, A, null)",
            "A.java:1:23: compiler.err.cant.resolve.location: kindname.class, B, , , (compiler.misc.location: kindname.class, A, null)",
            "2 errors"
    );
    private static final List<String> expectedFailureOutput2A = List.of(
            "- compiler.warn.invalid.path: \"test/jarOut/J.jar",
            "- compiler.warn.invalid.path: test/src\"",
            "A.java:1:18: compiler.err.cant.resolve.location: kindname.class, J, , , (compiler.misc.location: kindname.class, A, null)",
            "A.java:1:23: compiler.err.cant.resolve.location: kindname.class, B, , , (compiler.misc.location: kindname.class, A, null)",
            "2 errors",
            "2 warnings"
    );
    private static final List<String> expectedFailureOutput2B = List.of(
            "- compiler.warn.path.element.not.found: \"test/jarOut/J.jar",
            "- compiler.warn.path.element.not.found: test/src\"",
            "A.java:1:18: compiler.err.cant.resolve.location: kindname.class, J, , , (compiler.misc.location: kindname.class, A, null)",
            "A.java:1:23: compiler.err.cant.resolve.location: kindname.class, B, , , (compiler.misc.location: kindname.class, A, null)",
            "2 errors",
            "2 warnings"
    );

    public static void main(String... args) throws Exception {
        new ClassPathWithDoubleQuotesTest().runTests();
    }

    ClassPathWithDoubleQuotesTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void test(Path base) throws Exception {
        Path current = base.resolve(".");
        Path jarSrc = current.resolve("jarSrc");
        tb.writeJavaFiles(jarSrc, JarSrc);
        Path jarOut = current.resolve("jarOut");
        tb.createDirectories(jarOut);
        new JarTask(tb).run(jarArgs).writeAll();

        Path src = current.resolve("src");
        tb.writeJavaFiles(src, ASrc, BSrc);

        /** In any system there can be three possible scenarios:
         *  1 - The system swallows the problem character (the quote in this case)
         *      and the test case compiles
         *  2 - The problem character gets into javac, but it's not bad enough to trigger
         *      InvalidPathException, but it does mean you can't find the file you're looking for
         *  3 - The problem character gets into javac and is bad enough to trigger
         *      InvalidPathException, in which case javac needs to handle the exception in a reasonable way.
         */

        // testing scenario 1
        System.err.println("invoking javac EXEC mode without double quotes in the CLASSPATH env variable");
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "test/jarOut/J.jar" + File.pathSeparator + "test/src")
                .files("test/src/A.java").run(Task.Expect.SUCCESS);
        System.err.println("successful compilation");
        System.err.println();

        // testing scenario 2
        System.err.println("Simulate a system in which double quotes are preserved in the environment variable," +
                "and for which they are a legal filename character");
        List<String> log = new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "Ztest/jarOut/J.jar" + File.pathSeparator + "test/srcZ")
                .options("-XDrawDiagnostics")
                .files("test/src/A.java").run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.STDERR);
        log = log.stream().filter(s->!s.matches("^Picked up .*JAVA.*OPTIONS:.*")).collect(Collectors.toList());
        tb.checkEqual(log, expectedFailureOutput1);
        System.err.println("compilation is expected to fail");
        System.err.println();

        // testing scenario 3
        System.err.println("invoking javac EXEC mode with double quotes in the CLASSPATH env variable");
        List<String> log2 = new JavacTask(tb, Task.Mode.EXEC)
                    .envVar("CLASSPATH", "\"test/jarOut/J.jar" + File.pathSeparator + "test/src\"")
                    .options("-Xlint:path", "-XDrawDiagnostics")
                    .files("test/src/A.java").run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.STDERR);
        log2 = log2.stream().filter(s->!s.matches("^Picked up .*JAVA.*OPTIONS:.*")).collect(Collectors.toList());
        Assert.check(log2.equals(expectedFailureOutput2A) || log2.equals(expectedFailureOutput2B),
                "unexpected output");
    }
}
