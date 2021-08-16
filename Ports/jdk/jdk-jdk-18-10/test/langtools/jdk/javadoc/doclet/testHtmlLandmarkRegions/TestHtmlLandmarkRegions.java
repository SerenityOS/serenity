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
 * @bug 8210047 8199892 8215599 8223378 8239817
 * @summary some pages contains content outside of landmark region
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestHtmlLandmarkRegions
 */


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import builder.ClassBuilder;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestHtmlLandmarkRegions extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestHtmlLandmarkRegions tester = new TestHtmlLandmarkRegions();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestHtmlLandmarkRegions() {
        tb = new ToolBox();
    }

    @Test
    public void testModules(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createModules(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-doctitle", "Document Title",
                "-header", "Test Header",
                "-bottom", "bottom text",
                "--module-source-path", srcDir.toString(),
                "--module", "m1,m2");

        checkExit(Exit.OK);

        checkOrder("index.html",
                """
                    <header role="banner" class="flex-header">
                    <nav role="navigation">""",
                """
                    <main role="main">
                    <div class="header">
                    <h1 class="title">Document Title</h1>""",
                """
                    <footer role="contentinfo"> """,
                """
                    bottom text"""
        );
    }

    @Test
    public void testPackages(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createPackages(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-doctitle", "Document Title",
                "-header", "Test Header",
                "-bottom", "bottom text",
                "-sourcepath", srcDir.toString(),
                "pkg1", "pkg2");

        checkExit(Exit.OK);

        checkOrder("index.html",
                """
                    <header role="banner" class="flex-header">
                    <nav role="navigation">""",
                """
                    <main role="main">
                    <div class="header">
                    <h1 class="title">Document Title</h1>""",
                """
                    <footer role="contentinfo">""",
                """
                    bottom text""");
    }

    @Test
    public void testDocFiles(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createPackages(srcDir);
        Path docFiles = Files.createDirectory(srcDir.resolve("pkg1").resolve("doc-files"));
        Files.write(docFiles.resolve("s.html"), List.of(
                """
                    <html>
                      <head>
                        <title>"Hello World"</title>
                      </head>
                      <body>
                         A sample doc file.
                      </body>
                    </html>"""));

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-bottom", "bottom text",
                "-sourcepath", srcDir.toString(),
                "pkg1", "pkg2");

        checkExit(Exit.OK);

        checkOrder("pkg1/doc-files/s.html",
                """
                    <header role="banner" class="flex-header">
                    <nav role="navigation">
                    """,
                """
                    <main role="main">A sample doc file""",
                """
                    <footer role="contentinfo">""",
                """
                    bottom text"""
                );
    }

    void createModules(Path srcDir) throws Exception {
        new ModuleBuilder(tb, "m1")
                .classes("package p1; public class a { }")
                .classes("package p2; public class b { }")
                .write(srcDir);
        new ModuleBuilder(tb, "m2")
                .classes("package p3; public class c { }")
                .classes("package p4; public class d { }")
                .write(srcDir);
    }

    void createPackages(Path srcDir) throws Exception {
        new ClassBuilder(tb, "pkg1.A")
                .setModifiers("public", "class")
                .write(srcDir);
        new ClassBuilder(tb, "pkg2.B")
                .setModifiers("public", "class")
                .write(srcDir);
    }
}
