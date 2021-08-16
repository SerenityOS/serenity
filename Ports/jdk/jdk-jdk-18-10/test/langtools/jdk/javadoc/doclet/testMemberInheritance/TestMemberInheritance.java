/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4638588 4635809 6256068 6270645 8025633 8026567 8162363 8175200
 *      8192850 8182765 8220217 8224052 8237383
 * @summary Test to make sure that members are inherited properly in the Javadoc.
 *          Verify that inheritance labels are correct.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestMemberInheritance
 */

import javadoc.tester.JavadocTester;

public class TestMemberInheritance extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMemberInheritance tester = new TestMemberInheritance();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg", "diamond", "inheritDist", "pkg1", "pkg2", "pkg3");
        checkExit(Exit.OK);

        checkOutput("pkg/SubClass.html", true,
                // Public field should be inherited
                """
                    <a href="BaseClass.html#pubField">""",
                // Public method should be inherited
                """
                    <a href="BaseClass.html#pubMethod()">""",
                // Public inner class should be inherited.
                """
                    <a href="BaseClass.pubInnerClass.html" title="class in pkg">""",
                // Protected field should be inherited
                """
                    <a href="BaseClass.html#proField">""",
                // Protected method should be inherited
                """
                    <a href="BaseClass.html#proMethod()">""",
                // Protected inner class should be inherited.
                """
                    <a href="BaseClass.proInnerClass.html" title="class in pkg">""",
                // New labels as of 1.5.0
                """
                    Nested classes/interfaces inherited from class&nbsp;pkg.<a href="BaseClass.html" title="class in pkg">BaseClass</a>""",
                """
                    Nested classes/interfaces inherited from interface&nbsp;pkg.<a href="BaseInterfa\
                    ce.html" title="interface in pkg">BaseInterface</a>""");

        checkOutput("pkg/BaseClass.html", true,
                // Test overriding/implementing methods with generic parameters.
                """
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="BaseInterface.html#getAnnotation(java.lang.Class)">getAnnotat\
                    ion</a></code>&nbsp;in interface&nbsp;<code><a href="BaseInterface.html" title="\
                    interface in pkg">BaseInterface</a></code></dd>
                    </dl>""");

        checkOutput("diamond/Z.html", true,
                // Test diamond inheritance member summary (6256068)
                """
                    <code><a href="A.html#aMethod()">aMethod</a></code>""");

        checkOutput("inheritDist/C.html", true,
                // Test that doc is inherited from closed parent (6270645)
                "<div class=\"block\">m1-B</div>");

        checkOutput("pkg/SubClass.html", false,
                """
                    <a href="BaseClass.html#staticMethod()">staticMethod</a></code>""");

        checkOutput("pkg1/Implementer.html", true,
                // ensure the method makes it
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab1 method-summary-table-tab4"><code>static java.time.Period</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab1 method-summary-table-tab4"><code><a href="#between(java.time.LocalDate,java\
                    .time.LocalDate)" class="member-name-link">between</a><wbr>(java.time.LocalDate&\
                    nbsp;startDateInclusive,
                     java.time.LocalDate&nbsp;endDateExclusive)</code></div>""");

        checkOutput("pkg1/Implementer.html", false,
                """
                    <h3>Methods inherited from interface&nbsp;pkg1.<a href="Interface.html" title="interface in pkg1">Interface</a></h3>
                    <code><a href="Interface.html#between(java.time.chrono.ChronoLocalDate,java.time\
                    .chrono.ChronoLocalDate)">between</a></code>"""
        );

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <section class="class-description" id="class-description">
                    <hr>
                    <div class="type-signature"><span class="modifiers">public abstract class </span\
                    ><span class="element-name type-name-label">DocumentedNonGenericChild</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    </section>""");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-tab2 m\
                    ethod-summary-table-tab3"><code>protected abstract java.lang.String</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-tab2 \
                    method-summary-table-tab3"><code><a href="#parentMethod(T)" class="member-name-link">\
                    parentMethod</a><wbr>(java.lang.String&nbsp;t)</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-tab2 me\
                    thod-summary-table-tab3">
                    <div class="block">Returns some value with an inherited search tag.</div>
                    """);

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <section class="detail" id="parentMethod(T)">
                    <h3 id="parentMethod(java.lang.Object)">parentMethod</h3>
                    <div class="member-signature"><span class="modifiers">protected abstract</span>&\
                    nbsp;<span class="return-type">java.lang.String</span>&nbsp;<span class="element\
                    -name">parentMethod</span><wbr><span class="parameters">(java.lang.String&nbsp;t\
                    )</span>
                                                              throws <span class="exceptions">java.lang.IllegalArgumentException,
                    java.lang.InterruptedException,
                    java.lang.IllegalStateException</span></div>
                    <div class="block">Returns some value with an <span id="inheritedsearchtag" clas\
                    s="search-tag-result">inherited search tag</span>.</div>""");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <dt>Throws:</dt>
                    <dd><code>java.lang.InterruptedException</code> - a generic error</dd>
                    <dd><code>java.lang.IllegalStateException</code> - illegal state</dd>
                    <dd><code>java.lang.IllegalArgumentException</code></dd>""");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <div class="col-first even-row-color"><code>java.lang.String</code></div>
                    <div class="col-second even-row-color"><code><a href="#parentField" class="membe\
                    r-name-link">parentField</a></code></div>
                    <div class="col-last even-row-color">
                    <div class="block">A field.</div>""",
                """
                    <section class="detail" id="parentField">
                    <h3>parentField</h3>
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">java.lang.String</span>&nbsp;<span class="element-name">parentField</\
                    span></div>
                    <div class="block">A field.</div>
                    </section>""");

        checkOutput("pkg3/PrivateGenericParent.PublicChild.html", true,
                """
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>java.lang.String</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#method(T)" class="member-name-li\
                    nk">method</a><wbr>(java.lang.String&nbsp;t)</code></div>""",
                """
                    <section class="detail" id="method(T)">
                    <h3 id="method(java.lang.Object)">method</h3>
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">java.lang.String</span>&nbsp;<span class="element-name">metho\
                    d</span><wbr><span class="parameters">(java.lang.String&nbsp;t)</span></div>
                    </section>""");

        checkOutput("index-all.html", true,
                """
                    <dt><a href="pkg2/DocumentedNonGenericChild.html#parentField" class="member-name\
                    -link">parentField</a> - Variable in class pkg2.<a href="pkg2/DocumentedNonGener\
                    icChild.html" title="class in pkg2">DocumentedNonGenericChild</a></dt>
                    <dd>
                    <div class="block">A field.</div>
                    </dd>
                    """,
                """
                    <dt><a href="pkg2/DocumentedNonGenericChild.html#parentMethod(T)" class="member-\
                    name-link">parentMethod(String)</a> - Method in class pkg2.<a href="pkg2/Documen\
                    tedNonGenericChild.html" title="class in pkg2">DocumentedNonGenericChild</a></dt>
                    <dd>
                    <div class="block">Returns some value with an inherited search tag.</div>
                    </dd>""");
        checkOutput("member-search-index.js", true,
                """
                    {"p":"pkg2","c":"DocumentedNonGenericChild","l":"parentField"}""",
                """
                    {"p":"pkg2","c":"DocumentedNonGenericChild","l":"parentMethod(String)","u":"parentMethod(T)"}""");
        checkOutput("tag-search-index.js", true,
                """
                    {"l":"inherited search tag","h":"pkg2.UndocumentedGenericParent.parentMethod(Str\
                    ing)","u":"pkg2/DocumentedNonGenericChild.html#inheritedsearchtag"}""");

    }

    @Test
    public void testSplitIndex() {
        javadoc("-d", "out-split",
                "-splitindex",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg", "diamond", "inheritDist", "pkg1", "pkg2", "pkg3");
        checkExit(Exit.OK);

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                """
                    <section class="detail" id="parentMethod(T)">
                    <h3 id="parentMethod(java.lang.Object)">parentMethod</h3>
                    <div class="member-signature"><span class="modifiers">protected abstract</span>&\
                    nbsp;<span class="return-type">java.lang.String</span>&nbsp;<span class="element\
                    -name">parentMethod</span><wbr><span class="parameters">(java.lang.String&nbsp;t\
                    )</span>
                                                              throws <span class="exceptions">java.lang.IllegalArgumentException,
                    java.lang.InterruptedException,
                    java.lang.IllegalStateException</span></div>
                    <div class="block">Returns some value with an <span id="inheritedsearchtag" clas\
                    s="search-tag-result">inherited search tag</span>.</div>""");

        checkOutput("index-files/index-9.html", true,
                """
                    <dt><a href="../pkg2/DocumentedNonGenericChild.html#parentField" class="member-n\
                    ame-link">parentField</a> - Variable in class pkg2.<a href="../pkg2/DocumentedNo\
                    nGenericChild.html" title="class in pkg2">DocumentedNonGenericChild</a></dt>
                    <dd>
                    <div class="block">A field.</div>
                    </dd>
                    """,
                """
                    <dt><a href="../pkg2/DocumentedNonGenericChild.html#parentMethod(T)" class="memb\
                    er-name-link">parentMethod(String)</a> - Method in class pkg2.<a href="../pkg2/D\
                    ocumentedNonGenericChild.html" title="class in pkg2">DocumentedNonGenericChild</a></dt>
                    <dd>
                    <div class="block">Returns some value with an inherited search tag.</div>
                    </dd>""");
        checkOutput("member-search-index.js", true,
                """
                    {"p":"pkg2","c":"DocumentedNonGenericChild","l":"parentField"}""",
                """
                    {"p":"pkg2","c":"DocumentedNonGenericChild","l":"parentMethod(String)","u":"parentMethod(T)"}""");
        checkOutput("tag-search-index.js", true,
                """
                    {"l":"inherited search tag","h":"pkg2.UndocumentedGenericParent.parentMethod(Str\
                    ing)","u":"pkg2/DocumentedNonGenericChild.html#inheritedsearchtag"}""");
    }

}
