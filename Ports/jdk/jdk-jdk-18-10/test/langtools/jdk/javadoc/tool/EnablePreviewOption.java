/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199196
 * @summary Test --enable-preview option in javadoc
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main EnablePreviewOption
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import jdk.javadoc.internal.tool.Main;
import jdk.javadoc.internal.tool.Main.Result;

import static jdk.javadoc.internal.tool.Main.Result.*;

import toolbox.TestRunner;
import toolbox.ToolBox;

public class EnablePreviewOption extends TestRunner {
    public static void main(String... args) throws Exception {
        new EnablePreviewOption().runTests();
    }

    ToolBox tb = new ToolBox();

    Path file = Paths.get("C.java");
    String thisVersion = System.getProperty("java.specification.version");
    String prevVersion = String.valueOf(Integer.valueOf(thisVersion) - 1);

    EnablePreviewOption() throws IOException {
        super(System.err);
        tb.writeFile(file, "public class C { }");
    }

    @Test
    public void testSource() {
        runTest(List.of("--enable-preview", "-source", thisVersion),
                OK,
                out -> !out.contains("error")
                && out.contains("Building tree for all the packages and classes..."));
    }

    @Test
    public void testRelease() {
        runTest(List.of("--enable-preview", "--release", thisVersion),
                OK,
                out -> !out.contains("error")
                && out.contains("Building tree for all the packages and classes..."));
    }

    @Test
    public void testNoVersion() {
        runTest(List.of("--enable-preview"),
                CMDERR,
                out -> out.contains("error: --enable-preview must be used with either -source or --release"));
    }

    @Test
    public void testBadSource() {
        runTest(List.of("--enable-preview", "-source", "BAD"),
                ERROR,
                out -> out.contains("error: invalid source release: BAD"));
    }

    @Test
    public void testOldSource() {
        runTest(List.of("--enable-preview", "-source", prevVersion),
                CMDERR,
                out -> out.matches("(?s)error: invalid source release .* with --enable-preview.*"));
    }

    private void runTest(List<String> options, Result expectedResult, Predicate<String> validate) {
        System.err.println("running with options: " + options);
        List<String> args = new ArrayList<>();
        args.addAll(options);
        args.add("-XDrawDiagnostics");
        args.add(file.toString());
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        int actualResult = Main.execute(args.toArray(new String[0]), pw);
        System.err.println("actual result=" + actualResult);
        System.err.println("actual output=" + out.toString());
        if (actualResult != expectedResult.exitCode)
            error("Exit code not as expected");
        if (!validate.test(out.toString())) {
            error("Output not as expected");
        }
    }
}
