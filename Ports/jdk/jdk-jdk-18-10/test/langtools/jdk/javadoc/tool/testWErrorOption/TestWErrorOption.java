/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8200383 8238259
 * @summary Add javadoc command line setting to fail on warnings
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build   toolbox.ToolBox javadoc.tester.*
 * @run main TestWErrorOption
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestWErrorOption extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestWErrorOption tester = new TestWErrorOption();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    /** Control test to verify behavior without -Werror. */
    @Test
    public void testControl(Path base) throws Exception {
        generateSrc(base);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", base.resolve("src").toString(),
                "-Xdoclint:none",
                "p");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, false,
                "error: warnings found and -Werror specified");
        checkOutput(Output.OUT, true,
                "1 warning");
    }

    @Test
    public void testWerror(Path base) throws Exception {
        generateSrc(base);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", base.resolve("src").toString(),
                "-Xdoclint:none",
                "-Werror",
                "p");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                "C.java:6: warning: @return tag cannot be used in method with void return type.",
                """
                    error: warnings found and -Werror specified
                    1 error
                    1 warning
                    """);
    }

    @Test
    public void testHelp(Path base) throws Exception {
        javadoc("-d", base.resolve("out").toString(),
                "--help");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, true,
                """
                    -Werror       Report an error if any warnings occur
                    """);
    }

    private void generateSrc(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                  """
                      package p;
                      public class C {
                        /** Comment.
                         *  @return warning
                         */
                        public void m() { }
                      }""");
    }
}

