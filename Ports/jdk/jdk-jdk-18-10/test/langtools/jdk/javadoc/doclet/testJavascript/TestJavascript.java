/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4665566 4855876 7025314 8012375 8015997 8016328 8024756 8148985 8151921 8151743 8196202 8223378
 * @summary  Verify that the output has the right javascript.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestJavascript
 */

import javadoc.tester.JavadocTester;

public class TestJavascript extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestJavascript tester = new TestJavascript();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg", testSrc("TestJavascript.java"));
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", false,
                """
                    <script type="text/javascript"><!--
                    $('.navPadding').css('padding-top', $('.fixedNav').css("height"));
                    //-->
                    </script>""");

        checkOutput("index.html", false,
                """
                    <script type="text/javascript"><!--
                    $('.navPadding').css('padding-top', $('.fixedNav').css("height"));
                    //-->
                    """);

        checkOutput("script.js", false,
                """
                    $(window).resize(function() {
                            $('.navPadding').css('padding-top', $('.fixedNav').css("height"));
                        });""");
    }
}
