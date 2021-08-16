/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173302 8182765 8196202 8210047
 * @summary make sure the overview-summary and module-summary pages don't
 *          don't have the See link, and the overview is copied correctly.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestOverview
 */

import javadoc.tester.JavadocTester;

public class TestOverview extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestOverview tester = new TestOverview();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                    "-doctitle", "Document Title",
                    "-windowtitle", "Window Title",
                    "-overview", testSrc("overview.html"),
                    "-sourcepath", testSrc("src"),
                    "p1", "p2");
        checkExit(Exit.OK);
        checkOverview();
    }

    @Test
    public void test2() {
        javadoc("-d", "out-2",
                    "-doctitle", "Document Title",
                    "-windowtitle", "Window Title",
                    "-overview", testSrc("overview.html"),
                    "-sourcepath", testSrc("msrc"),
                    "p1", "p2");
        checkExit(Exit.OK);
        checkOverview();
    }

    void checkOverview() {
        checkOutput("index.html", true,
                """
                    <main role="main">
                    <div class="header">
                    <h1 class="title">Document Title</h1>
                    </div>
                    <div class="block">This is line1. This is line 2.</div>
                    """);
    }
}
