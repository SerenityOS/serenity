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
 * @bug 4492178
 * @summary Test to make sure that hidden overridden members are not
 * documented as inherited.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestHiddenMembers
 */

import javadoc.tester.JavadocTester;

public class TestHiddenMembers extends JavadocTester {

    private static final String[][] NEGATED_TEST = {
        { }
        };
    private static final String[] ARGS =
        new String[] {

        };

    public static void main(String... args) throws Exception {
        TestHiddenMembers tester = new TestHiddenMembers();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        // We should not inherit any members from BaseClass because they are all overridden and hidden
        // (declared as private).
        // TODO: check normal case of generated tags: upper case of lower case
        checkOutput("pkg/SubClass.html", false,
            """
                inherited from class pkg.<A HREF="../pkg/BaseClass.html">BaseClass</A>""");
    }
}
