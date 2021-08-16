/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4973609 8015249 8025633 8026567 6469561 8071982 8162363 8182765 8223364
             8242056 8261976
 * @summary  Make sure that annotation types with 0 members does not have
 *           extra HR tags.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestAnnotationTypes
 */

import javadoc.tester.JavadocTester;

public class TestAnnotationTypes extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestAnnotationTypes tester = new TestAnnotationTypes();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/AnnotationTypeField.html", true,
                """
                    <li>Summary:&nbsp;</li>
                    <li><a href="#field-summary">Field</a>&nbsp;|&nbsp;</li>""",
                """
                    <li>Detail:&nbsp;</li>
                    <li><a href="#field-detail">Field</a>&nbsp;|&nbsp;</li>""",
                "<!-- =========== FIELD SUMMARY =========== -->",
                "<h2>Field Summary</h2>",
                """
                    <div class="col-second even-row-color"><code><a href="#DEFAULT_NAME" class="memb\
                    er-name-link">DEFAULT_NAME</a></code></div>""",
                "<!-- ============ FIELD DETAIL =========== -->",
                """
                    <section class="detail" id="DEFAULT_NAME">
                    <h3>DEFAULT_NAME</h3>
                    <div class="member-signature"><span class="modifiers">static final</span>&nbsp;<\
                    span class="return-type">java.lang.String</span>&nbsp;<span class="element-name">DEFAULT_NAME</span></div>
                    """);

        checkOutput("pkg/AnnotationType.html", true,
                """
                    <li>Summary:&nbsp;</li>
                    <li>Field&nbsp;|&nbsp;</li>""",
                """
                    <li>Detail:&nbsp;</li>
                    <li>Field&nbsp;|&nbsp;</li>""");

        checkOutput("pkg/AnnotationType.html", true,
                    "<!-- ============ ANNOTATION INTERFACE MEMBER DETAIL =========== -->",
                    "<ul class=\"member-list\">",
                    "<li>",
                    """
                        <section class="details" id="annotation-interface-element-detail">""",
                    "<h2>Element Details</h2>",
                    "</a>",
                    "<ul class=\"member-list\">",
                    "<li>",
                    """
                        <section class="detail" id="value()">""",
                    "<h3>value</h3>\n",
                    """
                        <div class="member-signature"><span class="return-type">int</span>&nbsp;<span class="element-name">value</span></div>""");

        checkOutput("pkg/AnnotationType.html", false,
                """
                    <HR>

                    <P>

                    <P><!-- ========= END OF CLASS DATA ========= --><HR>""");
    }

    @Test
    public void testLinkSource() {
        javadoc("-d", "out-2",
                "-linksource",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("src-html/pkg/AnnotationType.html", true,
                "<title>Source code</title>",
                "@Documented public @interface AnnotationType {");

        checkOutput("src-html/pkg/AnnotationTypeField.html", true,
                "<title>Source code</title>",
                "@Documented public @interface AnnotationTypeField {");

        checkOutput("pkg/AnnotationType.html", true,
                """
                    <span class="modifiers">public @interface </span><span class="element-name"><a href="../src-html/pkg/AnnotationType.html#line-31">AnnotationType</a></span></div>""");

        checkOutput("pkg/AnnotationTypeField.html", true,
                """
                    <span class="modifiers">public @interface </span><span class="element-name"><a href="../src-html/pkg/AnnotationTypeField.html#line-31">AnnotationTypeField</a></span></div>""");
    }
}
