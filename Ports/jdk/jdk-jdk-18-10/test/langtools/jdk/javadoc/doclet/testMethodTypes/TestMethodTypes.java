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
 * @bug      8002304 8024096 8193671 8196201 8203791 8184205
 * @summary  Test for various method type tabs in the method summary table
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestMethodTypes
 */

import javadoc.tester.JavadocTester;

public class TestMethodTypes extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMethodTypes tester = new TestMethodTypes();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/A.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="method-summary-table-tab0" role="tab" aria-selected="true" aria-cont\
                    rols="method-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" o\
                    nclick="show('method-summary-table', 'method-summary-table', 3)" class="active-t\
                    able-tab">All Methods</button>\
                    <button id="method-summary-table-tab1" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab1', 3)" class="t\
                    able-tab">Static Methods</button>\
                    <button id="method-summary-table-tab2" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab2', 3)" class="t\
                    able-tab">Instance Methods</button>\
                    <button id="method-summary-table-tab4" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab4', 3)" class="t\
                    able-tab">Concrete Methods</button>\
                    <button id="method-summary-table-tab6" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab6', 3)" class="t\
                    able-tab">Deprecated Methods</button>\
                    </div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4">""");

        checkOutput("pkg1/B.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="method-summary-table-tab0" role="tab" aria-selected="true" aria-cont\
                    rols="method-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" o\
                    nclick="show('method-summary-table', 'method-summary-table', 3)" class="active-t\
                    able-tab">All Methods</button>\
                    <button id="method-summary-table-tab1" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab1', 3)" class="t\
                    able-tab">Static Methods</button>\
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
                    </div>""");

        checkOutput("pkg1/D.html", true,
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
                    <button id="method-summary-table-tab4" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab4', 3)" class="t\
                    able-tab">Concrete Methods</button>\
                    <button id="method-summary-table-tab6" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab6', 3)" class="t\
                    able-tab">Deprecated Methods</button>\
                    </div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4 method-summary-table-tab6">""");

        checkOutput("pkg1/A.html", false,
                "<div class=\"caption\"><span>Methods</span></div>");

        checkOutput("pkg1/B.html", false,
                "<div class=\"caption\"><span>Methods</span></div>");

        checkOutput("pkg1/D.html", false,
                "<div class=\"caption\"><span>Methods</span></div>");
    }
}
