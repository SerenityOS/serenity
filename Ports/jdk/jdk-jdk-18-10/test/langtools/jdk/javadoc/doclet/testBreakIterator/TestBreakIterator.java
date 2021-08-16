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
 * @bug 4165985
 * @summary Determine the end of the first sentence using BreakIterator.
 * If the first sentence of "method" is parsed correctly, the test passes.
 * Correct Answer: "This is a class (i.e. it is indeed a class)."
 * Wrong Answer: "This is a class (i.e."
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestBreakIterator
 */

import javadoc.tester.JavadocTester;

public class TestBreakIterator extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestBreakIterator tester = new TestBreakIterator();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-breakiterator",
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/BreakIteratorTest.html", true,
            """
                <div class="block">This is a class (i.e. it is indeed a class).</div>""");

        checkOutput("pkg/BreakIteratorTest.html", true,
                """
                    <div class="block">tests the breakiterator (i.e. how the firstSentence is broken up).</div>""");

        checkOutput("pkg/BreakIteratorTest.html", true,
                """
                    <div class="block">with an inline tag <code>jdk.javadoc.taglet.Taglet</code> does it work.</div>""");

        checkOutput("pkg/BreakIteratorTest.html", true,
                """
                    <div class="block">with a block tag</div>""");

        checkOutput("pkg/BreakIteratorTest.html", true,
                """
                    <div class="block">with an anchor for the
                     <a href="../index-all.html">top level index</a>.</div>""");

        checkOutput("pkg/BreakIteratorTest.html", true,
                """
                    <div class="block">A constant indicating that the keyLocation is indeterminate
                     or not relevant.</div>""");
    }
}
