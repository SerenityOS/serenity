/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8253996
 * @summary Verify doclint behavior when doclint not available
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @run main/othervm --limit-modules jdk.compiler,jdk.zipfs LimitedImage
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import toolbox.JavacTask;
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

        Path testSource = Path.of("Test.java");
        tb.writeFile(testSource, "class Test {}");

        List<String> actualOutput;
        List<String> expectedOutput = List.of(
                "- compiler.warn.doclint.not.available",
                "1 warning"
        );

        //check proper diagnostics when doclint provider not present:
        System.err.println("Test -Xdoclint when doclint not available");
        actualOutput = new JavacTask(tb, Mode.CMDLINE)
                .options("-XDrawDiagnostics", "-Xdoclint")
                .files(testSource)
                .outdir(".")
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        tb.checkEqual(expectedOutput, actualOutput);
    }

}
