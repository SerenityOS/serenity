/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8173425 8186332 8182765 8196202
 * @summary  tests for the summary tag behavior
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestSummaryTag
 */

import javadoc.tester.JavadocTester;

public class TestSummaryTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSummaryTag tester = new TestSummaryTag();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "p1");
        checkExit(Exit.OK);

        checkOutput("index-all.html", true,
            """
                <dl class="index">
                <dt><a href="p1/A.html#m()" class="member-name-link">m()</a> - Method in class p1.<a\
                 href="p1/A.html" title="class in p1">A</a></dt>
                <dd>
                <div class="block">First sentence</div>
                </dd>
                <dt><a href="p1/B.html#m()" class="member-name-link">m()</a> - Method in class p1.<a\
                 href="p1/B.html" title="class in p1">B</a></dt>
                <dd>
                <div class="block">First sentence</div>
                </dd>
                <dt><a href="p1/A.html#m1()" class="member-name-link">m1()</a> - Method in class p1.\
                <a href="p1/A.html" title="class in p1">A</a></dt>
                <dd>
                <div class="block"> First sentence</div>
                </dd>
                <dt><a href="p1/A.html#m2()" class="member-name-link">m2()</a> - Method in class p1.\
                <a href="p1/A.html" title="class in p1">A</a></dt>
                <dd>
                <div class="block">Some html &lt;foo&gt; &nbsp; codes</div>
                </dd>
                <dt><a href="p1/A.html#m3()" class="member-name-link">m3()</a> - Method in class p1.\
                <a href="p1/A.html" title="class in p1">A</a></dt>
                <dd>
                <div class="block">First sentence</div>
                </dd>
                <dt><a href="p1/A.html#m4()" class="member-name-link">m4()</a> - Method in class p1.\
                <a href="p1/A.html" title="class in p1">A</a></dt>
                <dd>
                <div class="block">First sentence i.e. the first sentence</div>
                </dd>
                </dl>
                """,
            """
                <div class="block">The first... line</div>
                """
        );

        // make sure the second @summary's content is displayed correctly
        checkOutput("p1/A.html", true,
             """
                 <section class="detail" id="m3()">
                 <h3>m3</h3>
                 <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                 lass="return-type">void</span>&nbsp;<span class="element-name">m3</span>()</div>
                 <div class="block">First sentence  some text maybe second sentence.</div>
                 </section>
                 """
        );

        checkOutput("p1/package-summary.html", true,
                """
                    <div class="block">The first... line second from ...</div>""");
    }

    @Test
    public void test2() {
        javadoc("-d", "out2",
                "-sourcepath", testSrc,
                "p2");
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true, "package.html:3: warning: invalid use of @summary");

        checkOutput("index-all.html", true, """
            <div class="block">foo bar</div>
            """);

        checkOutput("p2/package-summary.html", true, """
            <div class="block">foo bar baz.</div>
            """);
    }

    @Test
    public void test3() {
        javadoc("-d", "out3",
                "-sourcepath", testSrc,
                "-overview", testSrc("p3/overview.html"),
                "p3");
        checkExit(Exit.OK);

        checkOutput("index.html", true,
                """
                    <div class="block">The first... line second from ...</div>""");
    }
}
