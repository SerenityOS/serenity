/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8213957 8213958
 * @summary Test use of at-index in package-iinfo and doc-files
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestIndexInPackageFiles
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestIndexInPackageFiles extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestIndexInPackageFiles  tester = new TestIndexInPackageFiles ();
        tester.runTests();
    }

    ToolBox tb = new ToolBox();

    @Test
    public void test() throws IOException {
        Path src = Path.of("src");
        tb.writeJavaFiles(src,
              """
                  /**
                   * Summary.
                   * {@index test.name.1 additional info}
                   * {@systemProperty test.property.1}
                   */
                  package p.q;""",
              """
                  package p.q;
                  /** This is a class in p.q. */
                  public class C { }
                  """);

        tb.writeFile(src.resolve("p/q/doc-files/extra.html"),
            """
                <html><head><title>Extra</title></head><body>
                <h1>Extra</h1>
                {@index test.name.2 additional info}
                {@systemProperty test.property.2}
                </body></html>
                """);

        tb.writeFile("overview.html",
            """
                <html><head><title>Overview</title></head><body>
                <h1>Overview</h1>
                {@index test.name.3 additional info}
                </body></html>
                """);


        javadoc("-d", "out",
                "-sourcepath", src.toString(),
                "-overview", "overview.html",
                "p.q");

        checkExit(Exit.OK);

        // Note there is an implicit call to checkLinks, but that only
        // checks the links are valid if they are actually present.
        // Here, we specifically check for both ends of each link.
        // However, we assume the search index files are generated appropriately,
        // to match the A-Z index files checked here.

        checkOutput("p/q/package-summary.html", true,
            """
                <span id="test.name.1" class="search-tag-result">test.name.1</span>""",
            """
                <span id="test.property.1" class="search-tag-result">test.property.1</span>""");

        checkOutput("p/q/doc-files/extra.html", true,
            """
                <span id="test.name.2" class="search-tag-result">test.name.2</span>""",
            """
                <span id="test.property.2" class="search-tag-result">test.property.2</span>""");

        checkOutput("index.html", true,
            """
                <span id="test.name.3" class="search-tag-result">test.name.3</span>""");

        checkOutput("index-all.html", true,
            """
                <a href="p/q/package-summary.html#test.name.1" class="search-tag-link">test.name.1</a>""",
            """
                <a href="p/q/doc-files/extra.html#test.name.2" class="search-tag-link">test.name.2</a>""",
            """
                <a href="index.html#test.name.3" class="search-tag-link">test.name.3</a> - Search tag in Overview</dt>""",
            """
                <a href="p/q/package-summary.html#test.property.1" class="search-tag-link">test.property.1</a>""",
            """
                <a href="p/q/doc-files/extra.html#test.property.2" class="search-tag-link">test.property.2</a>""");
    }
}

