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
 * @bug 8175219
 * @summary test --ignore-errors works correctly
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main IgnoreSourceErrors
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import toolbox.*;
import toolbox.Task.*;

/**
 * Dummy javadoc comment.
 */
public class IgnoreSourceErrors  extends TestRunner {

    final ToolBox tb;
    final Path testSrc;

    public IgnoreSourceErrors() throws IOException {
        super(System.err);
        tb = new ToolBox();
        testSrc = Paths.get("Foo.java");
        emitSample(testSrc);
    }

    public static void main(String... args) throws Exception {
        new IgnoreSourceErrors().runTests();
    }

    @Test
    public void runIgnoreErrorsOffByDefault() throws Exception {
        JavadocTask task = new JavadocTask(tb, Task.Mode.CMDLINE);
        task.options(testSrc.toString());
        Task.Result result = task.run(Expect.FAIL);
        String out = result.getOutput(OutputKind.DIRECT);
        if (!out.contains("modifier static not allowed here")) {
            throw new Exception("expected string not found \'modifier static not allowed here\'");
        }
    }

    @Test
    public void runIgnoreErrorsOn() throws Exception {
        JavadocTask task = new JavadocTask(tb, Task.Mode.CMDLINE);
        task.options("--ignore-source-errors", testSrc.toString());
        Task.Result result = task.run(Expect.SUCCESS);
        String out = result.getOutput(OutputKind.DIRECT);
        if (!out.contains("modifier static not allowed here")) {
            throw new Exception("expected string not found \'modifier static not allowed here\'");
        }
    }

    void emitSample(Path file) throws IOException {
        String[] contents = {
            "/** A java file with errors */",
            "public static class Foo {}"
        };
        Files.write(file, Arrays.asList(contents), StandardOpenOption.CREATE);
    }
}
