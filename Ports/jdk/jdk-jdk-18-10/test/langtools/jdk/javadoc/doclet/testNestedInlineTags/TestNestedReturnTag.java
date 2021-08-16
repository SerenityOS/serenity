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
 * @bug      8257925
 * @summary  enable more support for nested inline tags
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestNestedReturnTag
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestNestedReturnTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNestedReturnTag tester = new TestNestedReturnTag();
        tester.runTests(m -> new Object[] { Path.of(m.getName())});
    }

    ToolBox tb = new ToolBox();

    enum DocLintKind {
        DOCLINT("-Xdoclint"),
        NO_DOCLINT("-Xdoclint:none");
        final String option;
        DocLintKind(String option) {
            this.option = option;
        }
    }

    @Test
    public void testReturnReturnDocLint(Path base) throws IOException {
        testReturnReturn(base, DocLintKind.DOCLINT);
    }

    @Test
    public void testReturnReturnNoDocLint(Path base) throws IOException {
        testReturnReturn(base, DocLintKind.NO_DOCLINT);
    }

    void testReturnReturn(Path base, DocLintKind dlk) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    /** Comment. */
                    public class C {
                        /** {@return abc ABC {@return def DEF} GHI} */
                        public int m() { return 0; }
                    }
                    """);
        javadoc("-d", base.resolve("api").toString(),
                "-sourcepath", src.toString(),
                dlk.option,
                "p");
        checkExit(Exit.OK);

        checkOutput("p/C.html", false,
                "{@return");
        checkOutput(Output.OUT, dlk == DocLintKind.DOCLINT,
                "C.java:4: warning: @return has already been specified");
    }

    @Test
    public void testReturnValue(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    /** Comment. */
                    public class C {
                        /** {@return abc ABC {@value Short#MAX_VALUE} DEF} */
                        public int m() { return 0; }
                    }
                    """);
        javadoc("-d", base.resolve("api").toString(),
                "-sourcepath", src.toString(),
                "--no-platform-links",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/C.html", false,
                "{@value");
        checkOutput("p/C.html", true,
                "ABC 32767 DEF");
    }
}
