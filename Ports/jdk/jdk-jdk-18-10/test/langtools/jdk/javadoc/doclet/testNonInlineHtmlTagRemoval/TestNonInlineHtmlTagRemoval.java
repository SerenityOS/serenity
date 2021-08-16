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
 * @bug      8048628 8174715 8182765
 * @summary  Verify html inline tags are removed correctly in the first sentence.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestNonInlineHtmlTagRemoval
 */

import javadoc.tester.JavadocTester;

public class TestNonInlineHtmlTagRemoval extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNonInlineHtmlTagRemoval tester = new TestNonInlineHtmlTagRemoval();
        tester.runTests();
    }

    @Test
    public void testPositive() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                testSrc("C.java"));
        checkExit(Exit.ERROR);

        checkOutput(Output.OUT, true,
                "attribute not supported in HTML5: compact",
                "attribute not supported in HTML5: type");

        checkOutput("C.html", true,
                """
                    <div class="block">case1   end of sentence.</div>""",
                """
                    <div class="block">case2   end of sentence.</div>""",
                """
                    <div class="block">case3   end of sentence.</div>""",
                """
                    <div class="block">case4   end of sentence.</div>""",
                """
                    <div class="block">case5   end of sentence.</div>""",
                """
                    <div class="block">case6   end of sentence.</div>""",
                """
                    <div class="block">case7   end of sentence.</div>""",
                """
                    <div class="block">case8   end of sentence.</div>""",
                """
                    <div class="block">case9   end of sentence.</div>""",
                """
                    <div class="block">caseA   end of sentence.</div>""",
                """
                    <div class="block">caseB A block quote example:</div>""");
    }

    @Test
    public void testNegative() {
        javadoc("-d", "out2",
                "-sourcepath", testSrc,
                testSrc("Negative.java"));
        checkExit(Exit.ERROR);

        checkOutput("Negative.html", true,
                """
                    <div class="block">case1: A hanging &lt;  : xx&lt;</div>""");
    }
}
