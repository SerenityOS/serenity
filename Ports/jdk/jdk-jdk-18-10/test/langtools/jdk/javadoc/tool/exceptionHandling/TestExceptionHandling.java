/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8151102
 * @summary verify that option --dump-on-error functions correctly
 * @library /tools/lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.TestRunner toolbox.JavadocTask toolbox.Task
 * @run main TestExceptionHandling
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.JavadocTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

/**
 * This class tests if stack traces printed when
 * --dump-on-error. The standard doclet is used,
 * to test the doclet as well as the tool.
 */
public class TestExceptionHandling extends TestRunner {

    final ToolBox tb;
    final Path testSrcFile;
    final PrintStream ostream;
    final JavadocTask cmdTask;
    final JavadocTask apiTask;

    public static void main(String... args) throws Exception {
        TestExceptionHandling tester = new TestExceptionHandling();
        tester.runTests();
    }

    TestExceptionHandling() throws IOException {
        super(System.err);
        tb = new ToolBox();
        ostream = System.err;
        testSrcFile = Paths.get("A.java").toAbsolutePath();
        tb.writeFile(testSrcFile, "public class A { }");
        cmdTask = new JavadocTask(tb, Task.Mode.CMDLINE);
        apiTask = new JavadocTask(tb, Task.Mode.API);
    }

    @Test
    public void testDocletTrace() throws Exception {
        Path out = Paths.get("out");
        // create a file with the same name as the output
        out.toFile().createNewFile();
        cmdTask.outdir(out);
        cmdTask.options("--dump-on-error");
        cmdTask.files(testSrcFile);
        Task.Result tr = cmdTask.run(Task.Expect.FAIL);

        String errString = "Destination directory is not a directory: " + out.toString();
        // check the regular message
        assertPresent("error: " + errString, tr.getOutputLines(Task.OutputKind.DIRECT));
        // check that first line of the stack trace is present
        assertPresent("jdk.javadoc.internal.doclets.toolkit.util.SimpleDocletException: " +
                errString, tr.getOutputLines(Task.OutputKind.STDERR));

    }

    @Test
    public void testToolTrace() throws Exception {
        Path out = Paths.get("out.dir");
        cmdTask.options("--dump-on-error", "-doclet", "NonExistentDoclet");
        cmdTask.outdir(out);
        cmdTask.files(testSrcFile);
        Task.Result tr = cmdTask.run(Task.Expect.FAIL);

        // check the regular message
        assertPresent("error: Cannot find doclet class NonExistentDoclet",
                tr.getOutputLines(Task.OutputKind.DIRECT));

        // check that first line of the stack trace is present
        assertPresent("java.lang.ClassNotFoundException: NonExistentDoclet",
                tr.getOutputLines(Task.OutputKind.STDERR));

    }

    @Test
    public void testApiModeMissingDoclet() throws Exception {
        apiTask.options("-doclet", "MissingDoclet");
        try {
            Task.Result result = apiTask.run(Task.Expect.FAIL);
        } catch (IllegalArgumentException iae) {
            // ok got the right exception
            return;
        }
        throw new Exception("expected exception/error not found");
    }

    @Test
    public void testApiModeMultipleDoclets() throws Exception {
        apiTask.options("-doclet", "MissingDoclet",
                "-doclet", "SomeDoclet");
        try {
            Task.Result result = apiTask.run(Task.Expect.FAIL);
        } catch (IllegalArgumentException iae) {
            // ok got the right exception
            return;
        }
        throw new Exception("expected exception/error not found");
    }

    void assertPresent(String regex, List<String> output) throws Exception {
        List<String> gresult = tb.grep(regex, output);
        if (gresult.isEmpty()) {
            ostream.println("Expected: " + regex);
            ostream.println("Output: ");
            output.forEach(s -> {
                ostream.println(s);
            });
            throw new Exception("Test fails expected output not found: " + regex);
        }
    }
}
