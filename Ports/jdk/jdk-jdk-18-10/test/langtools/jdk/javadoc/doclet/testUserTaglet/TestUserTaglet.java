/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8176836 8201817
 * @summary  Provide Taglet with context
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* InfoTaglet
 * @run main TestUserTaglet
 */

import javadoc.tester.JavadocTester;

public class TestUserTaglet extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestUserTaglet tester = new TestUserTaglet();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-tagletpath", System.getProperty("test.class.path"),
                "-taglet", "InfoTaglet",
                "pkg");
        checkExit(Exit.OK);

        // The following checks verify that information was successfully
        // derived from the context information available to the taglet.
        checkOutput("pkg/C.html", true,
            "<li>Element: CLASS C",
            "<li>Element supertypes: [java.lang.Object]",
            "<li>Doclet: class jdk.javadoc.doclet.StandardDoclet"
        );
    }
}
