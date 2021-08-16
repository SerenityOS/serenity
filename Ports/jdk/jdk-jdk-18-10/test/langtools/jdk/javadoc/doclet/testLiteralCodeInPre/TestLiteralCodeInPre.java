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
 * @bug      8002387 8014636 8078320 8175200 8186332
 * @summary  Improve rendered HTML formatting for {@code}
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestLiteralCodeInPre
 */

import javadoc.tester.JavadocTester;

public class TestLiteralCodeInPre extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestLiteralCodeInPre tester = new TestLiteralCodeInPre();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-Xdoclint:none",
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/Test.html", true,
                """
                    no_pre</span>()</div>
                    <div class="block">abc<code>def</code>ghi</div>""",
                """
                    no_pre_extra_whitespace</span>()</div>
                    <div class="block">abc<code> def  </code>ghi</div>""",
                """
                    in_pre</span>()</div>
                    <div class="block"><pre> abc<code> def  </code>ghi</pre></div>""",
                """
                    pre_after_text</span>()</div>
                    <div class="block">xyz <pre> abc<code> def  </code>ghi</pre></div>""",
                """
                    after_pre</span>()</div>
                    <div class="block">xyz <pre> pqr </pre> abc<code> def  </code>ghi</div>""",
                """
                    back_in_pre</span>()</div>
                    <div class="block">xyz <pre> pqr </pre> mno <pre> abc<code> def  </code>ghi</pre></div>""",
                """
                    typical_usage_code</span>()</div>
                    <div class="block">Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                     Example:  <pre><code>
                       line 0 @Override
                       line 1 &lt;T&gt; void m(T t) {
                       line 2     // do something with T
                       line 3 }
                     </code></pre>
                     and so it goes.</div>""",
                """
                    typical_usage_literal</span>()</div>
                    <div class="block">Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                     Example:  <pre>
                       line 0 @Override
                       line 1 &lt;T&gt; void m(T t) {
                       line 2     // do something with T
                       line 3 }
                     </pre>
                     and so it goes.</div>""",
                """
                    recommended_usage_literal</span>()</div>
                    <div class="block">Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                     Example:  <pre>
                       line 0 @Override
                       line 1 &lt;T&gt; void m(T t) {
                       line 2     // do something with T
                       line 3 } </pre>
                     and so it goes.</div>""",
                """
                    <div class="block">Test for html in pre, note the spaces
                     <PRE>
                     <b>id           </b>
                     </PRE></div>""",
                """
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">void</span>&nbsp;<span class="element-name">htmlAttrInPre1</span>()</\
                    div>
                    <div class="block">More html tag outliers.
                     <pre>
                     @Override
                     <code> some.function() </code>
                     </pre></div>""");
    }
}
