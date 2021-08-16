/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      7180906 8026567 8239804
 * @summary  Test to make sure that the since tag works correctly
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestSinceTag
 */

import javadoc.tester.JavadocTester;

public class TestSinceTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSinceTag tester = new TestSinceTag();
        tester.runTests();
        tester.printSummary();
    }

    @Test
    public void testSince() {
        javadoc("-d", "out-since",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);

        checkSince(true);
    }

    @Test
    public void testNoSince() {
        javadoc("-d", "out-nosince",
                "-sourcepath", testSrc,
                "-nosince",
                "pkg1");
        checkExit(Exit.OK);

        checkSince(false);
    }

    void checkSince(boolean on) {
        checkOutput("pkg1/C1.html", on,
                """
                    <dl class="notes">
                    <dt>Since:</dt>
                    <dd>JDK1.0</dd>""");

        checkOutput("serialized-form.html", on,
                """
                    <dl class="notes">
                    <dt>Since:</dt>
                    <dd>1.4</dd>""");
    }
}
