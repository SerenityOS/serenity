/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4492643 4689286 8196201 8184205
 * @summary Test that a package page is properly generated when a .java file
 * passed to Javadoc.  Also test that the proper package links are generated
 * when single or multiple packages are documented.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestPackagePage
 */

import javadoc.tester.JavadocTester;

public class TestPackagePage extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestPackagePage tester = new TestPackagePage();
        tester.runTests();
    }

    @Test
    public void testSinglePackage() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                testSrc("com/pkg/C.java"));
        checkExit(Exit.OK);

        checkOutput("com/pkg/package-summary.html", true,
            "This is a package page.");

        // With just one package, all general pages link to the single package page.
        checkOutput("com/pkg/C.html", true,
            """
                <a href="package-summary.html">Package</a>""");
        checkOutput("com/pkg/package-tree.html", true,
            """
                <li><a href="package-summary.html">Package</a></li>""");
        checkOutput("deprecated-list.html", true,
            """
                <li><a href="com/pkg/package-summary.html">Package</a></li>""");
        checkOutput("index-all.html", true,
            """
                <li><a href="com/pkg/package-summary.html">Package</a></li>""");
        checkOutput("help-doc.html", true,
            """
                <li><a href="com/pkg/package-summary.html">Package</a></li>""");
    }

    @Test
    public void testMultiplePackages() {
        javadoc("-d", "out-2",
                "-sourcepath", testSrc,
                "com.pkg", "pkg2");
        checkExit(Exit.OK);

        //With multiple packages, there is no package link in general pages.
        checkOutput("deprecated-list.html", true,
            "<li>Package</li>");
        checkOutput("index-all.html", true,
            "<li>Package</li>");
        checkOutput("help-doc.html", true,
            "<li>Package</li>");
        checkOutput("allclasses-index.html", true,
                """
                    <div id="all-classes-table">
                    <div class="caption"><span>Classes</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>
                    """);
        checkOutput("allpackages-index.html", true,
                """
                    <div class="caption"><span>Package Summary</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    """);
        checkOutput("type-search-index.js", true,
                """
                    {"l":"All Classes and Interfaces","u":"allclasses-index.html"}""");
        checkOutput("package-search-index.js", true,
                """
                    {"l":"All Packages","u":"allpackages-index.html"}""");
        checkOutput("index-all.html", true,
                """
                    <br><a href="allclasses-index.html">All&nbsp;Classes&nbsp;and&nbsp;Interfaces</a\
                    ><span class="vertical-separator">|</span><a href="allpackages-index.html">All&n\
                    bsp;Packages</a>""");
    }
}
