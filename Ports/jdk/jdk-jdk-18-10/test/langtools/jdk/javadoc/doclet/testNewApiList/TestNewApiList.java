/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8263468
 * @summary  New page for "recent" new API
 * @library  ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestNewApiList
 */

import javadoc.tester.JavadocTester;

/**
 * Test --since option and "New API" list.
 */
public class TestNewApiList extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNewApiList test = new TestNewApiList();
        test.runTests();
    }

    @Test
    public void testMultiRelease() throws Exception {
        javadoc("-d", "out-multi",
                "--no-platform-links",
                "--module-source-path", testSrc,
                "--since", "0.9,v1.0,1.2,2.0b,3.2,5",
                "--since-label", "New API in recent releases",
                "--module", "mdl",
                "pkg");
        checkExit(Exit.OK);
        checkMultiReleaseContents();
        checkMultiReleaseNewElements();
        checkMultiReleaseDeprecatedElements();
    }

    @Test
    public void testSingleRelease() throws Exception {
        javadoc("-d", "out-single",
                "--no-platform-links",
                "--module-source-path", testSrc,
                "--since", "5",
                "--module", "mdl",
                "pkg");
        checkExit(Exit.OK);
        checkSingleReleaseContents();
        checkSingleReleaseNewElements();
        checkSingleReleaseDeprecatedElements();
    }

    @Test
    public void testPackage() throws Exception {
        javadoc("-d", "out-package",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "--since", "1.2,2.0b,3.2,5,6",
                "pkg");
        checkExit(Exit.OK);
        checkPackageContents();
        checkPackageNewElements();
        checkPackageDeprecatedElements();
    }

    @Test
    public void testNoList() throws Exception {
        javadoc("-d", "out-none",
                "--no-platform-links",
                "--module-source-path", testSrc,
                "--since", "foo,bar",
                "--since-label", "New API in foo and bar",
                "--module", "mdl",
                "pkg");
        checkExit(Exit.OK);
        checkFiles(false, "new-list.html");
    }

    private void checkMultiReleaseContents() {
        checkOutput("new-list.html", true,
            """
                <h1 title="New API in recent releases" class="title">New API in recent releases</h1>
                <h2 title="Contents">Contents</h2>
                <ul>
                <li><a href="#module">Modules</a></li>
                <li><a href="#package">Packages</a></li>
                <li><a href="#interface">Interfaces</a></li>
                <li><a href="#class">Classes</a></li>
                <li><a href="#enum-class">Enum Classes</a></li>
                <li><a href="#exception">Exceptions</a></li>
                <li><a href="#error">Errors</a></li>
                <li><a href="#record-class">Record Classes</a></li>
                <li><a href="#annotation-interface">Annotation Interfaces</a></li>
                <li><a href="#field">Fields</a></li>
                <li><a href="#method">Methods</a></li>
                <li><a href="#constructor">Constructors</a></li>
                <li><a href="#enum-constant">Enum Constants</a></li>
                <li><a href="#annotation-interface-member">Annotation Interface Elements</a></li>
                </ul>
                </div>
                <span class="help-note">(The leftmost tab "New ..." indicates all the new elements, \
                regardless of the releases in which they were added. Each of the other tabs "Added i\
                n ..." indicates the new elements added in a specific release. Any element shown und\
                er the leftmost tab is also shown under one of the righthand tabs.)</span>""");
    }

    private void checkMultiReleaseNewElements() {
        checkOutput("new-list.html", true,
            """
                <div id="module">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="mod\
                ule-tab0" role="tab" aria-selected="true" aria-controls="module.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('module', 'module', 2)" class="active-\
                table-tab">New Modules</button><button id="module-tab2" role="tab" aria-selected="fa\
                lse" aria-controls="module.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncl\
                ick="show('module', 'module-tab2', 2)" class="table-tab">Added in 3.2</button></div>
                <div id="module.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="module-tab0">
                <div class="table-header col-first">Module</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color module module-tab2"><a href="mdl/module-summary.html">mdl</a></div>
                <div class="col-last even-row-color module module-tab2">
                <div class="block">Module mdl.</div>
                </div>""",
            """
                <div id="package">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="pac\
                kage-tab0" role="tab" aria-selected="true" aria-controls="package.tabpanel" tabindex\
                ="0" onkeydown="switchTab(event)" onclick="show('package', 'package', 2)" class="act\
                ive-table-tab">New Packages</button><button id="package-tab5" role="tab" aria-select\
                ed="false" aria-controls="package.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                )" onclick="show('package', 'package-tab5', 2)" class="table-tab">Added in v1.0</but\
                ton></div>
                <div id="package.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="package-tab0">
                <div class="table-header col-first">Package</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color package package-tab5"><a href="mdl/pkg/package-summary.html">pkg</a></div>
                <div class="col-last even-row-color package package-tab5">
                <div class="block">Package pkg.</div>
                </div>""",
            """
                <div id="interface">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="int\
                erface-tab0" role="tab" aria-selected="true" aria-controls="interface.tabpanel" tabi\
                ndex="0" onkeydown="switchTab(event)" onclick="show('interface', 'interface', 2)" cl\
                ass="active-table-tab">New Interfaces</button><button id="interface-tab6" role="tab"\
                 aria-selected="false" aria-controls="interface.tabpanel" tabindex="-1" onkeydown="s\
                witchTab(event)" onclick="show('interface', 'interface-tab6', 2)" class="table-tab">\
                Added in 0.9</button></div>
                <div id="interface.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="interface-tab0">
                <div class="table-header col-first">Interface</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color interface interface-tab6"><a href="\
                mdl/pkg/TestInterface.html" title="interface in pkg">pkg.TestInterface</a></div>
                <div class="col-last even-row-color interface interface-tab6">
                <div class="block">Test interface.</div>
                </div>""",
            """
                <div id="class">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="cla\
                ss-tab0" role="tab" aria-selected="true" aria-controls="class.tabpanel" tabindex="0"\
                 onkeydown="switchTab(event)" onclick="show('class', 'class', 2)" class="active-tabl\
                e-tab">New Classes</button><button id="class-tab4" role="tab" aria-selected="false" \
                aria-controls="class.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="s\
                how('class', 'class-tab4', 2)" class="table-tab">Added in 1.2</button></div>
                <div id="class.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="class-tab0">
                <div class="table-header col-first">Class</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color class class-tab4"><a href="mdl/pkg/\
                TestClass.html" title="class in pkg">pkg.TestClass</a></div>
                <div class="col-last even-row-color class class-tab4">
                <div class="block">TestClass declaration.</div>
                </div>""",
            """
                <div id="enum-class">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="enu\
                m-class-tab0" role="tab" aria-selected="true" aria-controls="enum-class.tabpanel" ta\
                bindex="0" onkeydown="switchTab(event)" onclick="show('enum-class', 'enum-class', 2)\
                " class="active-table-tab">New Enum Classes</button><button id="enum-class-tab6" rol\
                e="tab" aria-selected="false" aria-controls="enum-class.tabpanel" tabindex="-1" onke\
                ydown="switchTab(event)" onclick="show('enum-class', 'enum-class-tab6', 2)" class="t\
                able-tab">Added in 0.9</button></div>
                <div id="enum-class.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="enum-class-tab0">
                <div class="table-header col-first">Enum Class</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color enum-class enum-class-tab6"><a href\
                ="mdl/pkg/TestEnum.html" title="enum class in pkg">pkg.TestEnum</a></div>
                <div class="col-last even-row-color enum-class enum-class-tab6">
                <div class="block">Test enum class.</div>
                </div>""",
            """
                <div id="exception">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="exc\
                eption-tab0" role="tab" aria-selected="true" aria-controls="exception.tabpanel" tabi\
                ndex="0" onkeydown="switchTab(event)" onclick="show('exception', 'exception', 2)" cl\
                ass="active-table-tab">New Exceptions</button><button id="exception-tab6" role="tab"\
                 aria-selected="false" aria-controls="exception.tabpanel" tabindex="-1" onkeydown="s\
                witchTab(event)" onclick="show('exception', 'exception-tab6', 2)" class="table-tab">\
                Added in 0.9</button></div>
                <div id="exception.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="exception-tab0">
                <div class="table-header col-first">Exceptions</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color exception exception-tab6"><a href="\
                mdl/pkg/TestException.html" title="class in pkg">pkg.TestException</a></div>
                <div class="col-last even-row-color exception exception-tab6">
                <div class="block">Test exception class.</div>
                </div>""",
            """
                <div id="error">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="err\
                or-tab0" role="tab" aria-selected="true" aria-controls="error.tabpanel" tabindex="0"\
                 onkeydown="switchTab(event)" onclick="show('error', 'error', 2)" class="active-tabl\
                e-tab">New Errors</button><button id="error-tab3" role="tab" aria-selected="false" a\
                ria-controls="error.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="sh\
                ow('error', 'error-tab3', 2)" class="table-tab">Added in 2.0b</button></div>
                <div id="error.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="error-tab0">
                <div class="table-header col-first">Errors</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color error error-tab3"><a href="mdl/pkg/\
                TestError.html" title="class in pkg">pkg.TestError</a></div>
                <div class="col-last even-row-color error error-tab3">
                <div class="block">Test error class.</div>
                </div>""",
            """
                <div id="record-class">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="rec\
                ord-class-tab0" role="tab" aria-selected="true" aria-controls="record-class.tabpanel\
                " tabindex="0" onkeydown="switchTab(event)" onclick="show('record-class', 'record-cl\
                ass', 2)" class="active-table-tab">New Record Classes</button><button id="record-cla\
                ss-tab2" role="tab" aria-selected="false" aria-controls="record-class.tabpanel" tabi\
                ndex="-1" onkeydown="switchTab(event)" onclick="show('record-class', 'record-class-t\
                ab2', 2)" class="table-tab">Added in 3.2</button></div>
                <div id="record-class.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="record-class-tab0">
                <div class="table-header col-first">Record Class</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color record-class record-class-tab2"><a \
                href="mdl/pkg/TestRecord.html" title="class in pkg">pkg.TestRecord</a></div>
                <div class="col-last even-row-color record-class record-class-tab2">
                <div class="block">Test record.</div>
                </div>""",
            """
                <div id="annotation-interface">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="ann\
                otation-interface-tab0" role="tab" aria-selected="true" aria-controls="annotation-in\
                terface.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="show('annotatio\
                n-interface', 'annotation-interface', 2)" class="active-table-tab">New Annotation In\
                terfaces</button><button id="annotation-interface-tab3" role="tab" aria-selected="fa\
                lse" aria-controls="annotation-interface.tabpanel" tabindex="-1" onkeydown="switchTa\
                b(event)" onclick="show('annotation-interface', 'annotation-interface-tab3', 2)" cla\
                ss="table-tab">Added in 2.0b</button></div>
                <div id="annotation-interface.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="annotation-interface-tab0">
                <div class="table-header col-first">Annotation Interface</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color annotation-interface annotation-int\
                erface-tab3"><a href="mdl/pkg/TestAnnotation.html" title="annotation interface in pk\
                g">pkg.TestAnnotation</a></div>
                <div class="col-last even-row-color annotation-interface annotation-interface-tab3">
                <div class="block">An annotation interface.</div>
                </div>""",
            """
                <div id="field">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="fie\
                ld-tab0" role="tab" aria-selected="true" aria-controls="field.tabpanel" tabindex="0"\
                 onkeydown="switchTab(event)" onclick="show('field', 'field', 2)" class="active-tabl\
                e-tab">New Fields</button><button id="field-tab2" role="tab" aria-selected="false" a\
                ria-controls="field.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="sh\
                ow('field', 'field-tab2', 2)" class="table-tab">Added in 3.2</button><button id="fie\
                ld-tab3" role="tab" aria-selected="false" aria-controls="field.tabpanel" tabindex="-\
                1" onkeydown="switchTab(event)" onclick="show('field', 'field-tab3', 2)" class="tabl\
                e-tab">Added in 2.0b</button><button id="field-tab4" role="tab" aria-selected="false\
                " aria-controls="field.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick=\
                "show('field', 'field-tab4', 2)" class="table-tab">Added in 1.2</button><button id="\
                field-tab5" role="tab" aria-selected="false" aria-controls="field.tabpanel" tabindex\
                ="-1" onkeydown="switchTab(event)" onclick="show('field', 'field-tab5', 2)" class="t\
                able-tab">Added in v1.0</button></div>
                <div id="field.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="field-tab0">
                <div class="table-header col-first">Field</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color field field-tab4"><a href="mdl/pkg/\
                TestClass.html#field">pkg.TestClass.field</a></div>
                <div class="col-last even-row-color field field-tab4">
                <div class="block">TestClass field.</div>
                </div>
                <div class="col-summary-item-name odd-row-color field field-tab5"><a href="mdl/pkg/T\
                estError.html#field">pkg.TestError.field</a></div>
                <div class="col-last odd-row-color field field-tab5">
                <div class="block">Test error field.</div>
                </div>
                <div class="col-summary-item-name even-row-color field field-tab2"><a href="mdl/pkg/\
                TestException.html#field">pkg.TestException.field</a></div>
                <div class="col-last even-row-color field field-tab2">
                <div class="block">Exception field.</div>
                </div>
                <div class="col-summary-item-name odd-row-color field field-tab3"><a href="mdl/pkg/T\
                estInterface.html#field">pkg.TestInterface.field</a></div>
                <div class="col-last odd-row-color field field-tab3">
                <div class="block">Test interface field.</div>
                </div>""",
            """
                <div id="method">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="met\
                hod-tab0" role="tab" aria-selected="true" aria-controls="method.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('method', 'method', 2)" class="active-\
                table-tab">New Methods</button><button id="method-tab1" role="tab" aria-selected="fa\
                lse" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncl\
                ick="show('method', 'method-tab1', 2)" class="table-tab">Added in 5</button><button \
                id="method-tab2" role="tab" aria-selected="false" aria-controls="method.tabpanel" ta\
                bindex="-1" onkeydown="switchTab(event)" onclick="show('method', 'method-tab2', 2)" \
                class="table-tab">Added in 3.2</button><button id="method-tab3" role="tab" aria-sele\
                cted="false" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(even\
                t)" onclick="show('method', 'method-tab3', 2)" class="table-tab">Added in 2.0b</butt\
                on><button id="method-tab4" role="tab" aria-selected="false" aria-controls="method.t\
                abpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show('method', 'method-\
                tab4', 2)" class="table-tab">Added in 1.2</button><button id="method-tab5" role="tab\
                " aria-selected="false" aria-controls="method.tabpanel" tabindex="-1" onkeydown="swi\
                tchTab(event)" onclick="show('method', 'method-tab5', 2)" class="table-tab">Added in\
                 v1.0</button></div>
                <div id="method.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="method-tab0">
                <div class="table-header col-first">Method</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color method method-tab2"><a href="mdl/pk\
                g/TestAnnotation.html#optional()">pkg.TestAnnotation.optional()</a></div>
                <div class="col-last even-row-color method method-tab2">
                <div class="block">Optional annotation interface element.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab3"><a href="mdl/pkg\
                /TestAnnotation.html#required()">pkg.TestAnnotation.required()</a></div>
                <div class="col-last odd-row-color method method-tab3">
                <div class="block">Required annotation interface element.</div>
                </div>
                <div class="col-summary-item-name even-row-color method method-tab3"><a href="mdl/pk\
                g/TestClass.html#method()">pkg.TestClass.method()</a></div>
                <div class="col-last even-row-color method method-tab3">
                <div class="block">TestClass method.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab1"><a href="mdl/pkg\
                /TestClass.html#overloadedMethod(java.lang.String)">pkg.TestClass.overloadedMethod<w\
                br>(String)</a></div>
                <div class="col-last odd-row-color method method-tab1">
                <div class="block">TestClass overloaded method.</div>
                </div>
                <div class="col-summary-item-name even-row-color method method-tab2"><a href="mdl/pk\
                g/TestError.html#method()">pkg.TestError.method()</a></div>
                <div class="col-last even-row-color method method-tab2">
                <div class="block">Test error method.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab4"><a href="mdl/pkg\
                /TestException.html#method()">pkg.TestException.method()</a></div>
                <div class="col-last odd-row-color method method-tab4">
                <div class="block">Exception method.</div>
                </div>
                <div class="col-summary-item-name even-row-color method method-tab5"><a href="mdl/pk\
                g/TestInterface.html#method1()">pkg.TestInterface.method1()</a></div>
                <div class="col-last even-row-color method method-tab5">
                <div class="block">Interface method.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab2"><a href="mdl/pkg\
                /TestInterface.html#method2(java.lang.Class)">pkg.TestInterface.method2<wbr>(Class&l\
                t;?&gt;)</a></div>
                <div class="col-last odd-row-color method method-tab2">
                <div class="block">Interface method.</div>
                </div>
                <div class="col-summary-item-name even-row-color method method-tab1"><a href="mdl/pk\
                g/TestRecord.html#x()">pkg.TestRecord.x()</a></div>
                <div class="col-last even-row-color method method-tab1">
                <div class="block">Test record getter.</div>
                </div>""",
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">New Constructors</button><button id="constructor-tab1\
                " role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabindex="-1\
                " onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab1', 2)" \
                class="table-tab">Added in 5</button><button id="constructor-tab2" role="tab" aria-s\
                elected="false" aria-controls="constructor.tabpanel" tabindex="-1" onkeydown="switch\
                Tab(event)" onclick="show('constructor', 'constructor-tab2', 2)" class="table-tab">A\
                dded in 3.2</button><button id="constructor-tab3" role="tab" aria-selected="false" a\
                ria-controls="constructor.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                ck="show('constructor', 'constructor-tab3', 2)" class="table-tab">Added in 2.0b</but\
                ton></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab3"><a hr\
                ef="mdl/pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                <div class="col-last even-row-color constructor constructor-tab3">
                <div class="block">TestClass constructor.</div>
                </div>
                <div class="col-summary-item-name odd-row-color constructor constructor-tab2"><a hre\
                f="mdl/pkg/TestClass.html#%3Cinit%3E(java.lang.String)">pkg.TestClass<wbr>(String)</\
                a></div>
                <div class="col-last odd-row-color constructor constructor-tab2">
                <div class="block">TestClass constructor.</div>
                </div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab1"><a hr\
                ef="mdl/pkg/TestError.html#%3Cinit%3E()">pkg.TestError()</a></div>
                <div class="col-last even-row-color constructor constructor-tab1">
                <div class="block">Test error constructor.</div>
                </div>
                <div class="col-summary-item-name odd-row-color constructor constructor-tab1"><a hre\
                f="mdl/pkg/TestException.html#%3Cinit%3E()">pkg.TestException()</a></div>
                <div class="col-last odd-row-color constructor constructor-tab1">
                <div class="block">Exception constructor.</div>
                </div>""",
            """
                <div id="enum-constant">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="enu\
                m-constant-tab0" role="tab" aria-selected="true" aria-controls="enum-constant.tabpan\
                el" tabindex="0" onkeydown="switchTab(event)" onclick="show('enum-constant', 'enum-c\
                onstant', 2)" class="active-table-tab">New Enum Constants</button><button id="enum-c\
                onstant-tab2" role="tab" aria-selected="false" aria-controls="enum-constant.tabpanel\
                " tabindex="-1" onkeydown="switchTab(event)" onclick="show('enum-constant', 'enum-co\
                nstant-tab2', 2)" class="table-tab">Added in 3.2</button><button id="enum-constant-t\
                ab4" role="tab" aria-selected="false" aria-controls="enum-constant.tabpanel" tabinde\
                x="-1" onkeydown="switchTab(event)" onclick="show('enum-constant', 'enum-constant-ta\
                b4', 2)" class="table-tab">Added in 1.2</button><button id="enum-constant-tab5" role\
                ="tab" aria-selected="false" aria-controls="enum-constant.tabpanel" tabindex="-1" on\
                keydown="switchTab(event)" onclick="show('enum-constant', 'enum-constant-tab5', 2)" \
                class="table-tab">Added in v1.0</button><button id="enum-constant-tab6" role="tab" a\
                ria-selected="false" aria-controls="enum-constant.tabpanel" tabindex="-1" onkeydown=\
                "switchTab(event)" onclick="show('enum-constant', 'enum-constant-tab6', 2)" class="t\
                able-tab">Added in 0.9</button></div>
                <div id="enum-constant.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="enum-constant-tab0">
                <div class="table-header col-first">Enum Constant</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color enum-constant enum-constant-tab2"><\
                a href="mdl/pkg/TestEnum.html#DEPRECATED">pkg.TestEnum.DEPRECATED</a></div>
                <div class="col-last even-row-color enum-constant enum-constant-tab2">
                <div class="block">Deprecated.</div>
                </div>
                <div class="col-summary-item-name odd-row-color enum-constant enum-constant-tab6"><a\
                 href="mdl/pkg/TestEnum.html#ONE">pkg.TestEnum.ONE</a></div>
                <div class="col-last odd-row-color enum-constant enum-constant-tab6">
                <div class="block">One.</div>
                </div>
                <div class="col-summary-item-name even-row-color enum-constant enum-constant-tab4"><\
                a href="mdl/pkg/TestEnum.html#THREE">pkg.TestEnum.THREE</a></div>
                <div class="col-last even-row-color enum-constant enum-constant-tab4">
                <div class="block">Three.</div>
                </div>
                <div class="col-summary-item-name odd-row-color enum-constant enum-constant-tab5"><a\
                 href="mdl/pkg/TestEnum.html#TWO">pkg.TestEnum.TWO</a></div>
                <div class="col-last odd-row-color enum-constant enum-constant-tab5">
                <div class="block">Two.</div>
                </div>""",
            """
                <div id="annotation-interface-member">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="ann\
                otation-interface-member-tab0" role="tab" aria-selected="true" aria-controls="annota\
                tion-interface-member.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="s\
                how('annotation-interface-member', 'annotation-interface-member', 2)" class="active-\
                table-tab">New Annotation Interface Elements</button><button id="annotation-interfac\
                e-member-tab2" role="tab" aria-selected="false" aria-controls="annotation-interface-\
                member.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show('annotatio\
                n-interface-member', 'annotation-interface-member-tab2', 2)" class="table-tab">Added\
                 in 3.2</button><button id="annotation-interface-member-tab3" role="tab" aria-select\
                ed="false" aria-controls="annotation-interface-member.tabpanel" tabindex="-1" onkeyd\
                own="switchTab(event)" onclick="show('annotation-interface-member', 'annotation-inte\
                rface-member-tab3', 2)" class="table-tab">Added in 2.0b</button></div>
                <div id="annotation-interface-member.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="annotation-interface-member-tab0">
                <div class="table-header col-first">Annotation Interface Element</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color annotation-interface-member annotat\
                ion-interface-member-tab2"><a href="mdl/pkg/TestAnnotation.html#optional()">pkg.Test\
                Annotation.optional()</a></div>
                <div class="col-last even-row-color annotation-interface-member annotation-interface-member-tab2">
                <div class="block">Optional annotation interface element.</div>
                </div>
                <div class="col-summary-item-name odd-row-color annotation-interface-member annotati\
                on-interface-member-tab3"><a href="mdl/pkg/TestAnnotation.html#required()">pkg.TestA\
                nnotation.required()</a></div>
                <div class="col-last odd-row-color annotation-interface-member annotation-interface-member-tab3">
                <div class="block">Required annotation interface element.</div>
                </div>""");
    }

    private void checkMultiReleaseDeprecatedElements() {
        checkOutput("deprecated-list.html", true,
            """
                <div id="for-removal">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="for\
                -removal-tab0" role="tab" aria-selected="true" aria-controls="for-removal.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('for-removal', 'for-removal'\
                , 2)" class="active-table-tab">Terminally Deprecated Elements</button><button id="fo\
                r-removal-tab1" role="tab" aria-selected="false" aria-controls="for-removal.tabpanel\
                " tabindex="-1" onkeydown="switchTab(event)" onclick="show('for-removal', 'for-remov\
                al-tab1', 2)" class="table-tab">Terminally Deprecated in 5</button></div>
                <div id="for-removal.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="for-removal-tab0">
                <div class="table-header col-first">Element</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color for-removal for-removal-tab1"><a hr\
                ef="mdl/pkg/TestAnnotation.html#required()">pkg.TestAnnotation.required()</a></div>
                <div class="col-last even-row-color for-removal for-removal-tab1"></div>
                </div>""",
            """
                <div id="method">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="met\
                hod-tab0" role="tab" aria-selected="true" aria-controls="method.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('method', 'method', 2)" class="active-\
                table-tab">Deprecated Methods</button><button id="method-tab1" role="tab" aria-selec\
                ted="false" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                )" onclick="show('method', 'method-tab1', 2)" class="table-tab">Deprecated in 5</but\
                ton></div>
                <div id="method.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="method-tab0">
                <div class="table-header col-first">Method</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color method method-tab1"><a href="mdl/pk\
                g/TestAnnotation.html#required()">pkg.TestAnnotation.required()</a></div>
                <div class="col-last even-row-color method method-tab1"></div>
                </div>""",
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">Deprecated Constructors</button><button id="construct\
                or-tab1" role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabin\
                dex="-1" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab1\
                ', 2)" class="table-tab">Deprecated in 5</button></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab1"><a hr\
                ef="mdl/pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                <div class="col-last even-row-color constructor constructor-tab1"></div>
                </div>""",
            """
                <div id="enum-constant">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="enu\
                m-constant-tab0" role="tab" aria-selected="true" aria-controls="enum-constant.tabpan\
                el" tabindex="0" onkeydown="switchTab(event)" onclick="show('enum-constant', 'enum-c\
                onstant', 2)" class="active-table-tab">Deprecated Enum Constants</button><button id=\
                "enum-constant-tab1" role="tab" aria-selected="false" aria-controls="enum-constant.t\
                abpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show('enum-constant', '\
                enum-constant-tab1', 2)" class="table-tab">Deprecated in 5</button></div>
                <div id="enum-constant.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="enum-constant-tab0">
                <div class="table-header col-first">Enum Constant</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color enum-constant enum-constant-tab1"><\
                a href="mdl/pkg/TestEnum.html#DEPRECATED">pkg.TestEnum.DEPRECATED</a></div>
                <div class="col-last even-row-color enum-constant enum-constant-tab1"></div>
                </div>""",
            """
                <div id="annotation-interface-member">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="ann\
                otation-interface-member-tab0" role="tab" aria-selected="true" aria-controls="annota\
                tion-interface-member.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="s\
                how('annotation-interface-member', 'annotation-interface-member', 2)" class="active-\
                table-tab">Deprecated Annotation Interface Elements</button><button id="annotation-i\
                nterface-member-tab1" role="tab" aria-selected="false" aria-controls="annotation-int\
                erface-member.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="show('an\
                notation-interface-member', 'annotation-interface-member-tab1', 2)" class="table-tab\
                ">Deprecated in 5</button></div>
                <div id="annotation-interface-member.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="annotation-interface-member-tab0">
                <div class="table-header col-first">Annotation Interface Element</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color annotation-interface-member annotat\
                ion-interface-member-tab1"><a href="mdl/pkg/TestAnnotation.html#required()">pkg.Test\
                Annotation.required()</a></div>
                <div class="col-last even-row-color annotation-interface-member annotation-interface-member-tab1"></div>
                </div>""");
    }

    private void checkSingleReleaseContents() {
        checkOutput("new-list.html", true,
            """
                <h1 title="New API" class="title">New API</h1>
                <h2 title="Contents">Contents</h2>
                <ul>
                <li><a href="#method">Methods</a></li>
                <li><a href="#constructor">Constructors</a></li>
                </ul>
                </div>
                <ul class="block-list">""");
    }

    private void checkSingleReleaseNewElements() {
        checkOutput("new-list.html", true,
            """
                <div id="method">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="met\
                hod-tab0" role="tab" aria-selected="true" aria-controls="method.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('method', 'method', 2)" class="active-\
                table-tab">New Methods</button><button id="method-tab1" role="tab" aria-selected="fa\
                lse" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncl\
                ick="show('method', 'method-tab1', 2)" class="table-tab">New Methods</button></div>
                <div id="method.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="method-tab0">
                <div class="table-header col-first">Method</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color method method-tab1"><a href="mdl/pk\
                g/TestClass.html#overloadedMethod(java.lang.String)">pkg.TestClass.overloadedMethod<\
                wbr>(String)</a></div>
                <div class="col-last even-row-color method method-tab1">
                <div class="block">TestClass overloaded method.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab1"><a href="mdl/pkg\
                /TestRecord.html#x()">pkg.TestRecord.x()</a></div>
                <div class="col-last odd-row-color method method-tab1">
                <div class="block">Test record getter.</div>
                </div>""",
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">New Constructors</button><button id="constructor-tab1\
                " role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabindex="-1\
                " onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab1', 2)" \
                class="table-tab">New Constructors</button></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab1"><a hr\
                ef="mdl/pkg/TestError.html#%3Cinit%3E()">pkg.TestError()</a></div>
                <div class="col-last even-row-color constructor constructor-tab1">
                <div class="block">Test error constructor.</div>
                </div>
                <div class="col-summary-item-name odd-row-color constructor constructor-tab1"><a hre\
                f="mdl/pkg/TestException.html#%3Cinit%3E()">pkg.TestException()</a></div>
                <div class="col-last odd-row-color constructor constructor-tab1">
                <div class="block">Exception constructor.</div>
                </div>""");
    }

    private void checkSingleReleaseDeprecatedElements() {
        checkOutput("deprecated-list.html", true,
            """
                <h1 title="Deprecated API" class="title">Deprecated API</h1>
                <h2 title="Contents">Contents</h2>
                <ul>
                <li><a href="#for-removal">Terminally Deprecated</a></li>
                <li><a href="#method">Methods</a></li>
                <li><a href="#constructor">Constructors</a></li>
                <li><a href="#enum-constant">Enum Constants</a></li>
                <li><a href="#annotation-interface-member">Annotation Interface Elements</a></li>
                </ul>
                </div>
                <ul class="block-list">""",
            """
                <div id="for-removal">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="for\
                -removal-tab0" role="tab" aria-selected="true" aria-controls="for-removal.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('for-removal', 'for-removal'\
                , 2)" class="active-table-tab">Terminally Deprecated Elements</button><button id="fo\
                r-removal-tab1" role="tab" aria-selected="false" aria-controls="for-removal.tabpanel\
                " tabindex="-1" onkeydown="switchTab(event)" onclick="show('for-removal', 'for-remov\
                al-tab1', 2)" class="table-tab">Terminally Deprecated in 5</button></div>
                <div id="for-removal.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="for-removal-tab0">
                <div class="table-header col-first">Element</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color for-removal for-removal-tab1"><a hr\
                ef="mdl/pkg/TestAnnotation.html#required()">pkg.TestAnnotation.required()</a></div>
                <div class="col-last even-row-color for-removal for-removal-tab1"></div>
                </div>""",
            """
                <div id="method">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="met\
                hod-tab0" role="tab" aria-selected="true" aria-controls="method.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('method', 'method', 2)" class="active-\
                table-tab">Deprecated Methods</button><button id="method-tab1" role="tab" aria-selec\
                ted="false" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                )" onclick="show('method', 'method-tab1', 2)" class="table-tab">Deprecated in 5</but\
                ton></div>
                <div id="method.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="method-tab0">
                <div class="table-header col-first">Method</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color method method-tab1"><a href="mdl/pk\
                g/TestAnnotation.html#required()">pkg.TestAnnotation.required()</a></div>
                <div class="col-last even-row-color method method-tab1"></div>
                </div>""",
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">Deprecated Constructors</button><button id="construct\
                or-tab1" role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabin\
                dex="-1" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab1\
                ', 2)" class="table-tab">Deprecated in 5</button></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab1"><a hr\
                ef="mdl/pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                <div class="col-last even-row-color constructor constructor-tab1"></div>
                </div>""");
    }

    private void checkPackageContents() {
        checkOutput("new-list.html", true,
            """
                <h1 title="New API" class="title">New API</h1>
                <h2 title="Contents">Contents</h2>
                <ul>
                <li><a href="#class">Classes</a></li>
                <li><a href="#field">Fields</a></li>
                <li><a href="#method">Methods</a></li>
                <li><a href="#constructor">Constructors</a></li>
                </ul>
                </div>
                <span class="help-note">(The leftmost tab "New ..." indicates all the new elements, \
                regardless of the releases in which they were added. Each of the other tabs "Added i\
                n ..." indicates the new elements added in a specific release. Any element shown und\
                er the leftmost tab is also shown under one of the righthand tabs.)</span>""");
    }

    private void checkPackageNewElements() {
        checkOutput("new-list.html", true,
            """
                <div id="class">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="cla\
                ss-tab0" role="tab" aria-selected="true" aria-controls="class.tabpanel" tabindex="0"\
                 onkeydown="switchTab(event)" onclick="show('class', 'class', 2)" class="active-tabl\
                e-tab">New Classes</button><button id="class-tab5" role="tab" aria-selected="false" \
                aria-controls="class.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="s\
                how('class', 'class-tab5', 2)" class="table-tab">Added in 1.2</button></div>
                <div id="class.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="class-tab0">
                <div class="table-header col-first">Class</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color class class-tab5"><a href="pkg/Test\
                Class.html" title="class in pkg">pkg.TestClass</a></div>
                <div class="col-last even-row-color class class-tab5">
                <div class="block">TestClass declaration.</div>
                </div>""",
            """
                <div id="field">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="fie\
                ld-tab0" role="tab" aria-selected="true" aria-controls="field.tabpanel" tabindex="0"\
                 onkeydown="switchTab(event)" onclick="show('field', 'field', 2)" class="active-tabl\
                e-tab">New Fields</button><button id="field-tab5" role="tab" aria-selected="false" a\
                ria-controls="field.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onclick="sh\
                ow('field', 'field-tab5', 2)" class="table-tab">Added in 1.2</button></div>
                <div id="field.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="field-tab0">
                <div class="table-header col-first">Field</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color field field-tab5"><a href="pkg/Test\
                Class.html#field">pkg.TestClass.field</a></div>
                <div class="col-last even-row-color field field-tab5">
                <div class="block">TestClass field.</div>
                </div>""",
            """
                <div id="method">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="met\
                hod-tab0" role="tab" aria-selected="true" aria-controls="method.tabpanel" tabindex="\
                0" onkeydown="switchTab(event)" onclick="show('method', 'method', 2)" class="active-\
                table-tab">New Methods</button><button id="method-tab1" role="tab" aria-selected="fa\
                lse" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncl\
                ick="show('method', 'method-tab1', 2)" class="table-tab">Added in 6</button><button \
                id="method-tab2" role="tab" aria-selected="false" aria-controls="method.tabpanel" ta\
                bindex="-1" onkeydown="switchTab(event)" onclick="show('method', 'method-tab2', 2)" \
                class="table-tab">Added in 5</button><button id="method-tab4" role="tab" aria-select\
                ed="false" aria-controls="method.tabpanel" tabindex="-1" onkeydown="switchTab(event)\
                " onclick="show('method', 'method-tab4', 2)" class="table-tab">Added in 2.0b</button\
                ></div>
                <div id="method.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="method-tab0">
                <div class="table-header col-first">Method</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color method method-tab4"><a href="pkg/Te\
                stClass.html#method()">pkg.TestClass.method()</a></div>
                <div class="col-last even-row-color method method-tab4">
                <div class="block">TestClass method.</div>
                </div>
                <div class="col-summary-item-name odd-row-color method method-tab1"><a href="pkg/Tes\
                tClass.html#overloadedMethod(int)">pkg.TestClass.overloadedMethod<wbr>(int)</a></div>
                <div class="col-last odd-row-color method method-tab1">
                <div class="block">TestClass overloaded method.</div>
                </div>
                <div class="col-summary-item-name even-row-color method method-tab2"><a href="pkg/Te\
                stClass.html#overloadedMethod(java.lang.String)">pkg.TestClass.overloadedMethod<wbr>\
                (String)</a></div>
                <div class="col-last even-row-color method method-tab2">
                <div class="block">TestClass overloaded method.</div>
                </div>""",
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">New Constructors</button><button id="constructor-tab3\
                " role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabindex="-1\
                " onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab3', 2)" \
                class="table-tab">Added in 3.2</button><button id="constructor-tab4" role="tab" aria\
                -selected="false" aria-controls="constructor.tabpanel" tabindex="-1" onkeydown="swit\
                chTab(event)" onclick="show('constructor', 'constructor-tab4', 2)" class="table-tab"\
                >Added in 2.0b</button></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab4"><a hr\
                ef="pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                <div class="col-last even-row-color constructor constructor-tab4">
                <div class="block">TestClass constructor.</div>
                </div>
                <div class="col-summary-item-name odd-row-color constructor constructor-tab3"><a hre\
                f="pkg/TestClass.html#%3Cinit%3E(java.lang.String)">pkg.TestClass<wbr>(String)</a></div>
                <div class="col-last odd-row-color constructor constructor-tab3">
                <div class="block">TestClass constructor.</div>
                </div>""");
    }

    private void checkPackageDeprecatedElements() {
        checkOutput("deprecated-list.html", true,
            """
                <h1 title="Deprecated API" class="title">Deprecated API</h1>
                <h2 title="Contents">Contents</h2>
                <ul>
                <li><a href="#constructor">Constructors</a></li>
                </ul>
                </div>
                <span class="help-note">(The leftmost tab "Deprecated ..." indicates all the depreca\
                ted elements, regardless of the releases in which they were deprecated. Each of the \
                other tabs "Deprecated in ..." indicates the elements deprecated in a specific relea\
                se.)</span>
                """,
            """
                <div id="constructor">
                <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id="con\
                structor-tab0" role="tab" aria-selected="true" aria-controls="constructor.tabpanel" \
                tabindex="0" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor'\
                , 2)" class="active-table-tab">Deprecated Constructors</button><button id="construct\
                or-tab2" role="tab" aria-selected="false" aria-controls="constructor.tabpanel" tabin\
                dex="-1" onkeydown="switchTab(event)" onclick="show('constructor', 'constructor-tab2\
                ', 2)" class="table-tab">Deprecated in 5</button></div>
                <div id="constructor.tabpanel" role="tabpanel">
                <div class="summary-table two-column-summary" aria-labelledby="constructor-tab0">
                <div class="table-header col-first">Constructor</div>
                <div class="table-header col-last">Description</div>
                <div class="col-summary-item-name even-row-color constructor constructor-tab2"><a hr\
                ef="pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                <div class="col-last even-row-color constructor constructor-tab2"></div>
                </div>""");
    }
}
