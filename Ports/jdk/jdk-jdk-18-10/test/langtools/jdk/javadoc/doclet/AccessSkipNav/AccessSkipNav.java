/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4638136 7198273 8025633 8081854 8182765 8258659 8261976
 * @summary  Add ability to skip over nav bar for accessibility
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main AccessSkipNav
 */

import javadoc.tester.JavadocTester;

public class AccessSkipNav extends JavadocTester {

    public static void main(String... args) throws Exception {
        AccessSkipNav tester = new AccessSkipNav();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "p1", "p2");
        checkExit(Exit.OK);

        // Testing only for the presence of the <a href> and <a id>
        checkOutput("p1/C1.html", true,
                // Top navbar <a href>
                """
                    <a href="#skip-navbar-top" title="Skip navigation links">Skip navigation links</a>""",
                // Top navbar <span id>
                """
                    <span class="skip-nav" id="skip-navbar-top"></span>""");
    }
}
