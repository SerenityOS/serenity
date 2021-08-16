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
 * @bug 8180744
 * @summary Verify unsupported modules and module options handling.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavapTask toolbox.TestRunner
 * @run main ReleaseOption9
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class ReleaseOption9 extends TestRunner {

    private final ToolBox tb = new ToolBox();

    public ReleaseOption9() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new ReleaseOption9().runTests();
    }

    @Test
    public void testUnsafe(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { requires jdk.unsupported; }",
                          "package test; public class Test { sun.misc.Unsafe unsafe; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "Test.java:1:43: compiler.warn.sun.proprietary: sun.misc.Unsafe",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--release", "9")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testUnsafeUnnamed(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { sun.misc.Unsafe unsafe; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "Test.java:1:43: compiler.warn.sun.proprietary: sun.misc.Unsafe",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--release", "9")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testIncubatorHttpClient(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { requires jdk.incubator.httpclient; }",
                          "package test;\n" +
                          "public class Test {\n" +
                          "    jdk.incubator.http.HttpRequest v;\n" +
                          "}\n");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "- compiler.warn.incubating.modules: jdk.incubator.httpclient",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--release", "9")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }
}
