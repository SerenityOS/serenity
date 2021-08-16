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
 * @bug 4369014 4851991 8164407 8205593
 * @summary Determine if the docRoot inline tag works properly.
 * If docRoot performs as documented, the test passes.
 * Make sure that the docRoot tag works with the -bottom option.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestDocRootInlineTag
 */

import javadoc.tester.JavadocTester;

public class TestDocRootInlineTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocRootInlineTag tester = new TestDocRootInlineTag();
        tester.runTests();
    }

    @Test
    public void test() {
        String uri = "http://www.java.sun.com/j2se/1.4/docs/api";

        javadoc("-bottom", """
            The value of @docRoot is "{@docRoot}\"""",
                "-d", "out",
                "-source", "8",
                "-sourcepath", testSrc,
                "-linkoffline", uri, testSrc,
                testSrc("TestDocRootTag.java"), "pkg");
        checkExit(Exit.OK);

        checkOutput("TestDocRootTag.html", true,
                "<a href=\"" + uri + """
                    /java/io/File.html" title="class or interface in java.io" class="external-link"><code>File</code></a>""",
                """
                    <a href="./index-all.html">index</a>""",
                "<a href=\"" + uri + """
                    /java/io/File.html" title="class or interface in java.io" class="external-link"><code>Second File Link</code></a>""",
                "The value of @docRoot is \"./\"");

        checkOutput("index-all.html", true,
                """
                    My package page is <a href="./pkg/package-summary.html">here</a>""");
    }
}
