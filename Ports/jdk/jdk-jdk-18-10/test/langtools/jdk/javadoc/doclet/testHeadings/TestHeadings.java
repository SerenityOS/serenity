/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4905786 6259611 8162363 8196202 8261976
 * @summary  Make sure that headings use the TH tag instead of the TD tag.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @build    TestHeadings
 * @run main TestHeadings
 */

import javadoc.tester.JavadocTester;

public class TestHeadings extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHeadings tester = new TestHeadings();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-use",
                "-header", "Test Files",
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Field</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <h3 id="methods-inherited-from-class-java.lang.Object">Methods inherited from class&nbsp;java.lang.Object</h3>""");

        // Class use documentation
        checkOutput("pkg1/class-use/C1.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Field</div>
                    <div class="table-header col-last">Description</div>""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="table-header col-first">Method</div>
                    <div class="table-header col-last">Description</div>""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Constant Field</div>
                    <div class="table-header col-last">Value</div>""");

        // Serialized Form
        checkOutput("serialized-form.html", true,
                """
                    <h2 title="Package">Package&nbsp;<a href="pkg1/package-summary.html">pkg1</a></h2>""",
                """
                    <h3>Class&nbsp;<a href="pkg1/C1.html" title="class in pkg1">pkg1.C1</a></h3>""",
                "<h4>Serialized Fields</h4>");

        // Overview Summary
        checkOutput("index.html", true,
                "<title>Overview</title>");
    }
}
