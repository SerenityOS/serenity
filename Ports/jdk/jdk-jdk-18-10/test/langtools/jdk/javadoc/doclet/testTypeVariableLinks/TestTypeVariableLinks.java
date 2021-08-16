/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174805 8182765
 * @summary JavacTrees should use Types.skipTypeVars() to get the upper bound of type variables
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestTypeVariableLinks
 */

import javadoc.tester.JavadocTester;

public class TestTypeVariableLinks extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestTypeVariableLinks tester = new TestTypeVariableLinks();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "-package",
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/C.html", true,
                """
                    <div class="block">Linking to Object.equals() <code>Object.equals(Object)</code></div>""");
        checkOutput("pkg1/C.html", true,
                """
                    <div class="block">Linking to List.clear() <code>List.clear()</code></div>""");
        checkOutput("pkg1/C.html", true,
                """
                    <div class="block">Linking to Additional.doAction() <a href="Additional.html#doA\
                    ction()"><code>Additional.doAction()</code></a></div>""");
        checkOutput("pkg1/C.html", true,
                """
                    <div class="block">Linking to I.abstractAction() <a href="I.html#abstractAction(\
                    )"><code>I.abstractAction()</code></a></div>""");
    }
}
