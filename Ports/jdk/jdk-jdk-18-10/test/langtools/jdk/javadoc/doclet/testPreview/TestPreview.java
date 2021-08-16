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
 * @bug      8250768 8261976
 * @summary  test generated docs for items declared using preview
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.resources:+open
 * @build    javadoc.tester.*
 * @run main TestPreview
 */

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javadoc.tester.JavadocTester;

public class TestPreview extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestPreview tester = new TestPreview();
        tester.runTests();
    }

    @Test
    public void testUserJavadoc() {
        String doc = Paths.get(testSrc, "doc").toUri().toString();
        javadoc("-d", "out-user-javadoc",
                "-XDforcePreview", "--enable-preview", "-source", System.getProperty("java.specification.version"),
                "--patch-module", "java.base=" + Paths.get(testSrc, "api").toAbsolutePath().toString(),
                "--add-exports", "java.base/preview=m",
                "--module-source-path", testSrc,
                "-linkoffline", doc, doc,
                "m/pkg");
        checkExit(Exit.OK);

        checkOutput("m/pkg/TestPreviewDeclarationUse.html", true,
                    "<code><a href=\"TestPreviewDeclaration.html\" title=\"interface in pkg\">TestPreviewDeclaration</a></code>");
        checkOutput("m/pkg/TestPreviewAPIUse.html", true,
                "<a href=\"" + doc + "java.base/preview/Core.html\" title=\"class or interface in preview\" class=\"external-link\">Core</a><sup><a href=\"" + doc + "java.base/preview/Core.html#preview-preview.Core\" title=\"class or interface in preview\" class=\"external-link\">PREVIEW</a>");
        checkOutput("m/pkg/DocAnnotation.html", true,
                "<span class=\"modifiers\">public @interface </span><span class=\"element-name type-name-label\">DocAnnotation</span>");
        checkOutput("m/pkg/DocAnnotationUse1.html", true,
                "<div class=\"inheritance\">pkg.DocAnnotationUse1</div>");
        checkOutput("m/pkg/DocAnnotationUse2.html", true,
                "<div class=\"inheritance\">pkg.DocAnnotationUse2</div>");
    }

    @Test
    public void testPreviewAPIJavadoc() {
        javadoc("-d", "out-preview-api",
                "--patch-module", "java.base=" + Paths.get(testSrc, "api").toAbsolutePath().toString(),
                "--add-exports", "java.base/preview=m",
                "--source-path", Paths.get(testSrc, "api").toAbsolutePath().toString(),
                "--show-packages=all",
                "preview");
        checkExit(Exit.OK);

        checkOutput("preview-list.html", true,
                    """
                    <div id="record-class">
                    <div class="caption"><span>Record Classes</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Record Class</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="java.base/preview/CoreRecord.html" title="class in preview">preview.CoreRecord</a><sup><a href="java.base/preview/CoreRecord.html#preview-preview.CoreRecord">PREVIEW</a></sup></div>
                    <div class="col-last even-row-color"></div>
                    </div>
                    """,
                    """
                    <div id="method">
                    <div class="caption"><span>Methods</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Method</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-summary-item-name even-row-color"><a href="java.base/preview/CoreRecordComponent.html#i()">preview.CoreRecordComponent.i()</a><sup><a href="java.base/preview/CoreRecordComponent.html#preview-i()">PREVIEW</a></sup></div>
                    <div class="col-last even-row-color">
                    <div class="block">Returns the value of the <code>i</code> record component.</div>
                    </div>
                    """);
    }
}
