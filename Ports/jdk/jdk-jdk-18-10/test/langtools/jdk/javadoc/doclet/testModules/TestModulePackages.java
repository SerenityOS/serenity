/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178070 8196201 8184205 8246429 8198705
 * @summary Test packages table in module summary pages
 * @library /tools/lib ../../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ModuleBuilder toolbox.ToolBox javadoc.tester.*
 * @run main TestModulePackages
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javadoc.tester.JavadocTester;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

public class TestModulePackages extends JavadocTester {
    enum TabKind { EXPORTS, OPENS, CONCEALED };
    enum ColKind { EXPORTED_TO, OPENED_TO };

    public static void main(String... args) throws Exception {
        TestModulePackages tester = new TestModulePackages();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb;

    public TestModulePackages() {
        tb = new ToolBox();
    }

    // @Test: See: https://bugs.openjdk.java.net/browse/JDK-8193107
    public void empty(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("empty module")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkOutput("m/module-summary.html", false,
                """
                    <h3>Packages</h3>
                    <table class="packages-summary" summary="Packages table, listing packages, and an explanation">""");
    }

    @Test
    public void exportSingle(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports single package to all")
                .exports("p")
                .classes("package p; public class C { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.EXPORTS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");
    }

    @Test
    public void exportMultiple(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports multiple packages to all")
                .exports("p")
                .exports("q")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.EXPORTS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");
    }

    @Test
    public void exportSameName(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports same qualified package and types as module o")
                .exports("p")
                .classes("package p; public class C { }")
                .write(src);
        new ModuleBuilder(tb, "o")
                .comment("exports same qualified package and types as module m")
                .exports("p")
                .classes("package p; public class C { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "--module-source-path", src.toString(),
                "--module", "m,o");

        // error: the unnamed module reads package p from both o and m
        checkExit(Exit.ERROR);
        checkCaption("m", 3, TabKind.EXPORTS);
        checkCaption("o", 3, TabKind.EXPORTS);
        checkTableHead("m");
        checkTableHead("o");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");
        checkPackageRow("o", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");
        checkOutput("m/p/package-summary.html", true,
                """
                    <div class="sub-title"><span class="module-label-in-package">Module</span>&nbsp;<a href="../module-summary.html">m</a></div>
                    """);
        checkOutput("o/p/package-summary.html", true,
                """
                    <div class="sub-title"><span class="module-label-in-package">Module</span>&nbsp;<a href="../module-summary.html">o</a></div>
                    """);
        checkOutput("m/p/C.html", true,
                """
                    <div class="sub-title"><span class="module-label-in-type">Module</span>&nbsp;<a href="../module-summary.html">m</a></div>
                    <div class="sub-title"><span class="package-label-in-type">Package</span>&nbsp;<a href="package-summary.html">p</a></div>
                    """);
        checkOutput("o/p/C.html", true,
                """
                    <div class="sub-title"><span class="module-label-in-type">Module</span>&nbsp;<a href="../module-summary.html">o</a></div>
                    <div class="sub-title"><span class="package-label-in-type">Package</span>&nbsp;<a href="package-summary.html">p</a></div>
                    """);
        checkOutput("type-search-index.js", true,
                """
                     {"p":"p","m":"m","l":"C"},{"p":"p","m":"o","l":"C"}""");
        checkOutput("member-search-index.js", true,
                """
                     {"m":"m","p":"p","c":"C","l":"C()","u":"%3Cinit%3E()"}""",
                """
                     {"m":"o","p":"p","c":"C","l":"C()","u":"%3Cinit%3E()"}""");
    }

    @Test
    public void exportSomeQualified(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports multiple packages, some qualified")
                .exports("p")
                .exportsTo("q", "other")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        new ModuleBuilder(tb, "other")
                .comment("dummy module for target of export")
                .write(src);

        javadoc("-d", base.resolve("out-api").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.EXPORTS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");

        javadoc("-d", base.resolve("out-all").toString(),
                "-quiet",
                "-noindex",
                "--show-module-contents", "all",
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.EXPORTS);
        checkTableHead("m", ColKind.EXPORTED_TO);
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", "All Modules", null, "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab1",
                """
                    <a href="../other/module-summary.html">other</a>""", null, "&nbsp;");
    }

    @Test
    public void exportWithConcealed(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports package, has concealed package")
                .exports("p")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        javadoc("-d", base.resolve("out-api").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 4, TabKind.EXPORTS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", null, null, "&nbsp;");

        javadoc("-d", base.resolve("out-all").toString(),
                "-quiet",
                "-noindex",
                "--show-module-contents", "all",
                "--show-packages", "all",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.EXPORTS, TabKind.CONCEALED);
        checkTableHead("m", ColKind.EXPORTED_TO);
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab1", "All Modules", null, "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab3", "None", null, "&nbsp;");
    }

    @Test
    public void exportOpenWithConcealed(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("exports and opens qual and unqual, with concealed")
                .exports("e.all")
                .exportsTo("e.other", "other")
                .opens("o.all")
                .opensTo("o.other", "other")
                .exports("eo")
                .opens("eo")
                .classes("package e.all; public class CEAll { }")
                .classes("package e.other; public class CEOther { }")
                .classes("package o.all; public class COAll { }")
                .classes("package o.other; public class COOther { }")
                .classes("package eo; public class CEO { }")
                .classes("package c; public class C { }")
                .write(src);

        new ModuleBuilder(tb, "other")
                .comment("dummy module for target of export and open")
                .write(src);

        javadoc("-d", base.resolve("out-api").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 4, TabKind.EXPORTS, TabKind.OPENS);
        checkTableHead("m", ColKind.EXPORTED_TO, ColKind.OPENED_TO);
        checkPackageRow("m", "e.all", 0, "package-summary-table package-summary-table-tab1", "All Modules", "None", "&nbsp;");
        checkPackageRow("m", "eo", 1, "package-summary-table package-summary-table-tab1 package-summary-table-tab2", "All Modules", "All Modules", "&nbsp;");

        javadoc("-d", base.resolve("out-all").toString(),
                "-quiet",
                "-noindex",
                "--show-module-contents", "all",
                "--show-packages", "all",
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 4, TabKind.EXPORTS, TabKind.OPENS, TabKind.CONCEALED);
        checkTableHead("m", ColKind.EXPORTED_TO, ColKind.OPENED_TO);
        checkPackageRow("m", "c", 0, "package-summary-table package-summary-table-tab3", "None", "None", "&nbsp;");
        checkPackageRow("m", "e.all", 1, "package-summary-table package-summary-table-tab1", "All Modules", "None", "&nbsp;");
        checkPackageRow("m", "e.other", 2, "package-summary-table package-summary-table-tab1",
                """
                    <a href="../other/module-summary.html">other</a>""", "None", "&nbsp;");
        checkPackageRow("m", "eo", 3, "package-summary-table package-summary-table-tab1 package-summary-table-tab2", "All Modules", "All Modules", "&nbsp;");
        checkPackageRow("m", "o.all", 4, "package-summary-table package-summary-table-tab2", "None", "All Modules", "&nbsp;");
        checkPackageRow("m", "o.other", 5, "package-summary-table package-summary-table-tab2", "None",
                """
                    <a href="../other/module-summary.html">other</a>""", "&nbsp;");
    }

    @Test
    public void openModule(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, true, "m")
                .comment("open module")
                .classes("/** implicitly open package */ package p;")
                .classes("package p; public class C { } ")
                .classes("/** implicitly open package */ package q;")
                .classes("package q; public class D { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, null,
                """

                    <div class="block">implicitly open package</div>
                    """);
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab2", null, null,
                """

                    <div class="block">implicitly open package</div>
                    """);
    }
    @Test
    public void openSingle(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("opens single package to all")
                .opens("p")
                .classes("package p; public class C { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, null, "&nbsp;");
    }

    @Test
    public void openMultiple(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("opens multiple packages to all")
                .opens("p")
                .opens("q")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, null, "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab2", null, null, "&nbsp;");
    }

    @Test
    public void openSomeQualified(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("opens multiple packages, some qualified")
                .opens("p")
                .opensTo("q", "other")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        new ModuleBuilder(tb, "other")
                .comment("dummy module for target of export")
                .write(src);

        javadoc("-d", base.resolve("out-api").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, null, "&nbsp;");

        javadoc("-d", base.resolve("out-all").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--show-module-contents", "all",
                "--module-source-path", src.toString(),
                "--module", "m,other");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m", ColKind.OPENED_TO);
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, "All Modules", "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab2", null,
                """
                    <a href="../other/module-summary.html">other</a>""", "&nbsp;");
    }

