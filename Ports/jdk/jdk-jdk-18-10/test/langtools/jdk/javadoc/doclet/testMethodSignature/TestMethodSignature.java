/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8214126 8241470 8259499
 * @summary  Method signatures not formatted correctly in browser
 * @library  ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestMethodSignature
 */

import javadoc.tester.JavadocTester;

public class TestMethodSignature extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMethodSignature tester = new TestMethodSignature();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                """
                    <div class="member-signature"><span class="annotations">@Generated("GeneratedConstructor")
                    </span><span class="modifiers">public</span>&nbsp;<span class="element-name">C</span>()</div>""",

                """
                    <div class="member-signature"><span class="modifiers">public static</span>&nbsp;\
                    <span class="return-type">void</span>&nbsp;<span class="element-name">simpleMeth\
                    od</span><wbr><span class="parameters">(int&nbsp;i,
                     java.lang.String&nbsp;s,
                     boolean&nbsp;b)</span></div>""",

                """
                    <div class="member-signature"><span class="annotations">@Generated(value="SomeGeneratedName",
                               date="a date",
                               comments="some comment about the method below")
                    </span><span class="modifiers">public static</span>&nbsp;<span class="return-typ\
                    e">void</span>&nbsp;<span class="element-name">annotatedMethod</span><wbr><span \
                    class="parameters">(int&nbsp;i,
                     java.lang.String&nbsp;s,
                     boolean&nbsp;b)</span></div>""",

                """
                    <div class="member-signature"><span class="modifiers">public static</span>&nbsp;\
                    <span class="type-parameters-long">&lt;T1 extends java.lang.AutoCloseable,<wbr>
                    T2 extends java.lang.AutoCloseable,<wbr>
                    T3 extends java.lang.AutoCloseable,<wbr>
                    T4 extends java.lang.AutoCloseable,<wbr>
                    T5 extends java.lang.AutoCloseable,<wbr>
                    T6 extends java.lang.AutoCloseable,<wbr>
                    T7 extends java.lang.AutoCloseable,<wbr>
                    T8 extends java.lang.AutoCloseable&gt;</span>
                    <span class="return-type"><a href="C.With8Types.html" title="class in pkg">C.Wit\
                    h8Types</a>&lt;T1,<wbr>T2,<wbr>T3,<wbr>T4,<wbr>T5,<wbr>T6,<wbr>T7,<wbr>T8&gt;</s\
                    pan>&nbsp;<span class="element-name">bigGenericMethod</span><wbr><span class="pa\
                    rameters">(<a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T1\
                    &gt;&nbsp;t1,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T2&gt;&nbsp;t2,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T3&gt;&nbsp;t3,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T4&gt;&nbsp;t4,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T5&gt;&nbsp;t5,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T6&gt;&nbsp;t6,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T7&gt;&nbsp;t7,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T8&gt;&nbsp;t8)</span>
                                                                    throws <span class="exceptions">java.lang.IllegalArgumentException,
                    java.lang.IllegalStateException</span></div>""",

                """
                    <div class="member-signature"><span class="annotations">@Generated(value="SomeGeneratedName",
                               date="a date",
                               comments="some comment about the method below")
                    </span><span class="modifiers">public static</span>&nbsp;<span class="type-param\
                    eters-long">&lt;T1 extends java.lang.AutoCloseable,<wbr>
                    T2 extends java.lang.AutoCloseable,<wbr>
                    T3 extends java.lang.AutoCloseable,<wbr>
                    T4 extends java.lang.AutoCloseable,<wbr>
                    T5 extends java.lang.AutoCloseable,<wbr>
                    T6 extends java.lang.AutoCloseable,<wbr>
                    T7 extends java.lang.AutoCloseable,<wbr>
                    T8 extends java.lang.AutoCloseable&gt;</span>
                    <span class="return-type"><a href="C.With8Types.html" title="class in pkg">C.Wit\
                    h8Types</a>&lt;T1,<wbr>T2,<wbr>T3,<wbr>T4,<wbr>T5,<wbr>T6,<wbr>T7,<wbr>T8&gt;</s\
                    pan>&nbsp;<span class="element-name">bigGenericAnnotatedMethod</span><wbr><span \
                    class="parameters">(<a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? e\
                    xtends T1&gt;&nbsp;t1,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T2&gt;&nbsp;t2,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T3&gt;&nbsp;t3,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T4&gt;&nbsp;t4,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T5&gt;&nbsp;t5,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T6&gt;&nbsp;t6,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T7&gt;&nbsp;t7,
                     <a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;? extends T8&gt;&nbsp;t8)</span>
                                                                             throws <span class="exc\
                    eptions">java.lang.IllegalArgumentException,
                    java.lang.IllegalStateException</span></div>
                    <div class="block">Generic method with eight type args and annotation.</div>""",
                """
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type"><a href="C.Generic.html" title="class in pkg">C.Generic</a>&l\
                    t;java.lang.Integer&gt;.<a href="C.Generic.Inner.html" title="class in pkg">Inne\
                    r</a></span>&nbsp;<span class="element-name">nestedGeneric1</span><wbr><span cla\
                    ss="parameters">(<a href="C.Generic.html" title="class in pkg">C.Generic</a>&lt;\
                    java.lang.Integer&gt;.<a href="C.Generic.Inner.html" title="class in pkg">Inner<\
                    /a>&nbsp;i,
                     <a href="C.Generic.html" title="class in pkg">C.Generic</a>&lt;<a href="C.html"\
                     title="class in pkg">C</a>&gt;.<a href="C.Generic.Inner.html" title="class in p\
                    kg">Inner</a>&nbsp;j)</span></div>""",
                """
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type"><a href="C.Generic.html" title="class in pkg">C.Generic</a>&l\
                    t;<a href="C.F0.html" title="interface in pkg">C.F0</a>&lt;<a href="C.html" titl\
                    e="class in pkg">C</a>&gt;&gt;.<a href="C.Generic.Inner.html" title="class in pk\
                    g">Inner</a>.<a href="C.Generic.Inner.Foo.html" title="class in pkg">Foo</a></sp\
                    an>&nbsp;<span class="element-name">nestedGeneric2</span><wbr><span class="param\
                    eters">(<a href="C.Generic.html" title="class in pkg">C.Generic</a>&lt;java.lang\
                    .Integer&gt;.<a href="C.Generic.Inner.html" title="class in pkg">Inner</a>.<a hr\
                    ef="C.Generic.Inner.Foo.html" title="class in pkg">Foo</a>&nbsp;f,
                     <a href="C.Generic.html" title="class in pkg">C.Generic</a>&lt;<a href="C.F0.ht\
                    ml" title="interface in pkg">C.F0</a>&lt;<a href="C.html" title="class in pkg">C\
                    </a>&gt;&gt;.<a href="C.Generic.Inner.html" title="class in pkg">Inner</a>.<a hr\
                    ef="C.Generic.Inner.Foo.html" title="class in pkg">Foo</a>&nbsp;g)</span></div>""");

    }
}
