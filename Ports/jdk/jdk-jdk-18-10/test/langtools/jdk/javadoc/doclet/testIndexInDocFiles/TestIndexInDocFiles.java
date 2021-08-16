/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8235414
 * @summary Module level doc-files show "unnamed package" as holder
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox
 * @run main TestIndexInDocFiles
 */


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestIndexInDocFiles extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestIndexInDocFiles tester = new TestIndexInDocFiles();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    TestIndexInDocFiles() {
        tb = new ToolBox();
    }

    /**
     * Test support for index tag and system properties in package level doc-files.
     * @param base the base directory for scratch files
     * @throws IOException if an exception occurs
     */
    @Test
    public void testPackageDocFiles(Path base) throws IOException {
        Path src = base.resolve("src");

        // write the skeletal Java files
        tb.writeJavaFiles(src,
                "public class A { }\n",
                "package p.q; public class C { }\n");

        // write the top level (unnamed package) doc file
        Path topLevelDocFiles = src.resolve("doc-files");
        tb.writeFile(topLevelDocFiles.resolve("top-level-file.html"),
                """
                    <html>
                    <head><title>Top level HTML file</title></head>
                    <body><h1>Package HTML file</h1>
                    {@index top-level-index additional info}
                    {@systemProperty top.level.property}
                    File content</body>
                    </html>
                    """);

        // write the (named) package level doc file
        Path pkgDocFiles = src.resolve("p").resolve("q").resolve("doc-files");
        tb.writeFile(pkgDocFiles.resolve("package-file.html"),
                """
                    <html>
                    <head><title>Package HTML file</title></head>
                    <body><h1>Package HTML file</h1>
                    {@index package-index additional info}
                    {@systemProperty package.property}
                    File content</body>
                    </html>
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                src.resolve("A.java").toString(), "p.q");
        checkExit(Exit.OK);

        checkOutput("doc-files/top-level-file.html", true,
                """
                    <h1>Package HTML file</h1>
                    <span id="top-level-index" class="search-tag-result">top-level-index</span>
                    <code><span id="top.level.property" class="search-tag-result">top.level.property</span></code>
                    """);
        checkOutput("p/q/doc-files/package-file.html", true,
                """
                    <h1>Package HTML file</h1>
                    <span id="package-index" class="search-tag-result">package-index</span>
                    <code><span id="package.property" class="search-tag-result">package.property</span></code>
                    """);
        checkOutput("tag-search-index.js", true,
                """
                    {"l":"package-index","h":"package p.q","d":"additional info","u":"p/q/doc-files/package-file.html#package-index"}""",
                """
                    {"l":"package.property","h":"package p.q","d":"System Property","u":"p/q/doc-files/package-file.html#package.property"}""",
                """
                    {"l":"top-level-index","h":"unnamed package","d":"additional info","u":"doc-files/top-level-file.html#top-level-index"}""",
                """
                    {"l":"top.level.property","h":"unnamed package","d":"System Property","u":"doc-f\
                    iles/top-level-file.html#top.level.property"}""");
    }

    /**
     * Test support for index tags and system properties in module and package level doc-files.
     * @param base the base directory for scratch files
     * @throws IOException if an exception occurs
     */
    @Test
    public void testModuleDocFiles(Path base) throws IOException {
        Path src = base.resolve("src");

        // write the skeletal Java files
        tb.writeJavaFiles(src,
                "module m.n { exports p.q; }\n",
                "public class A { }\n",
                "package p.q; public class C { }\n");

        // write the doc files for the module
        Path mdlDocFiles = src.resolve("doc-files");
        tb.writeFile(mdlDocFiles.resolve("module-file.html"),
                """
                    <html>
                    <head><title>Module HTML file</title></head>
                    <body><h1>Module HTML file</h1>
                    {@index module-index additional info}
                    {@systemProperty module.property}
                    File content</body>
                    </html>
                    """);

        // write the doc files for a package in the module
        Path pkgDocFiles = src.resolve("p").resolve("q").resolve("doc-files");
        tb.writeFile(pkgDocFiles.resolve("package-file.html"),
                """
                    <html>
                    <head><title>Package HTML file</title></head>
                    <body><h1>Package HTML file</h1>
                    {@index package-index additional info}
                    {@systemProperty package.property}
                    File content</body>
                    </html>
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "--module", "m.n");
        checkExit(Exit.OK);

        checkOutput("m.n/doc-files/module-file.html", true,
                """
                    <h1>Module HTML file</h1>
                    <span id="module-index" class="search-tag-result">module-index</span>
                    <code><span id="module.property" class="search-tag-result">module.property</span></code>
                    """);
        checkOutput("m.n/p/q/doc-files/package-file.html", true,
                """
                    <h1>Package HTML file</h1>
                    <span id="package-index" class="search-tag-result">package-index</span>
                    <code><span id="package.property" class="search-tag-result">package.property</span></code>
                    """);
        checkOutput("tag-search-index.js", true,
                """
                    {"l":"module-index","h":"module m.n","d":"additional info","u":"m.n/doc-files/module-file.html#module-index"}""",
                """
                    {"l":"package-index","h":"package p.q","d":"additional info","u":"m.n/p/q/doc-files/package-file.html#package-index"}""",
                """
                    {"l":"module.property","h":"module m.n","d":"System Property","u":"m.n/doc-files/module-file.html#module.property"}""",
                """
                    {"l":"package.property","h":"package p.q","d":"System Property","u":"m.n/p/q/doc\
                    -files/package-file.html#package.property"}""");
    }
}
