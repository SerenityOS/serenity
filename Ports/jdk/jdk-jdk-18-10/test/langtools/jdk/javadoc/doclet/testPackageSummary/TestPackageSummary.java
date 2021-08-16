/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8189841 8253117 8263507
 * @summary Error in alternate row coloring in package-summary files
 * @summary Improve structure of package summary pages
 * @library  ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* TestPackageSummary
 * @run main TestPackageSummary
 */

import javadoc.tester.JavadocTester;

public class TestPackageSummary extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestPackageSummary tester = new TestPackageSummary();
        tester.runTests();
    }

    @Test
    public void testSummaryLinks() {
        javadoc("-d", "out-links",
                "-sourcepath", testSrc,
                "-subpackages", "pkg:pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg/package-summary.html", true,
                """
                    <div class="sub-nav">
                    <div>
                    <ul class="sub-nav-list">
                    <li>Package:&nbsp;</li>
                    <li>Description&nbsp;|&nbsp;</li>
                    <li>Related Packages&nbsp;|&nbsp;</li>
                    <li><a href="#class-summary">Classes and Interfaces</a></li>
                    </ul>
                    </div>""");
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="sub-nav">
                    <div>
                    <ul class="sub-nav-list">
                    <li>Package:&nbsp;</li>
                    <li><a href="#package-description">Description</a>&nbsp;|&nbsp;</li>
                    <li><a href="#related-package-summary">Related Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#class-summary">Classes and Interfaces</a></li>
                    </ul>
                    </div>""");
        checkOutput("pkg1/sub/package-summary.html", true,
                """
                    <div class="sub-nav">
                    <div>
                    <ul class="sub-nav-list">
                    <li>Package:&nbsp;</li>
                    <li>Description&nbsp;|&nbsp;</li>
                    <li><a href="#related-package-summary">Related Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#class-summary">Classes and Interfaces</a></li>
                    </ul>
                    </div>""");
    }

    @Test
    public void testStripes() {
        javadoc("-d", "out-stripes",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/package-summary.html", true,
                """
                    <div class="col-first even-row-color class-summary class-summary-tab2"><a href="C0.html" title="class in pkg">C0</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab2">&nbsp;</div>
                    <div class="col-first odd-row-color class-summary class-summary-tab2"><a href="C1.html" title="class in pkg">C1</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab2">&nbsp;</div>
                    <div class="col-first even-row-color class-summary class-summary-tab2"><a href="C2.html" title="class in pkg">C2</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab2">&nbsp;</div>
                    <div class="col-first odd-row-color class-summary class-summary-tab2"><a href="C3.html" title="class in pkg">C3</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab2">&nbsp;</div>
                    <div class="col-first even-row-color class-summary class-summary-tab2"><a href="C4.html" title="class in pkg">C4</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab2">&nbsp;</div>
                    """
        );
    }
}

