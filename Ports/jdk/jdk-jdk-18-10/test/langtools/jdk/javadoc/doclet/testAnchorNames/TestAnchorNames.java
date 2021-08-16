/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025633 8025524 8081854 8187521 8182765 8261976
 * @summary Test for valid name attribute in HTML anchors.
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestAnchorNames
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestAnchorNames extends JavadocTester {

    public final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestAnchorNames tester = new TestAnchorNames();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public TestAnchorNames() {
        tb = new ToolBox();
    }

    @Test
    public void testHtml5(Path ignore) {
        javadoc("-d", "out-html5",
                "-sourcepath", testSrc,
                "-source", "8", //so that '_' can be used as an identifier
                "-use",
                "pkg1");
        checkExit(Exit.OK);

        // Test some section markers and links to these markers
        checkOutput("pkg1/RegClass.html", true,
                """
                    <span class="skip-nav" id="skip-navbar-top">""",
                """
                    <a href="#skip-navbar-top" title="Skip navigation links">""",
                """
                    <section class="nested-class-summary" id="nested-class-summary">
                    <h2>Nested Class Summary</h2>""",
                "<a href=\"#nested-class-summary\">",
                """
                    <section class="method-summary" id="method-summary">
                    <h2>Method Summary</h2>""",
                "<a href=\"#method-summary\">",
                """
                    <section class="field-details" id="field-detail">
                    <h2>Field Details</h2>""",
                "<a href=\"#field-detail\">",
                """
                    <section class="constructor-details" id="constructor-detail">
                    <h2>Constructor Details</h2>""",
                "<a href=\"#constructor-detail\">");

        // Test some members and link to these members
        checkOutput("pkg1/RegClass.html", true,
                //The marker for this appears in the serialized-form.html which we will
                //test below
                """
                    <a href="../serialized-form.html#pkg1.RegClass">""");

        // Test some fields
        checkOutput("pkg1/RegClass.html", true,
                "<section class=\"detail\" id=\"_\">",
                "<a href=\"#_\" class=\"member-name-link\">",
                "<section class=\"detail\" id=\"_$\">",
                "<a href=\"#_$\" class=\"member-name-link\">",
                "<section class=\"detail\" id=\"$_\">",
                "<a href=\"#$_\" class=\"member-name-link\">",
                """
                    <section class="detail" id="$field">""",
                "<a href=\"#$field\" class=\"member-name-link\">",
                """
                    <section class="detail" id="fieldInCla$$">""",
                "<a href=\"#fieldInCla$$\" class=\"member-name-link\">",
                """
                    <section class="detail" id="S_$$$$$INT">""",
                "<a href=\"#S_$$$$$INT\" class=\"member-name-link\">",
                """
                    <section class="detail" id="method$$">""",
                "<a href=\"#method$$\" class=\"member-name-link\">");

        checkOutput("pkg1/DeprMemClass.html", true,
                """
                    <section class="detail" id="_field_In_Class">""",
                "<a href=\"#_field_In_Class\" class=\"member-name-link\">");

        // Test constructor
        checkOutput("pkg1/RegClass.html", true,
                """
                    <section class="detail" id="&lt;init&gt;(java.lang.String,int)">""",
                """
                    <a href="#%3Cinit%3E(java.lang.String,int)" class="member-name-link">""");

        // Test some methods
        checkOutput("pkg1/RegClass.html", true,
                """
                    <section class="detail" id="_methodInClass(java.lang.String)">""",
                """
                    <a href="#_methodInClass(java.lang.String)" class="member-name-link">""",
                """
                    <section class="detail" id="method()">""",
                "<a href=\"#method()\" class=\"member-name-link\">",
                """
                    <section class="detail" id="foo(java.util.Map)">""",
                "<a href=\"#foo(java.util.Map)\" class=\"member-name-link\">",
                """
                    <section class="detail" id="methodInCla$s(java.lang.String[])">""",
                """
                    <a href="#methodInCla$s(java.lang.String%5B%5D)" class="member-name-link">""",
                """
                    <section class="detail" id="_methodInClas$(java.lang.String,int)">""",
                """
                    <a href="#_methodInClas$(java.lang.String,int)" class="member-name-link">""",
                """
                    <section class="detail" id="methodD(pkg1.RegClass.$A)">""",
                """
                    <a href="#methodD(pkg1.RegClass.$A)" class="member-name-link">""",
                """
                    <section class="detail" id="methodD(pkg1.RegClass.D[])">""",
                """
                    <a href="#methodD(pkg1.RegClass.D%5B%5D)" class="member-name-link">""");

        checkOutput("pkg1/DeprMemClass.html", true,
                """
                    <section class="detail" id="$method_In_Class()">""",
                "<a href=\"#$method_In_Class()\" class=\"member-name-link\">");

        // Test enum
        checkOutput("pkg1/RegClass.Te$t_Enum.html", true,
                """
                    <section class="detail" id="$FLD2">""",
                "<a href=\"#$FLD2\" class=\"member-name-link\">");

        // Test nested class
        checkOutput("pkg1/RegClass._NestedClas$.html", true,
                """
                    <section class="detail" id="&lt;init&gt;()">""",
                "<a href=\"#%3Cinit%3E()\" class=\"member-name-link\">");

        // Test class use page
        checkOutput("pkg1/class-use/DeprMemClass.html", true,
                """
                    <a href="../RegClass.html#d____mc" class="member-name-link">""");

        // Test deprecated list page
        checkOutput("deprecated-list.html", true,
                """
                    <a href="pkg1/DeprMemClass.html#_field_In_Class">""",
                """
                    <a href="pkg1/DeprMemClass.html#$method_In_Class()">""");

        // Test constant values page
        checkOutput("constant-values.html", true,
                """
                    <a href="pkg1/RegClass.html#S_$$$$$INT">""");

        // Test serialized form page
        checkOutput("serialized-form.html", true,
                //This is the marker for the link that appears in the pkg1.RegClass.html page
                """
                    <section class="serialized-class-details" id="pkg1.RegClass">""");

        // Test member name index page
        checkOutput("index-all.html", true,
                """
                    <h2 class="title" id="I:$">$</h2>""",
                "<a href=\"#I:$\">$",
                "<a href=\"#I:_\">_");
    }

    /**
     * The following test is somewhat simplistic, but it is useful
     * in conjunction with the W3C Validation Service at https://validator.w3.org/nu/#file
     * @param base A working directory for this method, in which some UTF-8 source files
     *      will be generated
     * @throws IOException if there is a problem generating the source files
     */
    @Test
    public void testNonAscii(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public class Def {\n"
                + "    public int \u00e0\u00e9;\n"              // a`e'
                + "    public void \u00c0\u00c9() { }\n"        // A`E'
                + "    public int \u03b1\u03b2\u03b3;\n"        // alpha beta gamma
                + "    public void \u0391\u0392\u0393() { }\n"  // ALPHA BETA GAMMA
                + "}",
                "package p; \n"
                + "/**\n"
                + " * {@link Def#\u00e0\u00e9 &agrave;&eacute;}<br>\n"
                + " * {@link Def#\u00c0\u00c9() &Agrave;&Eacute;}<br>\n"
                + " * {@link Def#\u03b1\u03b2\u03b3 &alpha;&beta;&gamma;}<br>\n"
                + " * {@link Def#\u0391\u0392\u0393() &Alpha;&Beta;&Gamma;}<br>\n"
                + " */\n"
                + "public class Ref { }");

        javadoc("-d", "out-nonAscii",
                "-sourcepath", src.toString(),
                "-html5",
                "-encoding", "utf-8",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/Def.html", true,
                "<section class=\"detail\" id=\"\u00e0\u00e9\">",
                "<section class=\"detail\" id=\"\u00c0\u00c9()\">",
                "<section class=\"detail\" id=\"\u03b1\u03b2\u03b3\">",
                "<section class=\"detail\" id=\"\u0391\u0392\u0393()\">");

        checkOutput("p/Ref.html", true,
                """
                    <a href="Def.html#%C3%A0%C3%A9"><code>&agrave;&eacute;</code></a>""",
                """
                    <a href="Def.html#%C3%80%C3%89()"><code>&Agrave;&Eacute;</code></a>""",
                """
                    <a href="Def.html#%CE%B1%CE%B2%CE%B3"><code>&alpha;&beta;&gamma;</code></a>""",
                """
                    <a href="Def.html#%CE%91%CE%92%CE%93()"><code>&Alpha;&Beta;&Gamma;</code></a>""");

    }
}
