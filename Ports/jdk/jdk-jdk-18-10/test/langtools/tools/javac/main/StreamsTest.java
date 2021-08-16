/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8162359
 * @summary extra space in javac -help for -J and @ options
 * @modules jdk.compiler
 * @library /tools/lib
 * @build toolbox.TestRunner toolbox.ToolBox
 * @run main StreamsTest
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;

import toolbox.TestRunner;
import toolbox.ToolBox;

public class StreamsTest extends TestRunner {
    public static void main(String... args) throws Exception {
        new StreamsTest().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    StreamsTest() {
        super(System.err);
    }

    ToolBox tb = new ToolBox();
    static final String LINESEP = System.getProperty("line.separator");

    @Test // errors should be written to stderr
    public void testError(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");
        tb.writeJavaFiles(src,
            "import java.util.*; class C { # }");
        test(asList("-d", classes.toString(), src.resolve("C.java").toString()),
                null, "illegal character: '#'");
    }

    @Test // warnings should be written to stderr
    public void testWarning(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");
        tb.writeJavaFiles(src,
            "import java.util.*; class C { List list = new ArrayList(); }");
        test(asList("-d", classes.toString(), "-Xlint", src.resolve("C.java").toString()),
                null, "warning: [rawtypes]");
    }

    @Test // notes should be written to stderr
    public void testNote(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");
        tb.writeJavaFiles(src,
            "import java.util.*; class C { List<String> list = (List<String>) new ArrayList(); }");
        test(asList("-d", classes.toString(), src.resolve("C.java").toString()),
                null, "uses unchecked or unsafe operations.");
    }

    @Test // help output should be written to stdout
    public void testHelp(Path base) throws Exception {
        test(asList("-help"), "Usage: javac <options> <source files>", null);
    }

    @Test // version output should be written to stdout
    public void testVersion(Path base) throws Exception {
        test(asList("-version"), "javac", null);
    }

    @Test // version output should be written to stdout
    public void testFullVersion(Path base) throws Exception {
        test(asList("-fullversion"), "javac full version", null);
    }

    /**
     * Run javac as though run from the command line (but avoiding the entry point that
     * calls System.exit()), and that that expected output appears on appropriate output streams.
     * @param options the command-line options for javac
     * @param expectOut a string that should be contained in the output generated on stdout,
     *      or null, if no output should be generated to stdout
     * @param expectErra string that should be contained in the output generated on stderr,
     *      or null, if no output should be generated to stderr
     * @throws IOException if a problem occurs while setting up the streams
     */
    void test(List<String> options, String expectOut, String expectErr) throws IOException {
        out.println("test " + options);
        ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bsErr = new ByteArrayOutputStream();
        try (PrintStream psOut = new PrintStream(bsOut); PrintStream psErr = new PrintStream(bsErr)) {
            int rc;
            PrintStream saveOut = System.out;
            PrintStream saveErr = System.err;
            try {
                System.setOut(psOut);
                System.setErr(psErr);
                rc = com.sun.tools.javac.Main.compile(options.toArray(new String[0]));
            } finally {
                System.setErr(saveErr);
                System.setOut(saveOut);
            }
            System.err.println("javac exit code: " + rc);
        }
        check("stdout", bsOut.toString(), expectOut);
        check("stderr", bsErr.toString(), expectErr);
    }

    /**
     * Check that output is as expected.
     * @param name the name of the stream on which the output was found
     * @param actual the contents written to the stream
     * @param expect string that should be contained in the output, or null, if the output should be empty
     */
    void check(String name, String actual, String expect) {
        out.println("Check " + name);
        out.println("Expected: " + (expect == null ? "(nothing)" : expect));
        out.println("Actual:");
        out.println(actual.replace("\n", LINESEP));
        if (expect == null) {
            if (!actual.isEmpty()) {
                error(name + ": unexpected output");
            }
        } else if (!actual.contains(expect)) {
            error(name + ": expected output not found");
        }
    }
}

