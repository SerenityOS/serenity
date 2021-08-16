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
 * @run main TestNestedIndexTag
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

import static javadoc.tester.JavadocTester.Output.OUT;

public class TestNestedIndexTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNestedIndexTag tester = new TestNestedIndexTag();
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
    public void testIndexIndexDocLint(Path base) throws IOException {
        testIndexIndex(base, DocLintKind.DOCLINT);
    }

    @Test
    public void testIndexIndexNoDocLint(Path base) throws IOException {
        testIndexIndex(base, DocLintKind.NO_DOCLINT);
    }


    public void testIndexIndex(Path base, DocLintKind dlk) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    /** First sentence. {@index abc ABC {@index def DEF} GHI} */
                    public class C { }
                    """);
        javadoc("-d", base.resolve("api").toString(),
                "-sourcepath", src.toString(),
                dlk.option,
                "p");
        checkExit(Exit.OK);

        checkOutput("tag-search-index.js", false,
                "{@index");
        checkOutput(Output.OUT, dlk == DocLintKind.DOCLINT,
                "C.java:2: warning: nested tag: @index");
    }

    @Test
    public void testIndexValue(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    /** First sentence. {@index abc ABC {@value Short#MAX_VALUE} DEF} */
                    public class C { }
                    """);
        javadoc("-d", base.resolve("api").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("tag-search-index.js", false,
                "{@index");
        checkOutput("tag-search-index.js", true,
                "ABC 32767 DEF");
    }
}
