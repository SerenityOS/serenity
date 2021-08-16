/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190875 8215599
 * @summary modules not listed in overview/index page
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestIndexWithModules
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;


import javadoc.tester.JavadocTester;

public class TestIndexWithModules extends JavadocTester {

    final ToolBox tb;
    private final Path src;

    public static void main(String... args) throws Exception {
        TestIndexWithModules tester = new TestIndexWithModules();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestIndexWithModules() throws Exception{
        tb = new ToolBox();
        src = Paths.get("src");
        initModules();
    }

    @Test
    public void testIndexWithOverviewPath(Path base) throws Exception {
        Path out = base.resolve("out");

        tb.writeFile("overview.html",
                "<html><body>The overview summary page header</body></html>");

        javadoc("-d", out.toString(),
                "-overview", "overview.html",
                "--module-source-path", src.toString(),
                "--module", "m1");

        checkExit(Exit.OK);
        checkOrder("index.html",
                "The overview summary page header",
                "Modules",
                """
                    <a href="m1/module-summary.html">m1</a>""");

    }

    //multiple modules with frames
    @Test
    public void testIndexWithMultipleModules1(Path base) throws Exception {
        Path out = base.resolve("out");
        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "m1,m3,m4");

        checkExit(Exit.OK);

        checkOutput("overview-summary.html", true,
                "window.location.replace('index.html')");
        checkOrder("index.html",
                "Modules",
                """
                    <a href="m1/module-summary.html">m1</a>""",
                """
                    <a href="m3/module-summary.html">m3</a>""",
                """
                    <a href="m4/module-summary.html">m4</a>""");
    }

    //multiple modules with out frames
    @Test
    public void testIndexWithMultipleModules2(Path base) throws Exception {
        Path out = base.resolve("out");
        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "m1,m3,m4",
                "--no-frames");

        checkExit(Exit.OK);
        checkOrder("index.html",
                "Modules",
                """
                    <a href="m1/module-summary.html">m1</a>""",
                """
                    <a href="m3/module-summary.html">m3</a>""",
                """
                    <a href="m4/module-summary.html">m4</a>""");
    }

    @Test
    public void testIndexWithSingleModule(Path base) throws Exception {
        Path out = base.resolve("out");
        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "m2");

        checkExit(Exit.OK);
        checkOutput("index.html", true,
                "window.location.replace('m2/module-summary.html')");
    }

    //no modules and multiple packages
    @Test
    public void testIndexWithNoModules1(Path base) throws Exception{
        Path out = base.resolve("out");
        new ClassBuilder(tb, "P1.A1")
                .setModifiers("public","class")
                .write(src);

        new ClassBuilder(tb, "P2.A2")
                .setModifiers("public","class")
                .write(src);

        javadoc("-d", out.toString(),
                "--no-frames",
                "-sourcepath", src.toString(),
                "P1","P2");

        checkExit(Exit.OK);
        checkOrder("index.html",
                "Packages",
                """
                    <a href="P1/package-summary.html">P1</a>""",
                """
                    <a href="P2/package-summary.html">P2</a>""");

    }

    //no modules and one package
    @Test
    public void testIndexWithNoModules2(Path base) throws Exception{
        Path out = base.resolve("out");
        new ClassBuilder(tb, "P1.A1")
                .setModifiers("public","class")
                .write(src);

        javadoc("-d", out.toString(),
                "--no-frames",
                "-sourcepath", src.toString(),
                "P1");

        checkExit(Exit.OK);
        checkOrder("index.html",
                "window.location.replace('P1/package-summary.html')");
    }

    void initModules() throws Exception {
        new ModuleBuilder(tb, "m1")
                .exports("p1")
                .classes("package p1; public class c1{}")
                .write(src);

        new ModuleBuilder(tb, "m2")
                .exports("p1")
                .exports("p2")
                .classes("package p1; public class c1{}")
                .classes("package p2; public class c2{}")
                .write(src);

        new ModuleBuilder(tb, "m3").write(src);

        new ModuleBuilder(tb, "m4").write(src);
    }
}
