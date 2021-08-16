/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4636667 7052425 8016549 8196202
 * @summary  Use <H1, <H2>, and <H3> in proper sequence for accessibility
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main AccessH1
 */


import javadoc.tester.JavadocTester;

public class AccessH1 extends JavadocTester {

    public static void main(String... args) throws Exception {
        AccessH1 tester = new AccessH1();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-doctitle", "Document Title",
                "-sourcepath", testSrc,
                "p1", "p2");
        checkExit(Exit.OK);

        // Test the style sheet
        checkOutput("stylesheet.css", true,
                "h1 {\n"
                + "    font-size:20px;\n"
                + "}");

        // Test the doc title in the overview page
        checkOutput("index.html", true,
                """
                    <h1 class="title">Document Title</h1>""");
    }
}
