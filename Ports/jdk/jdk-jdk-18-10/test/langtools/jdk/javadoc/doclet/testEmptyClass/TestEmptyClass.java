/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4483401 4483407 4483409 4483413 4494343
 * @summary Test to make sure that Javadoc behaves properly when
 * run on a completely empty class (no comments or members).
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestEmptyClass
 */

import javadoc.tester.JavadocTester;

public class TestEmptyClass extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestEmptyClass tester = new TestEmptyClass();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-classpath", testSrc("src"),
                "-d", "out",
                "-sourcepath", testSrc("src"),
                testSrc("src/Empty.java"));
        checkExit(Exit.OK);

        //The overview tree should not link to classes that were not documented
        checkOutput("overview-tree.html", false,
                "<A HREF=\"TestEmptyClass.html\">");

        //The index page should not link to classes that were not documented
        checkOutput("index-all.html", false,
                "<A HREF=\"TestEmptyClass.html\">");
    }
}
