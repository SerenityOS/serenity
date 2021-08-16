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

/*
 * @test
 * @bug      8268774
 * @summary  Residual logging output written to STDOUT, not STDERR
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestToolStreams
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

// See also TestReporterStreams for testing doclet/reporter use of streams
public class TestToolStreams extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestToolStreams tester = new TestToolStreams();
        tester.runTests(m -> new Object[]{Path.of(m.getName())});
    }

    ToolBox tb = new ToolBox();

    TestToolStreams() throws IOException {
        tb.writeJavaFiles(Path.of("src"),
               """
                    package p1;
                    /** Comment 1. */
                    public class C1 { }""",
                """
                    package p2;
                    /** Comment 2. */
                    public class C2 { }""");
    }

    /**
     * Tests the entry point used by the DocumentationTool API and JavadocTester, in which
     * all output is written to a single specified writer.
     */
    @Test
    public void testSingleStream(Path base) {
        test(base, false, Output.OUT, Output.OUT);
    }

    /**
     * Tests the entry point used by the launcher, in which output is written to
     * writers that wrap {@code System.out} and {@code System.err}.
     */
    @Test
    public void testStandardStreams(Path base) {
        test(base, true, Output.STDOUT, Output.STDERR);
    }

    void test(Path base, boolean useStdStreams, Output stdOut, Output stdErr) {
        setOutputDirectoryCheck(DirectoryCheck.NONE);
        setUseStandardStreams(useStdStreams);

        javadoc("--help");
        checkExit(Exit.OK);

        if (stdOut != stdErr) {
            checkIsEmpty(stdErr);
        }

        checkOutput(stdOut, true,
                "Usage:");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", "src",
                "-verbose", // Note: triggers lots of javac messages as well as the javadoc time-taken message
                "p1",
                Path.of("src").resolve("p2").resolve("C2.java").toString());
        checkExit(Exit.OK);

        if (stdOut != stdErr) {
            checkIsEmpty(stdOut);
        }

        checkOutput(stdErr, true,
                "Loading source file src/p2/C2.java...".replace("/", FS),
                "Loading source files for package p1...",
                "Constructing Javadoc information",
                "[done in ", " ms]"
                );
    }

    void checkIsEmpty(Output out) {
        checking("no output to " + out);
        String s = getOutput(out);
        if (s.isEmpty()) {
            passed("no output written to " + out);
        } else {
            failed(out + " is not empty");
        }
    }
}