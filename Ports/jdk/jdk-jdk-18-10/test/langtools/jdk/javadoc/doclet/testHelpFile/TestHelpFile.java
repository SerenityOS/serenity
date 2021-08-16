/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      7132631 8241693
 * @summary  Make sure that the help file is generated correctly.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestHelpFile
 */

import javadoc.tester.JavadocTester;

public class TestHelpFile extends JavadocTester {
    /** A constant value to be documented. */
    public static final int ZERO = 0;

    public static void main(String... args) throws Exception {
        TestHelpFile tester = new TestHelpFile();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                testSrc("TestHelpFile.java"));
        checkExit(Exit.OK);

        checkOutput("help-doc.html", true,
            """
                <a href="constant-values.html">Constant Field Values</a>""");

        // check a representative sample of the contents
        checkOrder("help-doc.html",
                """
                    <section class="help-section" id="package">
                    <h3>Package</h3>""",
                """
                    <ul class="help-section-list">
                    <li>Interfaces</li>
                    <li>Classes</li>
                    <li>Enum Classes</li>""",
                """
                    </section>
                    <section class="help-section" id="class">
                    <h3>Class or Interface</h3>""",
                """
                    <ul class="help-section-list">
                    <li>Class Inheritance Diagram</li>
                    <li>Direct Subclasses</li>
                    """);
    }
}
