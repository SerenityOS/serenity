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
 * @bug      8267219
 * @summary  The method summary's description cell should not be removed when
 *           it only has inline tags that don't produce any output
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestEmptyInheritDoc
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestEmptyInheritDoc extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestEmptyInheritDoc tester = new TestEmptyInheritDoc();
        tester.runTests();
    }

    ToolBox tb = new ToolBox();
    Path src;

    TestEmptyInheritDoc() throws Exception {
        src = Files.createDirectories(Paths.get("src"));
        tb.writeJavaFiles(src,
                """
                    package pkg;

                    public class First {
                        public void act() {}
                    }
                    """,
                """
                    package pkg;

                    public class Second extends First {
                        /**
                         * {@inheritDoc}
                         */
                        public void act() {}
                        public void bark() {}
                    }
                    """);
    }

    @Test
    public void run() {
        javadoc("-d", "out-empty-inheritdoc",
                "-sourcepath", src.toString(),
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/Second.html", true,
                """
                   <div class="col-second even-row-color method-summary-table \
                   method-summary-table-tab2 method-summary-table-tab4"><code>\
                   <a href="#act()" class="member-name-link">act</a>()</code></div>
                   <div class="col-last even-row-color method-summary-table \
                   method-summary-table-tab2 method-summary-table-tab4"></div>""");
    }
}
