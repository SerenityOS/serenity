/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163800 8175200 8186332 8182765
 * @summary The fix for JDK-8072052 shows up other minor incorrect use of styles
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @build TestClassLinks
 * @run main TestClassLinks
 */

import javadoc.tester.JavadocTester;

public class TestClassLinks extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestClassLinks tester = new TestClassLinks();
        tester.runTests();
    }

    @Test
    public void test() {

        javadoc("-d", "out",
                "-Xdoclint:none",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "-package",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/C1.html", true,
                """
                    <code><a href="C2.html" title="class in p">C2</a></code>""",
                """
                    <code><a href="#%3Cinit%3E()" class="member-name-link">C1</a>()</code>""");

        checkOutput("p/C2.html", true,
                """
                    <code><a href="C3.html" title="class in p">C3</a></code>""",
                """
                    <code><a href="#%3Cinit%3E()" class="member-name-link">C2</a>()</code>""");

        checkOutput("p/C3.html", true,
                """
                    <code><a href="I1.html" title="interface in p">I1</a></code>, <code><a href="I12\
                    .html" title="interface in p">I12</a></code>, <code><a href="I2.html" title="int\
                    erface in p">I2</a></code>, <code><a href="IT1.html" title="interface in p">IT1<\
                    /a>&lt;T&gt;</code>, <code><a href="IT2.html" title="interface in p">IT2</a>&lt;\
                    java.lang.String&gt;</code>""",
                """
                    <code><a href="#%3Cinit%3E()" class="member-name-link">C3</a>()</code>""");

        checkOutput("p/I1.html", true,
                """
                    <code><a href="C3.html" title="class in p">C3</a></code>""",
                """
                    <code><a href="I12.html" title="interface in p">I12</a></code>""");

        checkOutput("p/I2.html", true,
                """
                    <code><a href="C3.html" title="class in p">C3</a></code>""",
                """
                    <code><a href="I12.html" title="interface in p">I12</a></code>""");

        checkOutput("p/I12.html", true,
                """
                    <code><a href="C3.html" title="class in p">C3</a></code>""",
                """
                    <code><a href="I1.html" title="interface in p">I1</a></code>, <code><a href="I2.\
                    html" title="interface in p">I2</a></code>""");

        checkOutput("p/IT1.html", true,
                """
                    <code><a href="C3.html" title="class in p">C3</a></code>""");

        checkOutput("p/IT2.html", true,
                """
                    code><a href="C3.html" title="class in p">C3</a></code>""");
    }
}
