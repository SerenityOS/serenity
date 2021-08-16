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
 * @bug 8262886
 * @summary javadoc generates broken links with {@inheritDoc}
 * @modules jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library ../../lib /tools/lib
 * @build toolbox.ToolBox toolbox.ModuleBuilder javadoc.tester.*
 * @run main TestRelativeModuleLinks
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

public class TestRelativeModuleLinks extends JavadocTester {

    public final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestRelativeModuleLinks tester = new TestRelativeModuleLinks();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public TestRelativeModuleLinks() {
        tb = new ToolBox();
    }

    @Test
    public void testRelativeModuleLinks(Path base) throws IOException {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "ma")
                .classes("package pa; public class A {}")
                .exports("pa")
                .comment("""
                         <a href="doc-files/file.html">relative module link</a>,
                         <a href="#module-fragment">fragment module link</a>.
                         <a id="module-fragment">Module fragment</a>.""")
                .write(src);
        new ModuleBuilder(tb, "mb")
                .classes("package pb; public class B {}")
                .exports("pb")
                .requiresTransitive("ma")
                .write(src);
        Path docFiles = src.resolve("ma").resolve("doc-files");
        tb.writeFile(docFiles.resolve("file.html"),
                """
                    <html>
                    <head><title>Module HTML File</title></head>
                    <body><h1>Module HTML File</h1>
                    File content</body>
                    </html>
                    """);
        javadoc("-d", base.resolve("api").toString(),
                "-quiet",
                "--module-source-path", src.toString(),
                "--module", "ma,mb");

        checkExit(Exit.OK);

        // Main page
        checkOutput("index.html", true,
                """
                <div class="block"><a href="./ma/doc-files/file.html">relative module link</a>,
                 <a href="./ma/module-summary.html#module-fragment">fragment module link</a>.</div>""");

        // Index page
        checkOutput("index-all.html", true,
                """
                <div class="block"><a href="./ma/doc-files/file.html">relative module link</a>,
                 <a href="./ma/module-summary.html#module-fragment">fragment module link</a>.</div>""");

        // Own module page
        checkOutput("ma/module-summary.html", true,
                """
                <div class="block"><a href="doc-files/file.html">relative module link</a>,
                 <a href="#module-fragment">fragment module link</a>.
                 <a id="module-fragment">Module fragment</a>.</div>""");

        // Other module page
        checkOutput("mb/module-summary.html", true,
                """
                <div class="block"><a href="../ma/doc-files/file.html">relative module link</a>,
                 <a href="../ma/module-summary.html#module-fragment">fragment module link</a>.</div>""");
    }
}

