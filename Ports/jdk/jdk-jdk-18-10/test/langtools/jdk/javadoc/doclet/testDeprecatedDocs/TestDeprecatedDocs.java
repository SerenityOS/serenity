/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4927552 8026567 8071982 8162674 8175200 8175218 8183511 8186332
 *           8169819 8074407 8191030 8182765 8184205 8243533 8261976
 * @summary  test generated docs for deprecated items
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestDeprecatedDocs
 */

import javadoc.tester.JavadocTester;

public class TestDeprecatedDocs extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDeprecatedDocs tester = new TestDeprecatedDocs();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("deprecated-list.html", true,
                "annotation_test1 passes",
                "annotation_test2 passes",
                "annotation_test3 passes",
                "annotation_test4 passes.",
                "class_test1 passes",
                "class_test2 passes",
                "class_test3 passes",
                "class_test4 passes",
                "enum_test1 passes",
                "enum_test2 passes",
                "error_test1 passes",
                "error_test2 passes",
                "error_test3 passes",
                "error_test4 passes",
                "exception_test1 passes",
                "exception_test2 passes",
                "exception_test3 passes",
                "exception_test4 passes",
                "interface_test1 passes",
                "interface_test2 passes",
                "interface_test3 passes",
                "interface_test4 passes",
                "pkg.DeprecatedClassByAnnotation",
                "pkg.DeprecatedClassByAnnotation()",
                "pkg.DeprecatedClassByAnnotation.method()",
                "pkg.DeprecatedClassByAnnotation.field"
        );

        checkOutput("pkg/DeprecatedClassByAnnotation.html", true,
                """
                    <div class="type-signature"><span class="annotations">@Deprecated
                    </span><span class="modifiers">public class </span><span class="element-name type-name-label">DeprecatedClassByAnnotation</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public</span>&nbsp;<span class="return-type">int<\
                    /span>&nbsp;<span class="element-name">field</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span></div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public</span>&nbsp;<span class="element-name">DeprecatedClassByAnnotation</span>()</div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span></div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated
                    </span><span class="modifiers">public</span>&nbsp;<span class="return-type">void\
                    </span>&nbsp;<span class="element-name">method</span>()</div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span></div>""");

        checkOutput("pkg/TestAnnotationType.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    @Documented
                    </span><span class="modifiers">public @interface </span><span class="element-name type-n\
                    ame-label">TestAnnotationType</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">annotation_test1 passes.</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">static final</span>&nbsp;<span class="return-type\
                    ">int</span>&nbsp;<span class="element-name">field</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">annotation_test4 passes.</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="return-type">int</span>&nbsp;<span class="element-name">required</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">annotation_test3 passes.</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="return-type">java.lang.String</span>&\
                    nbsp;<span class="element-name">optional</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">annotation_test2 passes.</div>
                    </div>""");

        checkOutput("pkg/TestClass.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public class </span><span class="element-name type-name-label">TestClass</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">class_test1 passes.</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public</span>&nbsp;<span class="element-name">TestClass</span>()</div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">class_test3 passes. This is the second sentence\
                     of deprecated description for a constructor.</div>
                    </div>""",
                """
                    <div class="col-last even-row-color">
                    <div class="block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">class_test2 passes.</div>
                    </div>
                    </div>""",
                """
                    <div class="col-last even-row-color">
                    <div class="block"><span class="deprecated-label">Deprecated, for removal: This \
                    API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">class_test3 passes.</div>
                    </div>
                    </div>""",
                """
                    <div class="col-last odd-row-color">
                    <div class="block"><span class="deprecated-label">Deprecated, for removal: This \
                    API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">class_test4 passes.</div>
                    </div>
                    </div>""",
                """
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4 method-summary-table-tab6">
                    <div class="block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">class_test5 passes.</div>
                    </div>
                    </div>""",
                """
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4 method-summary-table-tab6">
                    <div class="block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">class_test6 passes.</div>
                    </div>
                    </div>""",
                """
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab4 method-summary-table-tab6">
                    <div class="block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">class_test7 passes.</div>
                    </div>
                    </div>""");

        checkOutput("pkg/TestClass.html", false,
                """
                    <div class="deprecation-comment">class_test2 passes. This is the second sentence\
                     of deprecated description for a field.</div>
                    </div>
                    </div>""",
                """
                    <div class="deprecation-comment">class_test3 passes. This is the second sentence\
                     of deprecated description for a constructor.</div>
                    </div>
                    </div>""",
                """
                    <div class="deprecation-comment">class_test4 passes. This is the second sentence\
                     of deprecated description for a method.</div>
                    </div>
                    </div>""");

        checkOutput("pkg/TestEnum.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public enum </span><span class="element-name type-name-label">TestEnum</span>
                    <span class="extends-implements">extends java.lang.Enum&lt;<a href="TestEnum.htm\
                    l" title="enum class in pkg">TestEnum</a>&gt;</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">enum_test1 passes.</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public static final</span>&nbsp;<span class="retu\
                    rn-type"><a href="TestEnum.html" title="enum class in pkg">TestEnum</a></span>&nbsp;<s\
                    pan class="element-name">FOR_REMOVAL</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">enum_test3 passes.</div>
                    </div>""");

        checkOutput("pkg/TestError.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public class </span><span class="element-name type-name-label">TestError</span>
                    <span class="extends-implements">extends java.lang.Error</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">error_test1 passes.</div>
                    </div>""");

        checkOutput("pkg/TestException.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public class </span><span class="element-name type-name-label">TestException</span>
                    <span class="extends-implements">extends java.lang.Exception</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">exception_test1 passes.</div>
                    </div>""");

        checkOutput("pkg/TestInterface.html", true,
                """
                    <hr>
                    <div class="type-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span><span class="modifiers">public class </span><span class="element-name type-name-label">TestInterface</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">interface_test1 passes.</div>
                    </div>""");

        checkOutput("deprecated-list.html", true,
                """
                    <ul>
                    <li><a href="#for-removal">Terminally Deprecated</a></li>
                    <li><a href="#class">Classes</a></li>
                    <li><a href="#enum-class">Enum Classes</a></li>
                    <li><a href="#exception">Exceptions</a></li>
                    <li><a href="#error">Errors</a></li>
                    <li><a href="#annotation-interface">Annotation Interfaces</a></li>
                    <li><a href="#field">Fields</a></li>
                    <li><a href="#method">Methods</a></li>
                    <li><a href="#constructor">Constructors</a></li>
                    <li><a href="#enum-constant">Enum Constants</a></li>
                    <li><a href="#annotation-interface-member">Annotation Interface Elements</a></li>
                    </ul>""",
                """
                    <div id="for-removal">
                    <div class="caption"><span>Terminally Deprecated Elements</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Element</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div id="enum-class">
                    <div class="caption"><span>Deprecated Enum Classes</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Enum Class</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestEnum.html" title="enum class in pkg">pkg.TestEnum</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">enum_test1 passes.</div>
                    </div>""",
                """
                    <div id="exception">
                    <div class="caption"><span>Deprecated Exceptions</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Exceptions</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestException.html" title="class in pkg">pkg.TestException</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">exception_test1 passes.</div>
                    </div>""",
                """
                    <div id="field">
                    <div class="caption"><span>Deprecated Fields</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Field</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/DeprecatedClassByAnnotation.html#field">pkg.DeprecatedClassByAnnotation.field</a></div>
                    <div class="col-last even-row-color"></div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestAnnotationType.html#field">pkg.TestAnnotationType.field</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">annotation_test4 passes.</div>
                    </div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestClass.html#field">pkg.TestClass.field</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">class_test2 passes. This is the second sentence of deprecated description for a field.</div>
                    </div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestError.html#field">pkg.TestError.field</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">error_test2 passes.</div>
                    </div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestException.html#field">pkg.TestException.field</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">exception_test2 passes.</div>
                    </div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestInterface.html#field">pkg.TestInterface.field</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">interface_test2 passes.</div>
                    </div>
                    </div>
                    </div>""",
                """
                    <div id="method">
                    <div class="caption"><span>Deprecated Methods</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Method</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/DeprecatedClassByAnnotation.html#method()">pkg.DeprecatedClassByAnnotation.method()</a></div>
                    <div class="col-last even-row-color"></div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestAnnotationType.html#optional()">pkg.TestAnnotationType.optional()</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">annotation_test2 passes.</div>
                    </div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestAnnotationType.html#required()">pkg.TestAnnotationType.required()</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">annotation_test3 passes.</div>
                    </div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestClass.html#method()">pkg.TestClass.method()</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">class_test5 passes. This is the second sentence of deprecated description for a method.</div>
                    </div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestClass.html#overloadedMethod(int)">pkg.TestClass.overloadedMethod<wbr>(int)</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">class_test7 passes. Overloaded method 2.</div>
                    </div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestClass.html#overloadedMethod(java.lang.String)">pkg.TestClass.overloadedMethod<wbr>(String)</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">class_test6 passes. Overloaded method 1.</div>
                    </div>""",
                """
                    <div id="constructor">
                    <div class="caption"><span>Deprecated Constructors</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Constructor</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/DeprecatedClassByAnnotation.html#%3Cinit%3E()">pkg.DeprecatedClassByAnnotation()</a></div>
                    <div class="col-last even-row-color"></div>
                    <div class="col-summary-item-name odd-row-color"><a href="pkg/TestClass.html#%3Cinit%3E()">pkg.TestClass()</a></div>
                    <div class="col-last odd-row-color">
                    <div class="deprecation-comment">class_test3 passes. This is the second sentence of deprecated description for a constructor.</div>
                    </div>
                    <div class="col-summary-item-name even-row-color"><a href="pkg/TestClass.html#%3Cinit%3E(java.lang.String)">pkg.TestClass<wbr>(String)</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">class_test4 passes. Overloaded constructor.</div>
                    </div>""");
    }
}
