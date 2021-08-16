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
 * @bug      8242607
 * @summary  -Xdoclint doesn't report missing/unexpected comments
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestMissingComment
 */

import java.io.File;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestMissingComment extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestMissingComment tester = new TestMissingComment();
        tester.runTests(m -> new Object[] { Path.of(m.getName() )});
    }

    private ToolBox tb = new ToolBox();

    @Test
    public void testClass(Path base) throws Exception {
        test(base.resolve("class"), """
                    // no doc comment
                    public class C { }
                    """,
                """
                    testClass/class/src/C.java:2: warning: no comment
                    public class C { }
                           ^
                    """);
    }

    @Test
    public void testExecutable(Path base) throws Exception {
        test(base.resolve("executable"), """
                    /** Class comment. */
                    public class C {
                        // no doc comment
                        public void m() { }
                    }
                    """,
                """
                    testExecutable/executable/src/C.java:4: warning: no comment
                        public void m() { }
                                    ^
                    """);
    }

    @Test
    public void testField(Path base) throws Exception {
        test(base.resolve("field"), """
                    /** Class comment. */
                    public class C {
                        // no doc comment
                        public int f;
                    }
                    """,
                """
                    testField/field/src/C.java:4: warning: no comment
                        public int f;
                                   ^
                    """);
    }

    @Test
    public void testNested(Path base) throws Exception {
        test(base.resolve("nest"), """
                    /** Class comment. */
                    public class C {
                        // no doc comment
                        public class Nested { }
                    }
                    """,
                """
                    testNested/nest/src/C.java:4: warning: no comment
                        public class Nested { }
                               ^
                    """);
    }

    private void test(Path base, String code, String expect) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, code);

        javadoc("-d", base.resolve("api").toString(),
                "-Xdoclint:missing",
                "--no-platform-links",
                src.resolve("C.java").toString());
        checkExit(Exit.OK);
        checkOutput(Output.OUT, true,
                expect.replace("/", File.separator),
                "1 warning");
    }
}
