/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6786028 8026567
 * @summary This test verifies the use of <strong> HTML tag instead of <B> by Javadoc std doclet.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestHtmlStrongTag
 */

import javadoc.tester.JavadocTester;

public class TestHtmlStrongTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHtmlStrongTag tester = new TestHtmlStrongTag();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/C1.html", true,
            """
                <dl class="notes">
                <dt>See Also:</dt>""");

        checkOutput("pkg1/C1.html", false,
            "<STRONG>Method Summary</STRONG>",
            "<B>");

        checkOutput("pkg1/package-summary.html", false,
            "<STRONG>Class Summary</STRONG>");
    }

    @Test
    public void test2() {
        javadoc("-d", "out-2",
                "-sourcepath", testSrc,
                "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg2/C2.html", true,
                "<B>Comments:</B>");

        checkOutput("pkg2/C2.html", false,
                "<STRONG>Method Summary</STRONG>");
    }
}
