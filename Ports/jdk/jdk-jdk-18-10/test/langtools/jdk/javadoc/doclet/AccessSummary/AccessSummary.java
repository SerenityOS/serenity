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
 * @bug      4637604 4775148 8183037 8182765 8196202
 * @summary  Test the tables for summary attribute
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main AccessSummary
 */

import javadoc.tester.JavadocTester;

public class AccessSummary extends JavadocTester {
    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     * @throws Exception if the test fails
     */
    public static void main(String... args) throws Exception {
        AccessSummary tester = new AccessSummary();
        tester.runTests();
    }

    @Test
    public void testAccessSummary() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "p1", "p2");
        checkExit(Exit.OK);
        checkSummary(false);
    }

    void checkSummary(boolean found) {
        checkOutput("index.html", found,
                 """
                     summary="Package Summary table, listing packages, and an explanation\"""");

        // Test that the summary attribute appears or not
        checkOutput("p1/C1.html", found,
                 """
                     summary="Constructor Summary table, listing constructors, and an explanation\"""");

        // Test that the summary attribute appears or not
        checkOutput("constant-values.html", found,
                 """
                     summary="Constant Field Values table, listing constant fields, and values\"""");
    }
}
