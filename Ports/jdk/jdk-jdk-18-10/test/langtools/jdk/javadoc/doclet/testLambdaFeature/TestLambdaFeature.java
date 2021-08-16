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
 * @bug      8004893 8022738 8029143 8175200 8186332 8184205
 * @summary  Make sure that the lambda feature changes work fine in
 *           javadoc.
 * @library  ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* TestLambdaFeature
 * @run main TestLambdaFeature
 */

/*
 * NOTE : This test should be elided when version 1.7 support is removed from the JDK
 *              or the negative part of the test showing 1.7's non-support should be
 *              removed [ 8022738 ]
 */

import javadoc.tester.JavadocTester;

public class TestLambdaFeature extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestLambdaFeature tester = new TestLambdaFeature();
        tester.runTests();
    }

    @Test
    public void testDefault() {
        javadoc("-d", "out-default",
                "-sourcepath", testSrc,
                "pkg", "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg/A.html", true,
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab5"><code>default void</code></div>""",
                """
                    <div class="member-signature"><span class="modifiers">default</span>&nbsp;<span \
                    class="return-type">void</span>&nbsp;<span class="element-name">defaultMethod</span>()</\
                    div>
                    """,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="method-summary-table-tab0" role="tab" aria-selected="true" aria-cont\
                    rols="method-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" o\
                    nclick="show('method-summary-table', 'method-summary-table', 3)" class="active-t\
                    able-tab">All Methods</button>\
                    <button id="method-summary-table-tab2" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab2', 3)" class="t\
                    able-tab">Instance Methods</button>\
                    <button id="method-summary-table-tab3" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab3', 3)" class="t\
                    able-tab">Abstract Methods</button>\
                    <button id="method-summary-table-tab5" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab5', 3)" class="t\
                    able-tab">Default Methods</button>\
                    </div>""",
                """
                    <dl class="notes">
                    <dt>Functional Interface:</dt>
                    <dd>This is a functional interface and can therefore be used as the assignment t\
                    arget for a lambda expression or method reference.</dd>
                    </dl>""");

        checkOutput("pkg1/FuncInf.html", true,
                """
                    <dl class="notes">
                    <dt>Functional Interface:</dt>
                    <dd>This is a functional interface and can therefore be used as the assignment t\
                    arget for a lambda expression or method reference.</dd>
                    </dl>""");

        checkOutput("pkg/A.html", false,
                """
                    <td class="col-first"><code>default default void</code></td>""",
                "<pre>default&nbsp;default&nbsp;void&nbsp;defaultMethod()</pre>");

        checkOutput("pkg/B.html", false,
                """
                    <td class="col-first"><code>default void</code></td>""",
                """
                    <dl class="notes">
                    <dt>Functional Interface:</dt>""");

        checkOutput("pkg1/NotAFuncInf.html", false,
                """
                    <dl class="notes">
                    <dt>Functional Interface:</dt>
                    <dd>This is a functional interface and can therefore be used as the assignment t\
                    arget for a lambda expression or method reference.</dd>
                    </dl>""");
    }

    @Test
    public void testSource7() {
        javadoc("-d", "out-7",
                "-sourcepath", testSrc,
                "-source", "1.7",
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/FuncInf.html", false,
                """
                    <dl class="notes">
                    <dt>Functional Interface:</dt>""");
    }
}
