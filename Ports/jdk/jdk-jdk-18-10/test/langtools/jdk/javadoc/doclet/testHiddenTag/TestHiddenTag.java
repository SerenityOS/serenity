/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8073100 8182765 8196202 8261079 8261976
 * @summary ensure the hidden tag works as intended
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestHiddenTag
 */

import javadoc.tester.JavadocTester;

public class TestHiddenTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHiddenTag tester = new TestHiddenTag();
        tester.runTests();
    }

    /**
     * Perform tests on &#64;hidden tags
     */
    @Test
    public void test1() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-package",
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/A.html", true,
                """
                    <section class="detail" id="visibleField">""",
                """
                    <section class="detail" id="visibleMethod()">""",
                """
                    <dt>Direct Known Subclasses:</dt>
                    <dd><code><a href="A.VisibleInner.html" title="class in pkg1">A.VisibleInner</a>\
                    </code>, <code><a href="A.VisibleInnerExtendsInvisibleInner.html" title="class i\
                    n pkg1">A.VisibleInnerExtendsInvisibleInner</a></code></dd>""");

        checkOutput("pkg1/A.html", false,
                "invisibleField",
                "invisibleMethod()");

        checkOutput("pkg1/A.VisibleInner.html", true,
                """
                    <code><a href="A.html#visibleField">visibleField</a></code>""",
                """
                    <code><a href="A.html#visibleMethod()">visibleMethod</a></code>""",
                """
                    <h2 id="nested-classes-inherited-from-class-pkg1.A">Nested classes/interfaces in\
                    herited from class&nbsp;pkg1.<a href="A.html" title="class in pkg1">A</a></h2>
                    <code><a href="A.VisibleInner.html" title="class in pkg1">A.VisibleInner</a>, <a\
                     href="A.VisibleInnerExtendsInvisibleInner.html" title="class in pkg1">A.Visible\
                    InnerExtendsInvisibleInner</a></code></div>
                    """);

        checkOutput("pkg1/A.VisibleInner.html", false,
                "../pkg1/A.VisibleInner.html#VisibleInner()",
                "invisibleField",
                "invisibleMethod()");

        checkOutput("pkg1/A.VisibleInnerExtendsInvisibleInner.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public static class </span><\
                    span class="element-name type-name-label">A.VisibleInnerExtendsInvisibleInner</span>
                    <span class="extends-implements">extends <a href="A.html" title="class in pkg1">A</a></span></div>""",
                """
                    <code><a href="A.html#visibleField">visibleField</a></code>""",
                """
                    <code><a href="A.html#visibleMethod()">visibleMethod</a></code>""");

        checkOutput("pkg1/A.VisibleInnerExtendsInvisibleInner.html", false,
                "invisibleField",
                "invisibleMethod",
                "A.InvisibleInner");

        checkOutput("pkg1/Intf.html", true,
                """
                    <section class="detail" id="visibleDefaultMethod()">""",
                """
                    <section class="detail" id="visibleInterfaceMethod()">""",
                """
                    <dt>All Known Implementing Classes:</dt>
                    <dd><code><a href="Child.html" title="class in pkg1">Child</a></code></dd>
                    </dl>""");

        checkOutput("pkg1/Intf.html", false,
                "InvisibleParent",
                "invisibleDefaultMethod",
                "invisibleInterfaceMethod");

        checkOutput("pkg1/Child.html", true,
                """
                    <a href="InvisibleParent.VisibleInner.html" class="type-name-link" title="class \
                    in pkg1">InvisibleParent.VisibleInner</a>""",
                """
                    <a href="#visibleField" class="member-name-link">visibleField</a>""",
                """
                    <a href="#invisibleInterfaceMethod()" class="member-name-link">invisibleInterfaceMethod</a>""",
                """
                    <a href="#visibleInterfaceMethod()" class="member-name-link">visibleInterfaceMethod</a>""",
                """
                    <a href="#visibleMethod(pkg1.InvisibleParent)" class="member-name-link">visibleMethod</a>""",
                """
                    <a href="Intf.html#visibleDefaultMethod()">visibleDefaultMethod</a>""",
                // Invisible return or parameter types must not be linked
                """
                    <span class="return-type">pkg1.InvisibleParent</span>""",
                """
                    <span class="parameters">(pkg1.InvisibleParent&lt;? extends pkg1.InvisibleParent&gt;&nbsp;p)</span>""");

        checkOutput("pkg1/Child.html", false,
                "InvisibleParent.InvisibleInner",
                "invisibleField",
                "invisibleMethod",
                "invisibleDefaultMethod");

        checkOutput("pkg1/InvisibleParent.VisibleInner.html", true,
                """
                    <dt>Enclosing class:</dt>
                    <dd>pkg1.InvisibleParent&lt;T extends pkg1.InvisibleParent&gt;</dd>
                    </dl>""");

        checkOutput("pkg1/package-summary.html", false, "A.InvisibleInner");

        checkOutput("pkg1/package-tree.html", false, "A.InvisibleInner");

        checkOutput("pkg1/package-tree.html", false, "InvisibleParent.html");

        checkFiles(false,
                "pkg1/A.InvisibleInner.html",
                "pkg1/A.InvisibleInnerExtendsVisibleInner.html",
                "pkg1/InvisibleParent.html",
                "pkg1/InvisibleParent.InvisibleInner.html");
    }
}
