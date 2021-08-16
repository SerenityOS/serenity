/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  8222091 8245058
 * @summary  Javadoc does not handle package annotations correctly on package-info.java
 * @library  ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build   javadoc.tester.*
 * @run main TestPackageAnnotation
 */

import javadoc.tester.JavadocTester;

public class TestPackageAnnotation extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestPackageAnnotation tester = new TestPackageAnnotation();
        tester.runTests();
    }

    @Test
    public void testPackageInfoAnnotationNoComment() {
        javadoc("-d", "out-annotation",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-use",
                "pkg1");
        checkExit(Exit.OK);
        checkOutput("pkg1/package-summary.html", true,
                """
                    <main role="main">
                    <div class="header">
                    <h1 title="Package pkg1" class="title">Package pkg1</h1>
                    </div>
                    <hr>
                    <div class="package-signature"><span class="annotations">@Deprecated(since="1&lt;2&gt;3")
                    </span>package <span class="element-name">pkg1</span></div>
                    """);
    }

    @Test
    public void testPackageHtmlTag() {
        javadoc("-d", "out-annotation-2",
                "-sourcepath", testSrc,
                "-use",
                "pkg2");
        checkExit(Exit.OK);
        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">This package is deprecated.</div>
                    </div>
                    <div class="block">This is the description of package pkg2.</div>
                    </section>""");
    }

    @Test
    public void testPackageInfoAndHtml() {
        javadoc("-d", "out-annotation-3",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-use",
                "pkg3");
        checkExit(Exit.OK);
        checkOutput("pkg3/package-summary.html", true,
                """
                    <main role="main">
                    <div class="header">
                    <h1 title="Package pkg3" class="title">Package pkg3</h1>
                    </div>
                    <hr>
                    <div class="package-signature"><span class="annotations">@Deprecated(since="1&lt;2&gt;3")
                    </span>package <span class="element-name">pkg3</span></div>
                    """);
    }
}
