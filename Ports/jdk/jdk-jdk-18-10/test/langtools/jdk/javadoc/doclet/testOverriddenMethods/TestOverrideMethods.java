/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8157000 8192850 8182765 8223607 8261976
 * @summary  test the behavior of --override-methods option
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestOverrideMethods
 */

import javadoc.tester.JavadocTester;

public class TestOverrideMethods  extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestOverrideMethods tester = new TestOverrideMethods();
        tester.runTests();
    }

    @Test
    public void testInvalidOption() {
        // Make sure an invalid argument fails
        javadoc("-d", "out-bad-option",
                "-sourcepath", testSrc,
                "-javafx",
                "--disable-javafx-strict-checks",
                "--override-methods=nonsense",
                "pkg5");

        checkExit(Exit.CMDERR);
    }

    @Test
    public void testDetail() {
        // Make sure the option works
        javadoc("-d", "out-detail",
                "-sourcepath", testSrc,
                "-javafx",
                "--disable-javafx-strict-checks",
                "--override-methods=detail",
                "pkg5");

        checkExit(Exit.OK);
    }

    @Test
    public void testSummary() {
        javadoc("-d", "out-summary",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-javafx",
                "--disable-javafx-strict-checks",
                "--override-methods=summary",
                "pkg5", "pkg6");

        checkExit(Exit.OK);

        checkOrder("pkg5/Classes.C.html",
                // Check properties
                """
                    Properties declared in class&nbsp;pkg5.<a href="Classes.P.html""",
                "Classes.P",
                """
                    Classes.P.html#rateProperty">rate""",

                // Check nested classes
                "Nested classes/interfaces declared in class&nbsp;pkg5.",
                "Classes.P",
                "Classes.P.PN.html",
                "Classes.P.PN.html",
                """
                    type parameter in Classes.P.PN">K""",
                "type parameter in Classes.P.PN",
                "V",

                // Check fields
                """
                    Fields declared in class&nbsp;pkg5.<a href="Classes.P.html""",
                "Classes.P",
                "Classes.P.html#field0\">field0",

                // Check method summary
                "Method Summary",
                "void",
                "#m1()\" class=\"member-name-link\">m1",
                "A modified method",

                "void",
                """
                    #m4(java.lang.String,java.lang.String)" class="member-name-link">m4""",
                "java.lang.String&nbsp;k,",
                "java.lang.String",
                "&nbsp;v)",

                // Check footnotes
                """
                    Methods declared in class&nbsp;pkg5.<a href="Classes.GP.html""",
                "Classes.GP",
                "Classes.GP.html#m0()\">m0",

                // Check method details for override
                """
                    <dl class="notes">
                    <dt>Overrides:""",
                "Classes.GP.html#m7()\">m7",
                "in class",
                "Classes.GP.html",
                "Classes.GP"
        );

        checkOrder("pkg5/Classes.C.html",
                // Check footnotes 2
                "Methods declared in class&nbsp;pkg5.",
                """
                    Classes.P.html#getRate()">getRate""",
                "Classes.P.html#m2()\">m2",
                "Classes.P.html#m3()\">m3",
                "Classes.P.html#m4(K,V)\">m4",
                """
                    Classes.P.html#rateProperty()">rateProperty""",
                """
                    Classes.P.html#setRate(double)">setRate""",

                // Check @link
                """
                    A test of links to the methods in this class. <p>
                    """,
                "Classes.GP.html#m0()",
                "Classes.GP.m0()",
                "#m1()",
                "m1()",
                "Classes.P.html#m2()",
                "Classes.P.m2()",
                "Classes.P.html#m3()",
                "Classes.P.m3()",
                "m4(java.lang.String,java.lang.String)",
                "Classes.P.html#m5()",
                "Classes.P.m5()",
                "#m6()",
                "m6()",
                "#m7()",
                "m7()",
                "End of links",

                // Check @see
                "See Also:",
                "Classes.GP.html#m0()",
                "Classes.GP.m0()",
                "#m1()",
                "m1()",
                "Classes.P.html#m2()",
                "Classes.P.m2()",
                "Classes.P.html#m3()",
                "Classes.P.m3()",
                "#m4(java.lang.String,java.lang.String)",
                "m4(String k, String v)",
                """
                    Classes.P.html#m5()"><code>Classes.P.m5()""",
                "#m6()\"><code>m6()",
                "#m7()\"><code>m7()"
        );

        // Tests for interfaces

        // Make sure the static methods in the super interface
        // do not make it to this interface
        checkOutput("pkg5/Interfaces.D.html", false,
                "msd", "msn");

        checkOrder("pkg5/Interfaces.D.html",
                "Start of links <p>",
                """
                    Interfaces.A.html#m0()"><code>Interfaces.A.m0()""",
                """
                    Interfaces.A.html#m1()"><code>Interfaces.A.m1()""",
                """
                    Interfaces.A.html#m2()"><code>Interfaces.A.m2()""",
                """
                    Interfaces.A.html#m3()"><code>Interfaces.A.m3()""",
                "#m()\"><code>m()",
                "#n()\"><code>n()",
                """
                    Interfaces.C.html#o()"><code>Interfaces.C.o()""",
                "End of links",

                // Check @see links
                "See Also:",
                """
                    Interfaces.A.html#m0()"><code>Interfaces.A.m0()""",
                """
                    Interfaces.A.html#m1()"><code>Interfaces.A.m1()""",
                """
                    Interfaces.A.html#m2()"><code>Interfaces.A.m2()""",
                """
                    Interfaces.A.html#m3()"><code>Interfaces.A.m3()""",
                "#m()\"><code>m()",
                "#n()\"><code>n()",
                """
                    Interfaces.C.html#o()"><code>Interfaces.C.o()""",

                // Check properties
                """
                    Properties declared in interface&nbsp;pkg5.<a href="Interfaces.A.html" title="interface in pkg5">Interfaces.A</a>""",

                // Check nested classes
                "Nested classes/interfaces declared in interface&nbsp;pkg5.",
                "Interfaces.A",
                "Interfaces.A.AA.html",
                "Interfaces.A.AA",

                // Check Fields
                """
                    Fields declared in interface&nbsp;pkg5.<a href="Interfaces.A.html""",
                "Interfaces.A.html#f",
                "Interfaces.A.html#QUOTE\">QUOTE",
                "Interfaces.A.html#rate\">rate",

                // Check Method Summary
                "Method Summary",
                "#m()\" class=\"member-name-link\">m",
                "#n()\" class=\"member-name-link\">n",

                // Check footnotes
                """
                    Methods declared in interface&nbsp;pkg5.<a href="Interfaces.A.html""",
                """
                    Interfaces.A.html#getRate()">getRate""",
                """
                    Interfaces.A.html#rateProperty()">rateProperty""",
                "Interfaces.A.html#setRate(double)",
                """
                    Methods declared in interface&nbsp;pkg5.<a href="Interfaces.B.html""",
                "Interfaces.B.html#m1()\">m1",
                "Interfaces.B.html#m3()\">m3",
                """
                    Methods declared in interface&nbsp;pkg5.<a href="Interfaces.C.html""",
                """
                    <a href="Interfaces.C.html#o()">o</a>"""
        );

        // Test synthetic values and valuesof of an enum.
        checkOrder("index-all.html",
                """
                    <h2 class="title" id="I:M">M</h2>""",
                """
                    <a href="pkg5/Interfaces.C.html#m()" class="member-name-link">m()""",
                """
                    <a href="pkg5/Interfaces.D.html#m()" class="member-name-link">m()</a>""",
                """
                    <a href="pkg5/Classes.GP.html#m0()" class="member-name-link">m0()""",
                """
                    <a href="pkg5/Interfaces.A.html#m0()" class="member-name-link">m0()</a>""",
                """
                    <a href="pkg5/Classes.C.html#m1()" class="member-name-link">m1()</a>""",
                """
                    <a href="pkg5/Classes.P.html#m1()" class="member-name-link">m1()</a>""",
                """
                    <a href="pkg5/Interfaces.A.html#m1()" class="member-name-link">m1()</a>""",
                """
                    <a href="pkg5/Interfaces.B.html#m1()" class="member-name-link">m1()</a>""",
                """
                    <a href="pkg5/Classes.P.html#m2()" class="member-name-link">m2()</a>""",
                """
                    <a href="pkg5/Interfaces.A.html#m2()" class="member-name-link">m2()</a>""",
                """
                    <a href="pkg5/Classes.P.html#m3()" class="member-name-link">m3()</a>""",
                """
                    <a href="pkg5/Interfaces.A.html#m3()" class="member-name-link">m3()</a>""",
                """
                    <a href="pkg5/Interfaces.B.html#m3()" class="member-name-link">m3()</a>""",
                """
                    <a href="pkg5/Classes.C.html#m4(java.lang.String,java.lang.String)" class="membe\
                    r-name-link">m4(String, String)</a>""",
                """
                    <a href="pkg5/Classes.P.html#m4(K,V)" class="member-name-link">m4(K, V)</a>""",
                """
                    <a href="pkg5/Classes.P.html#m5()" class="member-name-link">m5()</a>""",
                """
                    <a href="pkg5/Classes.C.html#m6()" class="member-name-link">m6()</a>""",
                """
                    <a href="pkg5/Classes.P.html#m6()" class="member-name-link">m6()</a>""",
                """
                    <a href="pkg5/Classes.C.html#m7()" class="member-name-link">m7()</a>""",
                """
                    <a href="pkg5/Classes.GP.html#m7()" class="member-name-link">m7()</a>""",
                "Returns the enum constant of this class with the specified name.",
                """
                    Returns an array containing the constants of this enum class, in
                    the order they are declared."""
        );

        // Check methods with covariant return types, changes in modifiers or thrown exceptions.
        // Only those should be shown in summary; m1, m3, m9 should listed as declared in Base
        checkOutput("pkg6/Sub.html", true,
                """
                    <div class="summary-table three-column-summary" aria-labelledby="method-summary-table-tab0">
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>java.lang.String</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#m2()" class="member-name-link">\
                    m2</a>()</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">
                    <div class="block">This is Base::m2.</div>
                    </div>
                    <div class="col-first odd-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"><code>void</code></div>
                    <div class="col-second odd-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="#m4()" class="member-name-link">m4\
                    </a>()</code></div>
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab4">
                    <div class="block">This is Base::m4.</div>
                    </div>
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>java.lang.Object</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#m5()" class="member-name-link">m\
                    5</a>()</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">
                    <div class="block">This is Base::m5.</div>
                    </div>
                    <div class="col-first odd-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"><code>final java.lang.Object</code></div>
                    <div class="col-second odd-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="#m6()" class="member-name-link">m6\
                    </a>()</code></div>
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab4">
                    <div class="block">This is Base::m6.</div>
                    </div>
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>java.lang.Object</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#m7()" class="member-name-link">m\
                    7</a>()</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">
                    <div class="block">This is Base::m7.</div>
                    </div>
                    <div class="col-first odd-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab3"><code>abstract java.lang.Object</code></div>
                    <div class="col-second odd-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab3"><code><a href="#m8()" class="member-name-link">m8\
                    </a>()</code></div>
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab3">
                    <div class="block">This is Base::m8.</div>
                    </div>
                    """,
                """
                    <div class="inherited-list">
                    <h3 id="methods-inherited-from-class-pkg6.Base">Methods declared in class&nbsp;p\
                    kg6.<a href="Base.html" title="class in pkg6">Base</a></h3>
                    <code><a href="Base.html#m1()">m1</a>, <a href="Base.html#m3()">m3</a>, <a href="Base.html#m9()">m9</a></code></div>
                    """);
    }

    @Test
    public void testSummaryAnnotations() {
        javadoc("-d", "out-summary-annotations",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-javafx",
                "--disable-javafx-strict-checks",
                "--override-methods=summary",
                "-private",
                "pkg7");

        checkExit(Exit.OK);

        checkOutput("pkg7/AnnotatedSub1.html", true,
                """
                    <div class="inherited-list">
                    <h3 id="methods-inherited-from-class-pkg7.AnnotatedBase">Methods declared in int\
                    erface&nbsp;pkg7.<a href="AnnotatedBase.html" title="interface in pkg7">Annotate\
                    dBase</a></h3>
                    <code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></code></div>""");

        checkOutput("pkg7/AnnotatedSub2.html", true,
                """
                    <div class="member-signature"><span class="annotations"><a href="A.html" title="\
                    annotation interface in pkg7">@A</a>
                    </span><span class="return-type"><a href="A.html" title="annotation interface in\
                     pkg7">@A</a> java.lang.Iterable&lt;java.lang.String&gt;</span>&nbsp;<span class\
                    ="element-name">m1</span><wbr><span class="parameters">(java.lang.Class&lt;? ext\
                    ends java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub3.html", true,
                """
                    <div class="member-signature"><span class="annotations"><a href="A.html" title="\
                    annotation interface in pkg7">@A</a>
                    </span><span class="return-type"><a href="A.html" title="annotation interface in\
                     pkg7">@A</a> java.lang.Iterable&lt;java.lang.String&gt;</span>&nbsp;<span class\
                    ="element-name">m1</span><wbr><span class="parameters">(java.lang.Class&lt;? ext\
                    ends java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub4.html", true,
                """
                    <div class="member-signature"><span class="return-type">java.lang.Iterable&lt;<a\
                     href="A.html" title="annotation interface in pkg7">@A</a> java.lang.String&gt;<\
                    /span>&nbsp;<span class="element-name">m1</span><wbr><span class="parameters">(j\
                    ava.lang.Class&lt;? extends java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub5.html", true,
                """
                    <div class="member-signature"><span class="return-type">java.lang.Iterable&lt;ja\
                    va.lang.String&gt;</span>&nbsp;<span class="element-name">m1</span><wbr><span cl\
                    ass="parameters">(<a href="A.html" title="annotation interface in pkg7">@A</a>
                     <a href="A.html" title="annotation interface in pkg7">@A</a> java.lang.Class&lt\
                    ;? extends java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub6.html", true,
                """
                    <div class="member-signature"><span class="return-type">java.lang.Iterable&lt;ja\
                    va.lang.String&gt;</span>&nbsp;<span class="element-name">m1</span><wbr><span cl\
                    ass="parameters">(java.lang.Class&lt;<a href="A.html" title="annotation interfac\
                    e in pkg7">@A</a> ? extends java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub7.html", true,
                """
                    <div class="member-signature"><span class="return-type">java.lang.Iterable&lt;ja\
                    va.lang.String&gt;</span>&nbsp;<span class="element-name">m1</span><wbr><span cl\
                    ass="parameters">(java.lang.Class&lt;? extends <a href="A.html" title="annotatio\
                    n interface in pkg7">@A</a> java.lang.CharSequence&gt;&nbsp;p1,
                     int[]&nbsp;p2)</span></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");

        checkOutput("pkg7/AnnotatedSub8.html", true,
                """
                    <div class="member-signature"><span class="return-type">java.lang.Iterable&lt;ja\
                    va.lang.String&gt;</span>&nbsp;<span class="element-name">m1</span><wbr><span cl\
                    ass="parameters">(java.lang.Class&lt;? extends java.lang.CharSequence&gt;&nbsp;p1,
                     int <a href="A.html" title="annotation interface in pkg7">@A</a> []&nbsp;p2)</s\
                    pan></div>
                    <div class="block"><span class="descfrm-type-label">Description copied from inte\
                    rface:&nbsp;<code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">Ann\
                    otatedBase</a></code></span></div>
                    <div class="block">This is AnnotatedBase::m1.</div>
                    <dl class="notes">
                    <dt>Specified by:</dt>
                    <dd><code><a href="AnnotatedBase.html#m1(java.lang.Class,int%5B%5D)">m1</a></cod\
                    e>&nbsp;in interface&nbsp;<code><a href="AnnotatedBase.html" title="interface in\
                     pkg7">AnnotatedBase</a></code></dd>
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - first parameter</dd>
                    <dd><code>p2</code> - second parameter</dd>
                    <dt>Returns:</dt>
                    <dd>something</dd>
                    </dl>""");
    }
}