    @Test
    public void openWithConcealed(Path base) throws Exception {
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m")
                .comment("opens package, has concealed package")
                .opens("p")
                .classes("package p; public class C { }")
                .classes("package q; public class D { }")
                .write(src);

        javadoc("-d", base.resolve("out-api").toString(),
                "-quiet",
                "-noindex",
                "--show-packages", "all",  // required, to show open packages; see JDK-8193107
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS);
        checkTableHead("m");
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, null, "&nbsp;");

        javadoc("-d", base.resolve("out-all").toString(),
                "-quiet",
                "-noindex",
                "--show-module-contents", "all",
                "--show-packages", "all",
                "--module-source-path", src.toString(),
                "--module", "m");

        checkExit(Exit.OK);
        checkCaption("m", 3, TabKind.OPENS, TabKind.CONCEALED);
        checkTableHead("m", ColKind.OPENED_TO);
        checkPackageRow("m", "p", 0, "package-summary-table package-summary-table-tab2", null, "All Modules", "&nbsp;");
        checkPackageRow("m", "q", 1, "package-summary-table package-summary-table-tab3", null, "None", "&nbsp;");
    }


    private void checkCaption(String moduleName, int numberOfColumns, TabKind... kinds) {
        String expect;
        if (kinds.length > 1) {
            Set<TabKind> kindSet = Set.of(kinds);
            StringBuilder sb = new StringBuilder();
            sb.append("""
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="package-summary-table-tab0" role="tab" aria-selected="true" aria-con\
                    trols="package-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)"\
                     onclick="show('package-summary-table', 'package-summary-table',\s"""
                    + numberOfColumns + """
                    )" class="active-table-tab">All Packages</button>""");
            if (kindSet.contains(TabKind.EXPORTS)) {
                sb.append("""
                    <button id="package-summary-table-tab1" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab1',\s"""
                    + numberOfColumns + """
                    )" class="table-tab">Exports</button>""");
            }
            if (kindSet.contains(TabKind.OPENS)) {
                sb.append("""
                    <button id="package-summary-table-tab2" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab2',\s"""
                    + numberOfColumns + """
                    )" class="table-tab">Opens</button>""");
            }
            if (kindSet.contains(TabKind.CONCEALED)) {
                sb.append("""
                   <button id="package-summary-table-tab3" role="tab" aria-selected="false" aria-con\
                   trols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                    onclick="show('package-summary-table', 'package-summary-table-tab3',\s"""
                   + numberOfColumns + """
                   )" class="table-tab">Concealed</button>""");
            }
            sb.append("</div>");
            expect = sb.toString();
        } else {
            TabKind k = kinds[0];
            String name = k.toString().charAt(0) + k.toString().substring(1).toLowerCase();
            expect = "<div class=\"caption\"><span>" + name + "</span></div>";
        }

        checkOutput(moduleName + "/module-summary.html", true, expect);
    }


    private void checkTableHead(String moduleName, ColKind... kinds) {
        Set<ColKind> kindSet = Set.of(kinds);
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <div class="table-header col-first">Package</div>
            """);
        if (kindSet.contains(ColKind.EXPORTED_TO)) {
            sb.append("""
                <div class="table-header col-second">Exported To Modules</div>
                """);
        }
        if (kindSet.contains(ColKind.OPENED_TO)) {
            sb.append("""
                <div class="table-header col-second">Opened To Modules</div>
                """);
        }
        sb.append("""
            <div class="table-header col-last">Description</div>""");

        checkOutput(moduleName + "/module-summary.html", true, sb.toString());
    }

    private void checkPackageRow(String moduleName, String packageName, int index,
            String classes, String exportedTo, String openedTo, String desc) {
        StringBuilder sb = new StringBuilder();
        String color = (index % 2 == 1 ? "odd-row-color" : "even-row-color");
        sb.append("<div class=\"col-first " + color + " " + classes + "\"><a href=\""
                + packageName.replace('.', '/') + "/package-summary.html\">"
                + packageName + "</a></div>\n");
        if (exportedTo != null) {
            sb.append("<div class=\"col-second " + color + " " + classes + "\">" + exportedTo + "</div>\n");
        }
        if (openedTo != null) {
            sb.append("<div class=\"col-second " + color + " " + classes + "\">" + openedTo + "</div>\n");
        }
        sb.append("<div class=\"col-last " + color + " " + classes + "\">" + desc + "</div>");

        checkOutput(moduleName + "/module-summary.html", true, sb.toString());
    }

}

