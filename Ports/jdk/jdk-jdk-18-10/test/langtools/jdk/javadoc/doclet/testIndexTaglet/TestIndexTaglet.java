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
 * @bug 8202462
 * @summary {@index} may cause duplicate labels
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox builder.ClassBuilder
 * @run main TestIndexTaglet
 */


import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import builder.ClassBuilder.MethodBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestIndexTaglet extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestIndexTaglet tester = new TestIndexTaglet();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    TestIndexTaglet() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        MethodBuilder method = MethodBuilder
                .parse("public void func(A a) {}")
                .setComments("test description with {@index search_phrase_a class a}");

        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(method)
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");

        checkExit(Exit.OK);

        checkOrder("pkg/A.html",
                "<h2>Method Details</h2>\n",
                """
                    <div class="block">test description with <span id="search_phrase_a" class="searc\
                    h-tag-result">search_phrase_a</span></div>""");

        checkOrder("pkg/A.html",
                "<h2>Method Summary</h2>\n",
                """
                    <div class="block">test description with search_phrase_a</div>""");
    }

    @Test
    public void testIndexWithinATag(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        new ClassBuilder(tb, "pkg2.A")
                .setModifiers("public", "class")
                .addMembers(MethodBuilder.parse("public void func(){}")
                        .setComments("a within a : <a href='..'>{@index check}</a>"))
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg2");

        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "warning: {@index} tag, which expands to <a>, within <a>");
    }

    @Test
    public void testDuplicateReferences(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .setComments("This is a class. Here is {@index foo first}.")
                .addMembers(MethodBuilder.parse("public void m() {}")
                        .setComments("This is a method. Here is {@index foo second}."))
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");

        checkExit(Exit.OK);

        checkOutput("pkg/A.html", true,
                """
                    This is a class. Here is <span id="foo" class="search-tag-result">foo</span>.""",
                """
                    This is a method. Here is <span id="foo-1" class="search-tag-result">foo</span>.""");
    }
}
