/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      7052170 8047745
 * @summary  Run a test on -charset to make sure the charset gets generated as a
 *           part of the meta tag.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestCharset
 */

import javadoc.tester.JavadocTester;

public class TestCharset extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestCharset tester = new TestCharset();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-charset", "ISO-8859-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("index.html", true,
            """
                <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">""");
        checkOutput("pkg/Foo.html", true,
            """
                <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">""");

        checkOutput("index.html", false,
            """
                <meta http-equiv="Content-Type" content="text/html" charset="ISO-8859-1">""");
        checkOutput("pkg/Foo.html", false,
            """
                <meta http-equiv="Content-Type" content="text/html" charset="ISO-8859-1">""");
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("index.html", true,
            """
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">""");
        checkOutput("pkg/Foo.html", true,
            """
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">""");
    }
}
