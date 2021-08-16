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
 * @bug      8261203
 * @summary  Incorrectly escaped javadoc html with type annotations
 * @library  /tools/lib ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestMethodId
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestMethodId extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMethodId tester = new TestMethodId();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    private ToolBox tb = new ToolBox();

    @Test
    public void testMethodId(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    public class C {
                        public void m(@A("anno-text") int i) { }
                    }
                    """,
                """
                    package p;
                    public @interface A {
                        String value();
                    }
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "-Xdoclint:none",
                "--source-path", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/C.html",
                true,
                """
                    <code><a href="#m(int)" class="member-name-link">m</a><wbr>(int&nbsp;i)</code>""",
                """
                    <section class="detail" id="m(int)">
                    <h3>m</h3>""");
    }
}