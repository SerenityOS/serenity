/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      6786688 8008164 8162363 8169819 8183037 8182765 8184205 8242649 8259726
 * @summary  HTML tables should have table summary, caption and table headers.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestHtmlTableTags
 */

import javadoc.tester.JavadocTester;

public class TestHtmlTableTags extends JavadocTester {

    //Javadoc arguments.
    private static final String[] ARGS = new String[] {

    };


    public static void main(String... args) throws Exception {
        TestHtmlTableTags tester = new TestHtmlTableTags();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-use",
                "--no-platform-links",
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        checkHtmlTableTag();
        checkHtmlTableCaptions();
        checkHtmlTableHeaders();
        checkHtmlTableContents();
    }

    @Test
    public void testNoComment() {
        javadoc("-d", "out-nocomment",
                "-nocomment",
                "-sourcepath", testSrc,
                "-use",
                "--no-platform-links",
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        checkHtmlTableTag();
        checkHtmlTableCaptions();
        checkHtmlTableHeaders();
        checkHtmlTableContentsNoComment();
    }

    /*
     * Tests for validating table tag for HTML tables
     */
    void checkHtmlTableTag() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="summary-table two-column-summary" aria-labelledby="class-summary-tab0">""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="summary-table two-column-summary" aria-labelledby="class-summary-tab0">""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="summary-table three-column-summary">""",
                """
                    <div class="summary-table three-column-summary" aria-labelledby="method-summary-table-tab0">""");

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="summary-table three-column-summary">""",
                """
                    <div class="summary-table three-column-summary" aria-labelledby="method-summary-table-tab0">""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="summary-table three-column-summary">""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="summary-table three-column-summary">""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg1/class-use/C1.html", true,
                """
                    <div class="summary-table two-column-summary">""",
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="summary-table two-column-summary">""",
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="summary-table two-column-summary">""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="summary-table two-column-summary">""",
                """
                    <div class="summary-table two-column-summary">""");

        checkOutput("pkg2/package-use.html", true,
                """
                    <div class="summary-table two-column-summary">""",
                """
                    <div class="summary-table two-column-summary">""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="summary-table two-column-summary">""",
                """
                    <div class="summary-table two-column-summary">""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="summary-table three-column-summary">""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="summary-table two-column-summary">""");
    }

    /*
     * Tests for validating summary for HTML tables
     */
    void checkHtmlTableSummaries() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="type-summary">
                    <table summary="Class Summary table, listing classes, and an explanation">""",
                """
                    <div class="type-summary">
                    <table summary="Interface Summary table, listing interfaces, and an explanation">""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="type-summary">
                    <table summary="Enum Class Summary table, listing enums, and an explanation">""",
                """
                    <div class="type-summary">
                    <table summary="Annotation Interfaces Summary table, listing annotation types, and an explanation">""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="member-summary">
                    <table summary="Field Summary table, listing fields, and an explanation">""",
                "<div class=\"member-summary\">\n",
                """
                    <table summary="Method Summary table, listing methods, and an explanation" aria-labelledby="t0">""");

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="member-summary">
                    <table summary="Nested Class Summary table, listing nested classes, and an explanation">""",
                """
                    <div class="member-summary">
                    <table summary="Constructor Summary table, listing constructors, and an explanation">""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="member-summary">
                    <table summary="Enum Constant Summary table, listing enum constants, and an explanation">""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="member-summary">
                    <table summary="Required Element Summary table, listing required elements, and an explanation">""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="member-summary">
                    <table summary="Optional Element Summary table, listing optional elements, and an explanation">""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing packages, and an explanation">""");

        checkOutput("pkg1/class-use/C1.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing fields, and an explanation">""",
                """
                    <div class="use-summary">
                    <table summary="Use table, listing methods, and an explanation">""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing fields, and an explanation">""",
                """
                    <div class="use-summary">
                    <table summary="Use table, listing methods, and an explanation">""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing packages, and an explanation">""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing methods, and an explanation">""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing packages, and an explanation">""",
                """
                    <div class="use-summary">
                    <table summary="Use table, listing classes, and an explanation">""");

        checkOutput("pkg2/package-use.html", true,
                """
                    <div class="use-summary">
                    <table summary="Use table, listing packages, and an explanation">""",
                """
                    <div class="use-summary">
                    <table summary="Use table, listing classes, and an explanation">""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="deprecated-summary" id="field">
                    <table summary="Fields table, listing fields, and an explanation">""",
                """
                    <div class="deprecated-summary" id="method">
                    <table summary="Methods table, listing methods, and an explanation">""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="constants-summary">
                    <table summary="Constant Field Values table, listing constant fields, and values">""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="overview-summary" id="all-packages">
                    <table summary="Package Summary table, listing packages, and an explanation">""");
    }

    /*
     * Tests for validating caption for HTML tables
     */
    void checkHtmlTableCaptions() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id=\
                    "class-summary-tab0" role="tab" aria-selected="true" aria-controls="class-summar\
                    y.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="show('class-summa\
                    ry', 'class-summary', 2)" class="active-table-tab">All Classes and Interfaces</b\
                    utton>\
                    <button id="class-summary-tab1" role="tab" aria-selected="false" aria-controls="\
                    class-summary.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show\
                    ('class-summary', 'class-summary-tab1', 2)" class="table-tab">Interfaces</button\
                    >\
                    <button id="class-summary-tab2" role="tab" aria-selected="false" aria-controls="\
                    class-summary.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show\
                    ('class-summary', 'class-summary-tab2', 2)" class="table-tab">Classes</button></\
                    div>""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id=\
                    "class-summary-tab0" role="tab" aria-selected="true" aria-controls="class-summar\
                    y.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="show('class-summa\
                    ry', 'class-summary', 2)" class="active-table-tab">All Classes and Interfaces</b\
                    utton>\
                    <button id="class-summary-tab2" role="tab" aria-selected="false" aria-controls="\
                    class-summary.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show\
                    ('class-summary', 'class-summary-tab2', 2)" class="table-tab">Classes</button>\
                    <button id="class-summary-tab3" role="tab" aria-selected="false" aria-controls="\
                    class-summary.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show\
                    ('class-summary', 'class-summary-tab3', 2)" class="table-tab">Enum Classes</butt\
                    on>\
                    <button id="class-summary-tab7" role="tab" aria-selected="false" aria-controls="\
                    class-summary.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show\
                    ('class-summary', 'class-summary-tab7', 2)" class="table-tab">Annotation Interfa\
                    ces</button></div>""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="caption"><span>Fields</span></div>""",
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
                    <button id="method-summary-table-tab4" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab4', 3)" class="t\
                    able-tab">Concrete Methods</button>\
                    <button id="method-summary-table-tab6" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab6', 3)" class="t\
                    able-tab">Deprecated Methods</button>\
                    </div>
                    """);

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="caption"><span>Nested Classes</span></div>""",
                """
                    <div class="caption"><span>Constructors</span></div>""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="caption"><span>Enum Constants</span></div>""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="caption"><span>Required Elements</span></div>""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="caption"><span>Optional Elements</span></div>""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="caption"><span>Packages that use <a href="../I1.html" title="interface in pkg1">I1</a></span></div>""");

        checkOutput("pkg1/class-use/C1.html", true,
                """
                    <div class="caption"><span>Fields in <a href="../../pkg2/package-summary.html">pkg2</a> decl\
                    ared as <a href="../C1.html" title="class in pkg1">C1</a></span></div>""",
                """
                    <div class="caption"><span>Methods in <a href="../../pkg2/package-summary.html">pkg2</a> tha\
                    t return <a href="../C1.html" title="class in pkg1">C1</a></span></div>""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="caption"><span>Fields in <a href="../../pkg1/package-summary.html">pkg1</a> decl\
                    ared as <a href="../C2.html" title="class in pkg2">C2</a></span></div>""",
                """
                    <div class="caption"><span>Methods in <a href="../../pkg1/package-summary.html">pkg1</a> tha\
                    t return <a href="../C2.html" title="class in pkg2">C2</a></span></div>""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="caption"><span>Methods in <a href="../package-summary.html">pkg2</a> that return\
                     <a href="../C2.ModalExclusionType.html" title="enum class in pkg2">C2.ModalExclusionT\
                    ype</a></span></div>""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="caption"><span>Packages that use <a href="package-summary.html">pkg1</a></span></div>""",
                """
                    <div class="caption"><span>Classes in <a href="package-summary.html">pkg1</a> used by <a hre\
                    f="package-summary.html">pkg1</a></span></div>""");

        checkOutput("pkg2/package-use.html", true,
                """
                    <div class="caption"><span>Packages that use <a href="package-summary.html">pkg2</a></span></div>""",
                """
                    <div class="caption"><span>Classes in <a href="package-summary.html">pkg2</a> used by <a hre\
                    f="../pkg1/package-summary.html">pkg1</a></span></div>""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="caption"><span>Deprecated Fields</span></div>""",
                """
                    <div class="caption"><span>Deprecated Methods</span></div>""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="caption"><span>pkg1.<a href="pkg1/C1.html" title="class in pkg1">C1</a></span></div>""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="caption"><span>Packages</span></div>""");
    }

    /*
     * Test for validating headers for HTML tables
     */
    void checkHtmlTableHeaders() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Field</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Class</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Constructor</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="table-header col-first">Enum Constant</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Required Element</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Optional Element</div>
                    <div class="table-header col-last">Description</div>""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg1/class-use/C1.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Field</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Field</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/class-use/C2.ModalExclusionType.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>""");

        checkOutput("pkg2/package-use.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="table-header col-first">Field</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="table-header col-first">Method</div>
                    <div class="table-header col-last">Description</div>""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Constant Field</div>
                    <div class="table-header col-last">Value</div>""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""");
    }

    /*
     * Test for validating HTML table contents.
     */
    void checkHtmlTableContents() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="col-first odd-row-color class-summary class-summary-tab1"><a \
                    href="I1.html" title="interface in pkg1">I1</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab1">
                    <div class="block">A sample interface used to test table tags.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color class-summary class-summary-tab2"><a\
                     href="C1.html" title="class in pkg1">C1</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab2">
                    <div class="block">A test class.</div>
                    </div>""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="col-first odd-row-color class-summary class-summary-tab3"><a \
                    href="C2.ModalExclusionType.html" title="enum class in pkg2">C2.ModalExclusionType</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab3">
                    <div class="block">A sample enum.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color class-summary class-summary-tab7"><a\
                     href="C3.html" title="annotation interface in pkg2">C3</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab7">
                    <div class="block">Test Annotation class.</div>
                    </div>""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="col-first odd-row-color"><code><a href="../pkg2/C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second odd-row-color"><code><a href="#field" class="member-name-link">field</a></code></div>
                    <div class="col-last odd-row-color">
                    <div class="block">Test field for class.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>void</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#method1(int,int)" class="member-\
                    name-link">method1</a><wbr>(int&nbsp;a,
                     int&nbsp;b)</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">
                    <div class="block">Method that is implemented.</div>
                    </div>""");

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="col-first even-row-color"><code><a href="../pkg1/C1.html" title="class in pkg1">C1</a></code></div>
                    <div class="col-second even-row-color"><code><a href="#field" class="member-name-link">field</a></code></div>
                    <div class="col-last even-row-color">
                    <div class="block">A test field.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="../pkg1/C1.html" title="class in p\
                    kg1">C1</a></code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#method(pkg1.C1)" class="member-n\
                    ame-link">method</a><wbr>(<a href="../pkg1/C1.html" title="class in pkg1">C1</a>\
                    &nbsp;param)</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">
                    <div class="block">A sample method.</div>
                    </div>""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="col-first odd-row-color"><code><a href="#NO_EXCLUDE" class="member-n\
                    ame-link">NO_EXCLUDE</a></code></div>
                    <div class="col-last odd-row-color">
                    <div class="block">Test comment.</div>
                    </div>""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="col-second even-row-color"><code><a href="#value()" class="member-name-link">value</a></code></div>
                    <div class="col-last even-row-color">
                    <div class="block">Comment.</div>
                    </div>""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="col-first even-row-color"><code>boolean</code></div>
                    <div class="col-second even-row-color"><code><a href="#value()" class="member-name-link">value</a></code></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="col-first even-row-color"><a href="#pkg1">pkg1</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">Test package 1 used to test table tags.</div>
                    </div>""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="col-first even-row-color"><code><a href="../C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second even-row-color"><span class="type-name-label">C1.</span><\
                    code><a href="../../pkg1/C1.html#field" class="member-name-link">field</a></code></div>
                    <div class="col-last even-row-color">
                    <div class="block">Test field for class.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color"><code><a href="../C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second even-row-color"><span class="type-name-label">C1.</span><\
                    code><a href="../../pkg1/C1.html#method(pkg2.C2)" class="member-name-link">metho\
                    d</a><wbr>(<a href="../C2.html" title="class in pkg2">C2</a>&nbsp;param)</code><\
                    /div>
                    <div class="col-last even-row-color">
                    <div class="block">Method thats does some processing.</div>
                    </div>""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="col-first even-row-color"><a href="#pkg1">pkg1</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">Test package 1 used to test table tags.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color"><a href="class-use/C1.html#pkg2">C1</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">A test class.</div>
                    </div>""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="col-summary-item-name even-row-color"><a href="pkg2/C2.html#dep_field">pkg2.C2.dep_field</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">don't use this field anymore.</div>
                    </div>""",
                """
                    <div class="col-summary-item-name even-row-color"><a href="pkg1/C1.html#deprecat\
                    edMethod()">pkg1.C1.deprecatedMethod()</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">don't use this anymore.</div>
                    </div>""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="col-first even-row-color"><code id="pkg1.C1.CONSTANT1">public&nbsp;s\
                    tatic&nbsp;final&nbsp;java.lang.String</code></div>
                    <div class="col-second even-row-color"><code><a href="pkg1/C1.html#CONSTANT1">CONSTANT1</a></code></div>
                    <div class="col-last even-row-color"><code>"C1"</code></div>
                    </div>""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="col-first even-row-color all-packages-table all-packages-table-tab1"\
                    ><a href="pkg1/package-summary.html">pkg1</a></div>
                    <div class="col-last even-row-color all-packages-table all-packages-table-tab1">
                    <div class="block">Test package 1 used to test table tags.</div>
                    </div>""");
    }

    /*
     * Test for validating HTML table contents with -nocomment option.
     */
    void checkHtmlTableContentsNoComment() {
        //Package summary
        checkOutput("pkg1/package-summary.html", true,
                """
                    <div class="col-first odd-row-color class-summary class-summary-tab1"><a \
                    href="I1.html" title="interface in pkg1">I1</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab1"></div>""",
                """
                    <div class="col-first even-row-color class-summary class-summary-tab2"><a\
                     href="C1.html" title="class in pkg1">C1</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab2"></div>""");

        checkOutput("pkg2/package-summary.html", true,
                """
                    <div class="col-first odd-row-color class-summary class-summary-tab3"><a \
                    href="C2.ModalExclusionType.html" title="enum class in pkg2">C2.ModalExclusionTyp\
                    e</a></div>
                    <div class="col-last odd-row-color class-summary class-summary-tab3"></div>""",
                """
                    <div class="col-first even-row-color class-summary class-summary-tab7"><a\
                     href="C3.html" title="annotation interface in pkg2">C3</a></div>
                    <div class="col-last even-row-color class-summary class-summary-tab7"></div>""");

        // Class documentation
        checkOutput("pkg1/C1.html", true,
                """
                    <div class="col-first odd-row-color"><code><a href="../pkg2/C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second odd-row-color"><code><a href="#field" class="member-name-link">field</a></code></div>
                    <div class="col-last odd-row-color"></div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>void</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#method1(int,int)" class="member-\
                    name-link">method1</a><wbr>(int&nbsp;a,
                     int&nbsp;b)</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"></div>""");

        checkOutput("pkg2/C2.html", true,
                """
                    <div class="col-first even-row-color"><code><a href="../pkg1/C1.html" title="class in pkg1">C1</a></code></div>
                    <div class="col-second even-row-color"><code><a href="#field" class="member-name-link">field</a></code></div>
                    <div class="col-last even-row-color"></div>""",
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="../pkg1/C1.html" title="class in p\
                    kg1">C1</a></code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#method(pkg1.C1)" class="member-n\
                    ame-link">method</a><wbr>(<a href="../pkg1/C1.html" title="class in pkg1">C1</a>\
                    &nbsp;param)</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"></div>""");

        checkOutput("pkg2/C2.ModalExclusionType.html", true,
                """
                    <div class="col-first odd-row-color"><code><a href="#NO_EXCLUDE" class="member-n\
                    ame-link">NO_EXCLUDE</a></code></div>
                    <div class="col-last odd-row-color"></div>""");

        checkOutput("pkg2/C3.html", true,
                """
                    <div class="col-second even-row-color"><code><a href="#value()" class="member-na\
                    me-link">value</a></code></div>
                    <div class="col-last even-row-color"></div>""");

        checkOutput("pkg2/C4.html", true,
                """
                    <div class="col-first even-row-color"><code>boolean</code></div>
                    <div class="col-second even-row-color"><code><a href="#value()" class="member-na\
                    me-link">value</a></code></div>
                    <div class="col-last even-row-color"></div>
                    </div>""");

        // Class use documentation
        checkOutput("pkg1/class-use/I1.html", true,
                """
                    <div class="col-first even-row-color"><a href="#pkg1">pkg1</a></div>
                    <div class="col-last even-row-color"></div>""");

        checkOutput("pkg2/class-use/C2.html", true,
                """
                    <div class="col-first even-row-color"><code><a href="../C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second even-row-color"><span class="type-name-label">C1.</span><\
                    code><a href="../../pkg1/C1.html#field" class="member-name-link">field</a></code\
                    ></div>
                    <div class="col-last even-row-color"></div>""",
                """
                    <div class="col-first even-row-color"><code><a href="../C2.html" title="class in pkg2">C2</a></code></div>
                    <div class="col-second even-row-color"><span class="type-name-label">C1.</span><\
                    code><a href="../../pkg1/C1.html#method(pkg2.C2)" class="member-name-link">metho\
                    d</a><wbr>(<a href="../C2.html" title="class in pkg2">C2</a>&nbsp;param)</code></div>
                    <div class="col-last even-row-color"></div>""");

        // Package use documentation
        checkOutput("pkg1/package-use.html", true,
                """
                    <div class="col-first even-row-color"><a href="#pkg1">pkg1</a></div>
                    <div class="col-last even-row-color"></div>""",
                """
                    <div class="col-first even-row-color"><a href="class-use/C1.html#pkg2">C1</a></div>
                    <div class="col-last even-row-color"></div>""");

        // Deprecated
        checkOutput("deprecated-list.html", true,
                """
                    <div class="col-summary-item-name even-row-color"><a href="pkg2/C2.html#dep_field">pkg2.C2.dep_field</a></div>
                    <div class="col-last even-row-color"></div>""",
                """
                    <div class="col-summary-item-name even-row-color"><a href="pkg1/C1.html#deprecat\
                    edMethod()">pkg1.C1.deprecatedMethod()</a></div>
                    <div class="col-last even-row-color"></div>""");

        // Constant values
        checkOutput("constant-values.html", true,
                """
                    <div class="col-first even-row-color"><code id="pkg1.C1.CONSTANT1">public&nbsp;s\
                    tatic&nbsp;final&nbsp;java.lang.String</code></div>
                    <div class="col-second even-row-color"><code><a href="pkg1/C1.html#CONSTANT1">CONSTANT1</a></code></div>
                    <div class="col-last even-row-color"><code>"C1"</code></div>
                    </div>""");

        // Overview Summary
        checkOutput("index.html", true,
                """
                    <div class="col-first even-row-color all-packages-table all-packages-table-tab1"\
                    ><a href="pkg1/package-summary.html">pkg1</a></div>
                    <div class="col-last even-row-color all-packages-table all-packages-table-tab1"></div>""");
    }
}
