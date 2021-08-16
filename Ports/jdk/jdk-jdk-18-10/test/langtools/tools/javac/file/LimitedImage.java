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

/**
 * @test
 * @bug 8153391
 * @summary Verify javac behaves properly in absence of zip/jar FileSystemProvider
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @run main/othervm --limit-modules jdk.compiler LimitedImage
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.JarTask;
import toolbox.Task.Expect;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.ToolBox;

public class LimitedImage {
    public static void main(String... args) throws IOException {
        ToolBox tb = new ToolBox();

        //showing help should be OK
        new JavacTask(tb, Mode.CMDLINE)
                .options("--help")
                .run().writeAll();

        Path testSource = Paths.get("Test.java");
        tb.writeFile(testSource, "class Test {}");

        //when zip/jar FS is not needed, compilation should succeed
        new JavacTask(tb, Mode.CMDLINE)
                .classpath()
                .files(testSource)
                .outdir(".")
                .run()
                .writeAll();

        Path testJar = Paths.get("test.jar").toAbsolutePath();

        new JarTask(tb, testJar).run();

        List<String> actualOutput;
        List<String> expectedOutput = Arrays.asList(
                "- compiler.err.no.zipfs.for.archive: " + testJar.toString()
        );

        //check proper diagnostics when zip/jar FS not present:
        System.err.println("Test " + testJar + " on classpath");
        actualOutput = new JavacTask(tb, Mode.CMDLINE)
                .classpath(testJar)
                .options("-XDrawDiagnostics")
                .files(testSource)
                .outdir(".")
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Unexpected output");
        }

        System.err.println("Test " + testJar + " on sourcepath");
        actualOutput = new JavacTask(tb, Mode.CMDLINE)
                .sourcepath(testJar)
                .options("-XDrawDiagnostics")
                .files(testSource)
                .outdir(".")
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Unexpected output");
        }

        System.err.println("Test " + testJar + " on modulepath");
        actualOutput = new JavacTask(tb, Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--module-path", testJar.toString())
                .files(testSource)
                .outdir(".")
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Unexpected output");
        }

        expectedOutput = Arrays.asList(
                "- compiler.err.no.zipfs.for.archive: " + testJar.toString(),
                "1 error"
        );

        System.err.println("Test directory containing " + testJar + " on modulepath");
        actualOutput = new JavacTask(tb, Mode.CMDLINE)
                .classpath()
                .options("-XDrawDiagnostics",
                         "--module-path", testJar.getParent().toString())
                .files(testSource)
                .outdir(".")
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Unexpected output");
        }
    }

}
