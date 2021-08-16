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
 * @bug 4714257 8164407 8205593
 * @summary Test to make sure that the title attribute shows up in links.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestTitleInHref
 */

import javadoc.tester.JavadocTester;

public class TestTitleInHref extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestTitleInHref tester = new TestTitleInHref();
        tester.runTests();
    }

    @Test
    public void test() {
        String uri = "http://java.sun.com/j2se/1.4/docs/api";
        javadoc("-d", "out",
                "-source", "8",
                "-sourcepath", testSrc,
                "-linkoffline", uri, testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/Links.html", true,
                //Test to make sure that the title shows up in a class link.
                """
                    <a href="Class.html" title="class in pkg">""",
                //Test to make sure that the title shows up in an interface link.
                """
                    <a href="Interface.html" title="interface in pkg">""",
                //Test to make sure that the title shows up in cross link shows up
                "<a href=\"" + uri + """
                    /java/io/File.html" title="class or interface in java.io" class="external-link">\
                    <code>This is a cross link to class File</code></a>""");
    }
}
