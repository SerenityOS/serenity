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
 * @bug 8154119 8154262 8156077 8157987 8154261 8154817 8135291 8155995 8162363
 *      8168766 8168688 8162674 8160196 8175799 8174974 8176778 8177562 8175218
 *      8175823 8166306 8178043 8181622 8183511 8169819 8074407 8183037 8191464
 *      8164407 8192007 8182765 8196200 8196201 8196202 8196202 8205593 8202462
 *      8184205 8219060 8223378 8234746 8239804 8239816 8253117 8245058 8261976
 * @summary Test modules support in javadoc.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestModules
 */
import javadoc.tester.JavadocTester;

public class TestModules extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestModules tester = new TestModules();
        tester.runTests();
    }

    /**
     * Test generated module pages for HTML 5.
     */
    @Test
    public void testHtml5() {
        javadoc("-d", "out-html5",
                "-use",
                "-Xdoclint:none",
                "-overview", testSrc("overview.html"),
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "testpkgmdlA", "testpkgmdlB");
        checkExit(Exit.OK);
        checkHtml5Description(true);
        checkHtml5NoDescription(false);
        checkHtml5OverviewSummaryModules();
        checkModuleLink();
        checkModuleFilesAndLinks(true);
        checkModulesInSearch(true);
        checkAllPkgsAllClasses(true);
    }

    /**
     * Test generated module pages for HTML 5 with -nocomment option.
     */
    @Test
    public void testHtml5NoComment() {
        javadoc("-d", "out-html5-nocomment",
                "-nocomment",
                "-use",
                "-Xdoclint:none",
                "--no-platform-links",
                "-overview", testSrc("overview.html"),
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "testpkgmdlA", "testpkgmdlB");
        checkExit(Exit.OK);
        checkHtml5Description(false);
        checkHtml5NoDescription(true);
        checkModuleLink();
        checkModuleFilesAndLinks(true);
    }

    /**
     * Test generated pages, in an unnamed module, for HTML 5.
     */
    @Test
    public void testHtml5UnnamedModule() {
        javadoc("-d", "out-html5-nomodule",
                "-use",
                "-overview", testSrc("overview.html"),
                "-sourcepath", testSrc,
                "testpkgnomodule", "testpkgnomodule1");
        checkExit(Exit.OK);
        checkHtml5OverviewSummaryPackages();
        checkModuleFilesAndLinks(false);
        checkModulesInSearch(false);
    }

    /**
     * Test generated module pages with javadoc tags.
     */
    @Test
    public void testJDTagsInModules() {
        javadoc("-d", "out-mdltags",
                "-author",
                "-version",
                "-Xdoclint:none",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "--module", "moduletags,moduleB",
                "testpkgmdltags", "testpkgmdlB");
        checkExit(Exit.OK);
        checkModuleTags();
    }

    /**
     * Test generated module summary page.
     */
    @Test
    public void testModuleSummary() {
        javadoc("-d", "out-moduleSummary",
                "-use",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "testpkgmdlA", "testpkgmdlB", "moduleB/testpkg2mdlB");
        checkExit(Exit.OK);
        checkModuleSummary();
        checkNegatedModuleSummary();
    }

    /**
     * Test generated module summary page of an aggregating module.
     */
    @Test
    public void testAggregatorModuleSummary() {
        javadoc("-d", "out-aggregatorModuleSummary",
                "-use",
                "--module-source-path", testSrc,
                "--expand-requires", "transitive",
                "--module", "moduleT");
        checkExit(Exit.OK);
        checkAggregatorModuleSummary();
    }

    /**
     * Test generated module pages and pages with link to modules.
     */
    @Test
    public void testModuleFilesAndLinks() {
        javadoc("-d", "out-modulelinks",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "testpkgmdlA", "testpkgmdlB");
        checkExit(Exit.OK);
        checkModuleFilesAndLinks(true);
    }

    /**
     * Test generated module pages for a deprecated module.
     */
    @Test
    public void testModuleDeprecation() {
        javadoc("-d", "out-moduledepr",
                "-Xdoclint:none",
                "--no-platform-links",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB,moduletags",
                "testpkgmdlA", "testpkgmdlB", "testpkgmdltags");
        checkExit(Exit.OK);
        checkModuleDeprecation(true);
    }

    /**
     * Test annotations on modules.
     */
    @Test
    public void testModuleAnnotation() {
        javadoc("-d", "out-moduleanno",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "testpkgmdlA", "testpkgmdlB");
        checkExit(Exit.OK);
        checkModuleAnnotation();
    }

    /**
     * Test module summary pages in "api" mode.
     */
    @Test
    public void testApiMode() {
        javadoc("-d", "out-api",
                "-use",
                "--show-module-contents=api",
                "-author",
                "-version",
                "-Xdoclint:none",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB,moduleC,moduletags",
                "testpkgmdlA", "moduleA/concealedpkgmdlA", "testpkgmdlB", "testpkg2mdlB", "testpkgmdlC", "testpkgmdltags");
        checkExit(Exit.OK);
        checkModuleModeCommon();
        checkModuleModeApi(true);
        checkModuleModeAll(false);
    }

    /**
     * Test module summary pages in "all" mode.
     */
    @Test
    public void testAllMode() {
        javadoc("-d", "out-all",
                "-use",
                "--show-module-contents=all",
                "-author",
                "-version",
                "-Xdoclint:none",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB,moduleC,moduletags",
                "testpkgmdlA", "moduleA/concealedpkgmdlA", "testpkgmdlB", "testpkg2mdlB", "testpkgmdlC", "testpkgmdltags");
        checkExit(Exit.OK);
        checkModuleModeCommon();
        checkModuleModeApi(false);
        checkModuleModeAll(true);
    }

    /**
     * Test generated module summary page of a module with no exported package.
     */
    @Test
    public void testModuleSummaryNoExportedPkgAll() {
        javadoc("-d", "out-ModuleSummaryNoExportedPkgAll",
                "-use",
                "--show-module-contents=all",
                "-sourcepath", testSrc + "/moduleNoExport",
                "--module", "moduleNoExport",
                "testpkgmdlNoExport");
        checkExit(Exit.OK);
        checkModuleSummaryNoExported(true);
    }

    /**
     * Test generated module summary page of a module with no exported package.
     */
    @Test
    public void testModuleSummaryNoExportedPkgApi() {
        javadoc("-d", "out-ModuleSummaryNoExportedPkgApi",
                "-use",
                "-sourcepath", testSrc + "/moduleNoExport",
                "--module", "moduleNoExport",
                "testpkgmdlNoExport");
        checkExit(Exit.OK);
        checkModuleSummaryNoExported(false);
    }

    /**
     * Test generated module pages for javadoc run for a single module having a single package.
     */
    @Test
    public void testSingleModuleSinglePkg() {
        javadoc("-d", "out-singlemod",
                "--module-source-path", testSrc,
                "--module", "moduleC",
                "testpkgmdlC");
        checkExit(Exit.OK);
    }

    /**
     * Test generated module pages for javadoc run for a single module having multiple packages.
     */
    @Test
    public void testSingleModuleMultiplePkg() {
        javadoc("-d", "out-singlemodmultiplepkg",
                "--show-module-contents=all",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--add-modules", "moduleC",
                "--module", "moduleB",
                "testpkg2mdlB", "testpkgmdlB");
        checkExit(Exit.OK);
    }

    /**
     * Test -group option for modules. The overview-summary.html page should group the modules accordingly.
     */
    @Test
    public void testGroupOption() {
        javadoc("-d", "out-group",
                "--show-module-contents=all",
                "-Xdoclint:none",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "-group", "Module Group A", "moduleA*",
                "-group", "Module Group B & C", "moduleB*:moduleC*",
                "-group", "Java SE Modules", "java*",
                "--module", "moduleA,moduleB,moduleC,moduletags",
                "moduleA/concealedpkgmdlA", "testpkgmdlA", "testpkg2mdlB", "testpkgmdlB", "testpkgmdlC",
                "testpkgmdltags");
        checkExit(Exit.OK);
        checkGroupOption();
    }

    /**
     * Test -group option for modules and the ordering of module groups.
     * The overview-summary.html page should group the modules accordingly and display the group tabs in
     * the order it was provided on the command-line.
     */
    @Test
    public void testGroupOptionOrdering() {
        javadoc("-d", "out-groupOrder",
                "--show-module-contents=all",
                "-Xdoclint:none",
                "-tag", "regular:a:Regular Tag:",
                "-tag", "moduletag:s:Module Tag:",
                "--module-source-path", testSrc,
                "-group", "B Group", "moduleB*",
                "-group", "C Group", "moduleC*",
                "-group", "A Group", "moduleA*",
                "-group", "Java SE Modules", "java*",
                "--module", "moduleA,moduleB,moduleC,moduletags",
                "moduleA/concealedpkgmdlA", "testpkgmdlA", "testpkg2mdlB", "testpkgmdlB", "testpkgmdlC",
                "testpkgmdltags");
        checkExit(Exit.OK);
        checkGroupOptionOrdering();
    }

    /**
     * Test -group option for unnamed modules. The overview-summary.html page should group the packages accordingly.
     */
    @Test
    public void testUnnamedModuleGroupOption() {
        javadoc("-d", "out-groupnomodule",
                "-use",
                "-Xdoclint:none",
                "-overview", testSrc("overview.html"),
                "-sourcepath", testSrc,
                "-group", "Package Group 0", "testpkgnomodule",
                "-group", "Package Group 1", "testpkgnomodule1",
                "testpkgnomodule", "testpkgnomodule1");
        checkExit(Exit.OK);
        checkUnnamedModuleGroupOption();
    }

    /**
     * Test -group option for unnamed modules and the ordering of package groups.
     * The overview-summary.html page should group the packages accordingly and display the group tabs in
     * the order it was provided on the command-line.
     */
    @Test
    public void testGroupOptionPackageOrdering() {
        javadoc("-d", "out-groupPkgOrder",
                "-use",
                "-Xdoclint:none",
                "-overview", testSrc("overview.html"),
                "-sourcepath", testSrc,
                "-group", "Z Group", "testpkgnomodule",
                "-group", "A Group", "testpkgnomodule1",
                "testpkgnomodule", "testpkgnomodule1");
        checkExit(Exit.OK);
        checkGroupOptionPackageOrdering();
    }

    /**
     * Test -group option for a single module.
     */
    @Test
    public void testGroupOptionSingleModule() {
        javadoc("-d", "out-groupsinglemodule",
                "-use",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "-group", "Module Group B", "moduleB*",
                "--module", "moduleB",
                "testpkg2mdlB", "testpkgmdlB");
        checkExit(Exit.OK);
        checkGroupOptionSingleModule();
    }

    /**
     * Test -group option for a single module.
     */
    @Test
    public void testModuleName() {
        javadoc("-d", "out-modulename",
                "-use",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleB,test.moduleFullName",
                "testpkg2mdlB", "testpkgmdlB", "testpkgmdlfullname");
        checkExit(Exit.OK);
        checkModuleName(true);
    }

    /**
     * Test -linkoffline option.
     */
    @Test
    public void testLinkOffline() {
        String url = "https://docs.oracle.com/javase/9/docs/api/";
        javadoc("-d", "out-linkoffline",
                "-use",
                "--show-module-contents=all",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB",
                "-linkoffline", url, testSrc + "/jdk",
                "testpkgmdlA", "testpkgmdlB", "testpkg3mdlB");
        checkExit(Exit.OK);
        checkLinkOffline();
    }

    /**
     * Test -linksource option.
     */
    @Test
    public void testLinkSource() {
        javadoc("-d", "out-linksource",
                "-use",
                "-linksource",
                "--no-platform-links",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB");
        checkExit(Exit.OK);
        checkLinks();
        checkLinkSource(false);
    }

    /**
     * Test -linksource option combined with -private.
     */
    @Test
    public void testLinkSourcePrivate() {
        javadoc("-d", "out-linksource-private",
                "-use",
                "-private",
                "-linksource",
                "--no-platform-links",
                "-Xdoclint:none",
                "--module-source-path", testSrc,
                "--module", "moduleA,moduleB");
        checkExit(Exit.OK);
        checkLinks();
        checkLinkSource(true);
    }

    void checkDescription(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <!-- ============ MODULE DESCRIPTION =========== -->
                    <a name="module-description">
                    <!--   -->
                    </a>
                    <div class="block">This is a test description for the moduleA module with a Sear\
                    ch phrase <span id="searchphrase" class="search-tag-result">search phrase</span>\
                    .</div>""");
        checkOutput("moduleB/module-summary.html", found,
                """
                    <!-- ============ MODULE DESCRIPTION =========== -->
                    <a name="module-description">
                    <!--   -->
                    </a>
                    <div class="block">This is a test description for the moduleB module. Search wor\
                    d <span id="search_word" class="search-tag-result">search_word</span> with no de\
                    scription.</div>""");
        checkOutput("index.html", found,
                """
                    </script>
                    <div class="block">The overview summary page header.</div>
                    </div>
                    <div class="overview-summary">
                    <table summary="Module Summary table, listing modules, and an explanation">
                    <caption><span>Modules</span><span class="tab-end">&nbsp;</span></caption>""");
        checkOutput("index.html", false,
                """
                    </table>
                    </div>
                    <div class="block">The overview summary page header.</div>
                    </div>
                    <div class="overview-summary">
                    <table summary="Module Summary table, listing modules, and an explanation">
                    <caption><span>Modules</span><span class="tab-end">&nbsp;</span></caption>""");
    }

    void checkHtml5Description(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <section class="module-description" id="module-description">
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">This module is deprecated.</div>
                    </div>
                    <!-- ============ MODULE DESCRIPTION =========== -->
                    <div class="block">This is a test description for the moduleA module with a Sear\
                    ch phrase <span id="searchphrase" class="search-tag-result">search phrase</span>\
                    .</div>""");
        checkOutput("moduleB/module-summary.html", found,
                """
                    <section class="module-description" id="module-description">
                    <!-- ============ MODULE DESCRIPTION =========== -->
                    <div class="block">This is a test description for the moduleB module. Search wor\
                    d <span id="search_word" class="search-tag-result">search_word</span> with no de\
                    scription.</div>""");
        checkOutput("index.html", found,
                """
                    </nav>
                    </header>
                    <div class="flex-content">
                    <main role="main">
                    <div class="block">The overview summary page header.</div>
                    <div id="all-modules-table">
                    <div class="caption"><span>Modules</span></div>
                    <div class="summary-table two-column-summary">""");
        checkOutput("index.html", false,
                """
                    </div>
                    </main>
                    <main role="main">
                    <div class="block">The overview summary page header.</div>
                    </div>
                    <div id="all-modules-table">
                    <div class="caption"><span>Modules</span></div>
                    <div class="summary-table two-column-summary">""");
    }

    void checkHtml5NoDescription(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <div class="header">
                    <h1 title="Module moduleA" class="title">Module moduleA</h1>
                    </div>
                    <hr>
                    <div class="module-signature"><span class="annotations">@Deprecated(forRemoval=true)
                    </span>module <span class="element-name">moduleA</span></div>
                    <section class="summary">
                    <ul class="summary-list">
                    <li>
                    <section class="packages-summary" id="packages-summary">
                    <!-- ============ PACKAGES SUMMARY =========== -->""");
        checkOutput("moduleB/module-summary.html", found,
                """
                    <div class="header">
                    <h1 title="Module moduleB" class="title">Module moduleB</h1>
                    </div>
                    <hr>
                    <div class="module-signature"><span class="annotations"><a href="testpkgmdlB/Ann\
                    otationType.html" title="annotation interface in testpkgmdlB">@AnnotationType</a\
                    >(<a href="testpkgmdlB/AnnotationType.html#optional()">optional</a>="Module Anno\
                    tation",
                                    <a href="testpkgmdlB/AnnotationType.html#required()">required</a>=2016)
                    </span>module <span class="element-name">moduleB</span></div>
                    <section class="summary">
                    <ul class="summary-list">
                    <li>
                    <section class="packages-summary" id="packages-summary">
                    <!-- ============ PACKAGES SUMMARY =========== -->""");
    }

    void checkModuleLink() {
        checkOutput("index.html", true,
                "<li>Module</li>");
        checkOutput("moduleA/module-summary.html", true,
                """
                    <li class="nav-bar-cell1-rev">Module</li>""");
        checkOutput("moduleB/module-summary.html", true,
                """
                    <li class="nav-bar-cell1-rev">Module</li>""");
        checkOutput("moduleA/testpkgmdlA/class-use/TestClassInModuleA.html", true,
                """
                    <li><a href="../../module-summary.html">Module</a></li>""");
        checkOutput("moduleB/testpkgmdlB/package-summary.html", true,
                """
                    <li><a href="../module-summary.html">Module</a></li>""");
        checkOutput("moduleB/testpkgmdlB/TestClassInModuleB.html", true,
                """
                    <li><a href="../module-summary.html">Module</a></li>""");
        checkOutput("moduleB/testpkgmdlB/class-use/TestClassInModuleB.html", true,
                """
                    <li><a href="../../module-summary.html">Module</a></li>""");
    }

    void checkNoModuleLink() {
        checkOutput("testpkgnomodule/package-summary.html", true,
                """
                    <ul class="nav-list" title="Navigation">
                    <li><a href="../testpkgnomodule/package-summary.html">Package</a></li>""");
        checkOutput("testpkgnomodule/TestClassNoModule.html", true,
                """
                    <ul class="nav-list" title="Navigation">
                    <li><a href="../testpkgnomodule/package-summary.html">Package</a></li>""");
        checkOutput("testpkgnomodule/class-use/TestClassNoModule.html", true,
                """
                    <ul class="nav-list" title="Navigation">
                    <li><a href="../../testpkgnomodule/package-summary.html">Package</a></li>""");
    }

    void checkModuleTags() {
        checkOutput("moduletags/module-summary.html", true,
                """
                    Type Link: <a href="testpkgmdltags/TestClassInModuleTags.html" title="class in t\
                    estpkgmdltags"><code>TestClassInModuleTags</code></a>.""",
                """
                    Member Link: <a href="testpkgmdltags/TestClassInModuleTags.html#testMethod(java.\
                    lang.String)"><code>testMethod(String)</code></a>.""",
                """
                    Package Link: <a href="testpkgmdltags/package-summary.html"><code>testpkgmdltags</code></a>.""",
                """
                    </div>
                    <dl class="notes">""",
                """
                    <dt>Since:</dt>
                    <dd>JDK 9</dd>""",
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li>"Test see tag"</li>
                    <li><a href="testpkgmdltags/TestClassInModuleTags.html" title="class in testpkgmdlta\
                    gs"><code>TestClassInModuleTags</code></a></li>
                    </ul>
                    </dd>""",
                """
                    <dt>Regular Tag:</dt>
                    <dd>Just a regular simple tag.</dd>""",
                """
                    <dt>Module Tag:</dt>
                    <dd>Just a simple module tag.</dd>""",
                """
                    <dt>Version:</dt>
                    <dd>1.0</dd>""",
                """
                    <dt>Author:</dt>
                    <dd>Alice</dd>""");
        checkOutput("moduletags/testpkgmdltags/TestClassInModuleTags.html", false,
                """
                    <dt>Module Tag:</dt>
                    <dd>Just a simple module tag.</dd>""");
    }

    void checkHtml5OverviewSummaryModules() {
        checkOutput("index.html", true,
                """
                    <div id="all-modules-table">
                    <div class="caption"><span>Modules</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Module</div>
                    <div class="table-header col-last">Description</div>
                    """);
        checkOutput("overview-summary.html", false,
                """
                    <div id="all-modules-table">
                    <div class="caption"><span>Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    """);
    }

    void checkHtml5OverviewSummaryPackages() {
        checkOutput("index.html", false,
                """
                    <div class="overview-summary" id="all-modules-table">
                    <table class="summary-table">
                    <caption><span>Modules</span></caption>
                    <table-header>
                    <tr>
                    <th class="col-first" scope="col">Module</th>
                    <th class="col-last" scope="col">Description</th>
                    </tr>
                    </table-header>""",
                """
                    </table>
                    </div>
                    </main>
                    <main role="main">
                    <div class="block">The overview summary page header.</div>
                    </div>
                    <a id="Packages">
                    <!--   -->
                    </a>
                    <div class="overview-summary">
                    <table>
                    <caption><span>Packages</span></caption>""");
        checkOutput("index.html", true,
                """
                    <div id="all-packages-table">
                    <div class="caption"><span>Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    </nav>
                    </header>
                    <div class="flex-content">
                    <main role="main">
                    <div class="block">The overview summary page header.</div>
                    <div id="all-packages-table">
                    <div class="caption"><span>Packages</span></div>
                    <div class="summary-table two-column-summary">""");
    }

    void checkModuleSummary() {
        checkOutput("moduleA/module-summary.html", true,
                """
                    <ul class="sub-nav-list">
                    <li>Module:&nbsp;</li>
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li><a href="#modules-summary">Modules</a>&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li>Services</li>
                    </ul>""",
                """
                    <section class="modules-summary" id="modules-summary">
                    <!-- ============ MODULES SUMMARY =========== -->
                    <h2>Modules</h2>""",
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab1"><a href="testpkgmdlA/package-summary.html">testpkgmdlA</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab1">&nbsp;</div>""",
                """
                    <section class="packages-summary" id="packages-summary">
                    <!-- ============ PACKAGES SUMMARY =========== -->
                    <h2>Packages</h2>""",
                """
                    <div class="col-first even-row-color">transitive</div>
                    <div class="col-second even-row-color"><a href="../moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">This is a test description for the moduleB module.</div>
                    </div>
                    """);
        checkOutput("moduleB/module-summary.html", true,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li>Modules&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#services-summary">Services</a></li>""",
                """
                    <!-- ============ PACKAGES SUMMARY =========== -->
                    <h2>Packages</h2>""",
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab2"><a href="testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab2">&nbsp;</div>
                    </div>""",
                """
                    <!-- ============ PACKAGES SUMMARY =========== -->
                    <h2>Packages</h2>""",
                """
                    <!-- ============ SERVICES SUMMARY =========== -->
                    <h2>Services</h2>""",
                """
                    <div class="col-first even-row-color"><a href="testpkgmdlB/TestClassInModuleB.ht\
                    ml" title="class in testpkgmdlB">TestClassInModuleB</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">With a test description for uses.</div>
                    </div>""",
                """
                    <div class="caption"><span>Opens</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="caption"><span>Uses</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="caption"><span>Provides</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>""");
    }

    void checkAggregatorModuleSummary() {
        checkOutput("moduleT/module-summary.html", true,
                """
                    <div class="header">
                    <h1 title="Module moduleT" class="title">Module moduleT</h1>
                    </div>""",
                """
                    <div class="block">This is a test description for the moduleT module. Search phr\
                    ase <span id="searchphrase" class="search-tag-result">search phrase</span>. Make\
                     sure there are no exported packages.</div>""",
                """
                    <div class="col-first even-row-color">transitive</div>
                    <div class="col-second even-row-color"><a href="../moduleA/module-summary.html">moduleA</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">This is a test description for the moduleA module with a Search phrase search phrase.</div>
                    </div>
                    <div class="col-first odd-row-color">transitive</div>
                    <div class="col-second odd-row-color"><a href="../moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last odd-row-color">
                    <div class="block">This is a test description for the moduleB module.</div>
                    </div>""");
    }

    void checkNegatedModuleSummary() {
        checkOutput("moduleA/module-summary.html", false,
                """
                    <!-- ============ SERVICES SUMMARY =========== -->
                    <h2>Services</h2>""");
    }

    void checkModuleFilesAndLinks(boolean found) {
        checkFileAndOutput("moduleA/testpkgmdlA/package-summary.html", found,
                """
                    <li><a href="../module-summary.html">Module</a></li>""",
                """
                    <div class="sub-title"><span class="module-label-in-package">Module</span>&nbsp;\
                    <a href="../module-summary.html">moduleA</a></div>""");
        checkFileAndOutput("moduleA/testpkgmdlA/TestClassInModuleA.html", found,
                """
                    <li><a href="../module-summary.html">Module</a></li>""",
                """
                    <div class="sub-title"><span class="module-label-in-type">Module</span>&nbsp;<a \
                    href="../module-summary.html">moduleA</a></div>""");
        checkFileAndOutput("moduleB/testpkgmdlB/AnnotationType.html", found,
                """
                    <div class="sub-title"><span class="module-label-in-type">Module</span>&nbsp;<a \
                    href="../module-summary.html">moduleB</a></div>""",
                """
                    <div class="sub-title"><span class="package-label-in-type">Package</span>&nbsp;<\
                    a href="package-summary.html">testpkgmdlB</a></div>""");
        checkFiles(found,
                "moduleA/module-summary.html");
    }

    void checkModulesInSearch(boolean found) {
        checkOutput("index-all.html", found,
                """
                    <dl class="index">
                    <dt><a href="moduleA/module-summary.html">moduleA</a> - module moduleA</dt>
                    <dd>
                    <div class="block">This is a test description for the moduleA module with a Search phrase search phrase.</div>
                    </dd>
                    <dt><a href="moduleB/module-summary.html">moduleB</a> - module moduleB</dt>
                    <dd>
                    <div class="block">This is a test description for the moduleB module.</div>
                    </dd>
                    </dl>""",
                """
                    <dl class="index">
                    <dt><a href="moduleB/module-summary.html#search_word" class="search-tag-link">se\
                    arch_word</a> - Search tag in module moduleB</dt>
                    <dd>&nbsp;</dd>
                    <dt><a href="moduleA/module-summary.html#searchphrase" class="search-tag-link">s\
                    earch phrase</a> - Search tag in module moduleA</dt>
                    <dd>with description</dd>
                    </dl>""");
        checkOutput("index-all.html", false,
                """
                    <dt><a href="moduleA/module-summary.html#searchphrase" class="search-tag-link">s\
                    earch phrase</a> - Search tag in module moduleA</dt>
                    <dd>with description</dd>
                    <dt><a href="moduleA/module-summary.html#searchphrase" class="search-tag-link">s\
                    earch phrase</a></span> - Search tag in module moduleA</dt>
                    <dd>with description</dd>""");
    }

    void checkModuleModeCommon() {
        checkOutput("index.html", true,
                """
                    <div class="col-first even-row-color all-modules-table all-modules-table-tab1"><\
                    a href="moduleA/module-summary.html">moduleA</a></div>
                    <div class="col-last even-row-color all-modules-table all-modules-table-tab1">
                    <div class="block">This is a test description for the moduleA module with a Search phrase search phrase.</div>""",
                """
                    <div class="col-first odd-row-color all-modules-table all-modules-table-tab1"><a\
                     href="moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last odd-row-color all-modules-table all-modules-table-tab1">
                    <div class="block">This is a test description for the moduleB module.</div>""",
                """
                    <div class="col-first odd-row-color all-modules-table all-modules-table-tab1"><a\
                     href="moduletags/module-summary.html">moduletags</a></div>
                    <div class="col-last odd-row-color all-modules-table all-modules-table-tab1">
                    <div class="block">This is a test description for the moduletags module.<br>
                     Type Link: <a href="moduletags/testpkgmdltags/TestClassInModuleTags.html" title\
                    ="class in testpkgmdltags"><code>TestClassInModuleTags</code></a>.<br>
                     Member Link: <a href="moduletags/testpkgmdltags/TestClassInModuleTags.html#test\
                    Method(java.lang.String)"><code>testMethod(String)</code></a>.<br>
                     Package Link: <a href="moduletags/testpkgmdltags/package-summary.html"><code>testpkgmdltags</code></a>.<br></div>
                    </div>""");
        checkOutput("moduleA/module-summary.html", true,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li><a href="#modules-summary">Modules</a>&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li>Services</li>""",
                """
                    <div class="col-first even-row-color"><a href="../moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last even-row-color"><a href="../moduleB/testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    """);
        checkOutput("moduletags/module-summary.html", true,
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab1"><a href="testpkgmdltags/package-summary.html">testpkgmdltags</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab1">&nbsp;</div>""",
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li><a href="#modules-summary">Modules</a>&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li>Services</li>""",
                """
                    <div class="caption"><span>Indirect Requires</span></div>
                    <div class="details-table three-column-summary">""",
                """
                    <div class="col-first even-row-color">transitive</div>
                    <div class="col-second even-row-color"><a href="../moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">This is a test description for the moduleB module.</div>
                    </div>""",
                """
                    <div class="caption"><span>Indirect Exports</span></div>
                    <div class="details-table two-column-summary">""",
                """
                    <div class="caption"><span>Requires</span></div>
                    <div class="details-table three-column-summary">
                    <div class="table-header col-first">Modifier</div>
                    <div class="table-header col-second">Module</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="caption"><span>Indirect Requires</span></div>
                    <div class="details-table three-column-summary">
                    <div class="table-header col-first">Modifier</div>
                    <div class="table-header col-second">Module</div>
                    <div class="table-header col-last">Description</div>""",
                """
                    <div class="caption"><span>Indirect Opens</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">From</div>
                    <div class="table-header col-last">Packages</div>""",
                """
                    <div class="col-first even-row-color"><a href="../moduleB/module-summary.html">moduleB</a></div>
                    <div class="col-last even-row-color"><a href="../moduleB/testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    """);
    }

    void checkModuleModeApi(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab1"><a href="testpkgmdlA/package-summary.html">testpkgmdlA</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab1">&nbsp;</div>""");
        checkOutput("moduleB/module-summary.html", found,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li>Modules&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#services-summary">Services</a></li>""",
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab2"><a href="testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab2">&nbsp;</div>""",
                """
                    <div id="package-summary-table">
                    <div class="caption"><span>Opens</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab2"><a href="testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab2">&nbsp;</div>
                    </div>
                    </div>""",
                """
                    <div class="col-first even-row-color"><a href="testpkgmdlB/TestClassInModuleB.ht\
                    ml" title="class in testpkgmdlB">TestClassInModuleB</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">With a test description for uses.</div>
                    """);
        checkOutput("moduletags/module-summary.html", found,
                """
                    <div class="col-first even-row-color">transitive static</div>
                    <div class="col-second even-row-color"><a href="../moduleA/module-summary.html">moduleA</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">This is a test description for the moduleA module with a Search phrase search phrase.</div>""");
    }

    void checkModuleModeAll(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <div class="col-first even-row-color"> </div>
                    <div class="col-second even-row-color">java.base</div>
                    <div class="col-last even-row-color">&nbsp;</div>""",
                """
                    <div class="col-first even-row-color"> </div>
                    <div class="col-second even-row-color"><a href="../moduleC/module-summary.html">moduleC</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">This is a test description for the moduleC module.</div>
                    </div>""",
                """
                    <div class="col-first even-row-color"><a href="../moduleC/module-summary.html">moduleC</a></div>
                    <div class="col-last even-row-color"><a href="../moduleC/testpkgmdlC/package-summary.html">testpkgmdlC</a></div>""",
                """
                    <div class="col-first odd-row-color package-summary-table package-summary-table-\
                    tab1"><a href="testpkgmdlA/package-summary.html">testpkgmdlA</a></div>
                    <div class="col-second odd-row-color package-summary-table package-summary-table-tab1">All Modules</div>
                    <div class="col-last odd-row-color package-summary-table package-summary-table-tab1">&nbsp;</div>""",
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="package-summary-table-tab0" role="tab" aria-selected="true" aria-con\
                    trols="package-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)"\
                     onclick="show('package-summary-table', 'package-summary-table', 3)" class="acti\
                    ve-table-tab">All Packages</button>\
                    <button id="package-summary-table-tab1" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab1', 3)" clas\
                    s="table-tab">Exports</button>\
                    <button id="package-summary-table-tab3" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab3', 3)" clas\
                    s="table-tab">Concealed</button>\
                    </div>""",
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab3"><a href="concealedpkgmdlA/package-summary.html">concealedpkgmdlA</a></div\
                    >
                    <div class="col-second even-row-color package-summary-table package-summary-table-tab3">None</div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab3">&nbsp;</div>""");
        checkOutput("moduleB/module-summary.html", found,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li><a href="#modules-summary">Modules</a>&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#services-summary">Services</a></li>""",
                """
                    <div class="col-first even-row-color package-summary-table package-summary-table\
                    -tab2"><a href="testpkgmdlB/package-summary.html">testpkgmdlB</a></div>
                    <div class="col-second even-row-color package-summary-table package-summary-table-tab2">None</div>
                    <div class="col-second even-row-color package-summary-table package-summary-table-tab2">All Modules</div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab2">&nbsp;</div>""",
                """
                    <div class="col-first even-row-color"> </div>
                    <div class="col-second even-row-color">java.base</div>
                    <div class="col-last even-row-color">&nbsp;</div>""",
                """
                    <div class="col-first even-row-color"><a href="testpkgmdlB/TestClass2InModuleB.html"\
                     title="class in testpkgmdlB">TestClass2InModuleB</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>""",
                """
                    <div class="col-first even-row-color"><a href="testpkg2mdlB/TestInterface2InModuleB.h\
                    tml" title="interface in testpkg2mdlB">TestInterface2InModuleB</a></div>
                    <div class="col-last even-row-color">&nbsp;<br>(<span class="implementation-label">Im\
                    plementation(s):</span>&nbsp;<a href="testpkgmdlB/TestClass2InModuleB.html" titl\
                    e="class in testpkgmdlB">TestClass2InModuleB</a>)</div>""",
                """
                    <div class="col-first odd-row-color"><a href="testpkg2mdlB/TestInterfaceInModuleB.ht\
                    ml" title="interface in testpkg2mdlB">TestInterfaceInModuleB</a></div>
                    <div class="col-last odd-row-color">&nbsp;<br>(<span class="implementation-label">Im\
                    plementation(s):</span>&nbsp;<a href="testpkgmdlB/TestClassInModuleB.html" title\
                    ="class in testpkgmdlB">TestClassInModuleB</a>)</div>""",
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="package-summary-table-tab0" role="tab" aria-selected="true" aria-con\
                    trols="package-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)"\
                     onclick="show('package-summary-table', 'package-summary-table', 4)" class="acti\
                    ve-table-tab">All Packages</button>\
                    <button id="package-summary-table-tab1" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab1', 4)" clas\
                    s="table-tab">Exports</button>\
                    <button id="package-summary-table-tab2" role="tab" aria-selected="false" aria-co\
                    ntrols="package-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event\
                    )" onclick="show('package-summary-table', 'package-summary-table-tab2', 4)" clas\
                    s="table-tab">Opens</button>\
                    </div>""",
                """
                    <div class="col-first odd-row-color"><a href="testpkgmdlB/TestClassInModuleB.htm\
                    l" title="class in testpkgmdlB">TestClassInModuleB</a></div>
                    <div class="col-last odd-row-color">
                    <div class="block">With a test description for uses.</div>
                    """);
        checkOutput("moduleC/module-summary.html", found,
                """
                    <div class="caption"><span>Exports</span></div>
                    <div class="summary-table three-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-second">Exported To Modules</div>
                    <div class="table-header col-last">Description</div>""");
        checkOutput("moduletags/module-summary.html", found,
                """
                    <div class="col-first odd-row-color">transitive static</div>
                    <div class="col-second odd-row-color"><a href="../moduleA/module-summary.html">moduleA</a></div>
                    <div class="col-last odd-row-color">
                    <div class="block">This is a test description for the moduleA module with a Search phrase search phrase.</div>""");
    }

    void checkModuleDeprecation(boolean found) {
        checkOutput("moduleA/module-summary.html", found,
                """
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated, for re\
                    moval: This API element is subject to removal in a future version.</span>
                    <div class="deprecation-comment">This module is deprecated.</div>
                    </div>""");
        checkOutput("deprecated-list.html", found,
                """
                    <ul>
                    <li><a href="#for-removal">Terminally Deprecated</a></li>
                    <li><a href="#module">Modules</a></li>
                    </ul>""",
                """
                    <div class="col-summary-item-name even-row-color"><a href="moduleA/module-summary.html">moduleA</a></div>
                    <div class="col-last even-row-color">
                    <div class="deprecation-comment">This module is deprecated.</div>""");
        checkOutput("moduleB/module-summary.html", !found,
                """
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">This module is deprecated using just the javadoc tag.</div>
                    """);
        checkOutput("moduletags/module-summary.html", found,
                """
                    <div class="header">
                    <h1 title="Module moduletags" class="title">Module moduletags</h1>
                    </div>
                    <hr>
                    <div class="module-signature"><span class="annotations">@Deprecated
                    </span>module <span class="element-name">moduletags</span></div>""",
                """
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span></div>""");
    }

    void checkModuleAnnotation() {
        checkOutput("moduleB/module-summary.html", true,
                """
                    <div class="header">
                    <h1 title="Module moduleB" class="title">Module moduleB</h1>
                    </div>
                    <hr>
                    <div class="module-signature"><span class="annotations"><a href="testpkgmdlB/Ann\
                    otationType.html" title="annotation interface in testpkgmdlB">@AnnotationType</a\
                    >(<a href="testpkgmdlB/AnnotationType.html#optional()">optional</a>="Module Anno\
                    tation",
                                    <a href="testpkgmdlB/AnnotationType.html#required()">required</a>=2016)
                    </span>module <span class="element-name">moduleB</span></div>""");
        checkOutput("moduleB/module-summary.html", false,
                "@AnnotationTypeUndocumented");
    }

    void checkModuleSummaryNoExported(boolean found) {
        checkOutput("moduleNoExport/module-summary.html", found,
                """
                    <!-- ============ PACKAGES SUMMARY =========== -->
                    <h2>Packages</h2>""",
                """
                    <div class="caption"><span>Concealed</span></div>""");
    }

    void checkGroupOption() {
        checkOutput("index.html", true,
                """
                    <div id="all-modules-table">
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="all-modules-table-tab0" role="tab" aria-selected="true" aria-control\
                    s="all-modules-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick\
                    ="show('all-modules-table', 'all-modules-table', 2)" class="active-table-tab">Al\
                    l Modules</button>\
                    <button id="all-modules-table-tab1" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab1', 2)" class="table-tab">Mo\
                    dule Group A</button>\
                    <button id="all-modules-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab2', 2)" class="table-tab">Mo\
                    dule Group B &amp; C</button>\
                    <button id="all-modules-table-tab3" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab3', 2)" class="table-tab">Ot\
                    her Modules</button>\
                    </div>
                    <div id="all-modules-table.tabpanel" role="tabpanel">
                    <div class="summary-table two-column-summary" aria-labelledby="all-modules-table-tab0">""",
                """
                    var evenRowColor = "even-row-color";
                    var oddRowColor = "odd-row-color";
                    var tableTab = "table-tab";
                    var activeTableTab = "active-table-tab";""");
        checkOutput("index.html", false,
                """
                    <div class="overview-summary">
                    <table>
                    <caption><span>Modules</span></caption>""",
                "Java SE Modules");
    }

    void checkGroupOptionOrdering() {
        checkOutput("index.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="all-modules-table-tab0" role="tab" aria-selected="true" aria-control\
                    s="all-modules-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick\
                    ="show('all-modules-table', 'all-modules-table', 2)" class="active-table-tab">Al\
                    l Modules</button>\
                    <button id="all-modules-table-tab1" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab1', 2)" class="table-tab">B\
                     Group</button>\
                    <button id="all-modules-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab2', 2)" class="table-tab">C\
                     Group</button>\
                    <button id="all-modules-table-tab3" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab3', 2)" class="table-tab">A\
                     Group</button>\
                    <button id="all-modules-table-tab4" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab4', 2)" class="table-tab">Ot\
                    her Modules</button>\
                    </div>""");
        checkOutput("index.html", false,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="all-modules-table-tab0" role="tab" aria-selected="true" aria-control\
                    s="all-modules-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick\
                    ="show('all-modules-table', 'all-modules-table', 2)" class="active-table-tab">Al\
                    l Modules</button>\
                    <button id="all-modules-table-tab1" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab1', 2)" class="table-tab">A\
                     Group</button>\
                    <button id="all-modules-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab2', 2)" class="table-tab">B\
                     Group</button>\
                    <button id="all-modules-table-tab3" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab3', 2)" class="table-tab">C\
                     Group</button>\
                    <button id="all-modules-table-tab4" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab4', 2)" class="table-tab">Ot\
                    her Modules</button>\
                    </div>""",
                "Java SE Modules");
    }

    void checkUnnamedModuleGroupOption() {
        checkOutput("index.html", true,
                """
                    <div class="block">The overview summary page header.</div>
                    <div id="all-packages-table">
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="all-packages-table-tab0" role="tab" aria-selected="true" aria-contro\
                    ls="all-packages-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" oncli\
                    ck="show('all-packages-table', 'all-packages-table', 2)" class="active-table-tab\
                    ">All Packages</button>\
                    <button id="all-packages-table-tab1" role="tab" aria-selected="false" aria-contro\
                    ls="all-packages-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-packages-table', 'all-packages-table-tab1', 2)" class="table-tab">P\
                    ackage Group 0</button>\
                    <button id="all-packages-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-packages-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-packages-table', 'all-packages-table-tab2', 2)" class="table-tab">P\
                    ackage Group 1</button>\
                    </div>
                    <div id="all-packages-table.tabpanel" role="tabpanel">
                    <div class="summary-table two-column-summary" aria-labelledby="all-packages-table-tab0">""",
                """
                    var evenRowColor = "even-row-color";
                    var oddRowColor = "odd-row-color";
                    var tableTab = "table-tab";
                    var activeTableTab = "active-table-tab";""");
    }

    void checkGroupOptionPackageOrdering() {
        checkOutput("index.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="all-packages-table-tab0" role="tab" aria-selected="true" aria-contro\
                    ls="all-packages-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" oncli\
                    ck="show('all-packages-table', 'all-packages-table', 2)" class="active-table-tab\
                    ">All Packages</button>\
                    <button id="all-packages-table-tab1" role="tab" aria-selected="false" aria-contr\
                    ols="all-packages-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onc\
                    lick="show('all-packages-table', 'all-packages-table-tab1', 2)" class="table-tab\
                    ">Z Group</button>\
                    <button id="all-packages-table-tab2" role="tab" aria-selected="false" aria-contr\
                    ols="all-packages-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onc\
                    lick="show('all-packages-table', 'all-packages-table-tab2', 2)" class="table-tab\
                    ">A Group</button>\
                    </div>""");
    }

    void checkGroupOptionSingleModule() {
        checkOutput("index.html", true,
                "window.location.replace('moduleB/module-summary.html')");
    }

    void checkModuleName(boolean found) {
        checkOutput("test.moduleFullName/module-summary.html", found,
                """
                    <div class="header">
                    <h1 title="Module test.moduleFullName" class="title">Module test.moduleFullName</h1>
                    </div>""");
        checkOutput("index-all.html", found,
                """
                    <h2 class="title" id="I:T">T</h2>
                    <dl class="index">
                    <dt><a href="test.moduleFullName/module-summary.html">test.moduleFullName</a> - module test.moduleFullName</dt>
                    <dd>
                    <div class="block">This is a test description for the test.moduleFullName.</div>
                    </dd>""");
        checkOutput("test.moduleFullName/module-summary.html", !found,
                """
                    <div class="header">
                    <h1 title="Module moduleFullName" class="title">Module moduleFullName</h1>
                    </div>""");
        checkOutput("index-all.html", !found,
                """
                    <dl class="index">
                    <dt><a href="test.moduleFullName/module-summary.html">moduleFullName</a> - module moduleFullName</dt>
                    <dd>
                    <div class="block">This is a test description for the test.moduleFullName.</div>
                    </dd>
                    </dl>""");
    }

    void checkLinkOffline() {
        checkOutput("moduleB/testpkg3mdlB/package-summary.html", true,
                """
                    <a href="https://docs.oracle.com/javase/9/docs/api/java.base/java/lang/String.ht\
                    ml" title="class or interface in java.lang" class="external-link"><code>Link to \
                    String Class</code></a>""");
        checkOutput("moduleB/testpkg3mdlB/package-summary.html", true,
                """
                    <a href="https://docs.oracle.com/javase/9/docs/api/java.base/java/lang/package-s\
                    ummary.html" class="external-link"><code>Link to java.lang package</code></a>""");
        checkOutput("moduleB/testpkg3mdlB/package-summary.html", true,
                """
                    <a href="https://docs.oracle.com/javase/9/docs/api/java.base/module-summary.html\
                    " class="external-link"><code>Link to java.base module</code></a>""");
    }

    void checkLinkSource(boolean includePrivate) {
        checkOutput("moduleA/module-summary.html", !includePrivate,
                """
                    div id="package-summary-table">
                    <div class="caption"><span>Exports</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color package-summary-table package-summary-table-tab1">\
                    <a href="testpkgmdlA/package-summary.html">testpkgmdlA</a></div>
                    <div class="col-last even-row-color package-summary-table package-summary-table-tab1">&nbsp;</div>
                    </div>
                    </div>
                    """);
        checkOutput("moduleA/testpkgmdlA/TestClassInModuleA.html", true,
                """
                    <section class="class-description" id="class-description">
                    <hr>
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name"><a href="../../src-html/moduleA/testpkgmdlA/TestClassInModule\
                    A.html#line-25">TestClassInModuleA</a></span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    </section>""");
        checkOutput("src-html/moduleA/testpkgmdlA/TestClassInModuleA.html", true,
                """
                    <span class="source-line-no">019</span><span id="line-19"> * Please contact Orac\
                    le, 500 Oracle Parkway, Redwood Shores, CA 94065 USA</span>
                    <span class="source-line-no">020</span><span id="line-20"> * or visit www.oracle\
                    .com if you need additional information or have any</span>
                    <span class="source-line-no">021</span><span id="line-21"> * questions.</span>
                    <span class="source-line-no">022</span><span id="line-22"> */</span>
                    <span class="source-line-no">023</span><span id="line-23">package testpkgmdlA;</span>
                    <span class="source-line-no">024</span><span id="line-24"></span>
                    <span class="source-line-no">025</span><span id="line-25">public class TestClassInModuleA {</span>
                    <span class="source-line-no">026</span><span id="line-26">}</span>""");
        if (includePrivate) {
            checkOutput("src-html/moduleA/concealedpkgmdlA/ConcealedClassInModuleA.html", true,
                    """
                        <span class="source-line-no">024</span><span id="line-24">package concealedpkgmdlA;</span>
                        <span class="source-line-no">025</span><span id="line-25"></span>
                        <span class="source-line-no">026</span><span id="line-26">public class ConcealedClassInModuleA {</span>
                        <span class="source-line-no">027</span><span id="line-27">    public void testMethodConcealedClass() { }</span>
                        <span class="source-line-no">028</span><span id="line-28">}</span>""");
        }
    }

    void checkAllPkgsAllClasses(boolean found) {
        checkOutput("allclasses-index.html", true,
                """
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal"><button id=\
                    "all-classes-table-tab0" role="tab" aria-selected="true" aria-controls="all-clas\
                    ses-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" onclick="show('all\
                    -classes-table', 'all-classes-table', 2)" class="active-table-tab">All Classes a\
                    nd Interfaces</button>\
                    <button id="all-classes-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-classes-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-classes-table', 'all-classes-table-tab2', 2)" class="table-tab">Cl\
                    asses</button>\
                    <button id="all-classes-table-tab7" role="tab" aria-selected="false" aria-contro\
                    ls="all-classes-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-classes-table', 'all-classes-table-tab7', 2)" class="table-tab">An\
                    notation Interfaces</button></div>
                    """,
                """
                    <div class="table-header col-first">Class</div>
                    <div class="table-header col-last">Description</div>
                    """);
        checkOutput("allpackages-index.html", true,
                """
                    <div class="caption"><span>Package Summary</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    """);
        checkOutput("allclasses-index.html", found,
                """
                    <div class="summary-table two-column-summary" aria-labelledby="all-classes-table-tab0">
                    """);
        checkOutput("allpackages-index.html", found,
                """
                    <div class="caption"><span>Package Summary</span></div>
                    <div class="summary-table two-column-summary">
                    """);
        checkOutput("allclasses-index.html", !found,
                """
                    <table summary="Class Summary table, listing classes, and an explanation" aria-labelledby="t0">""");
        checkOutput("allpackages-index.html", !found,
                """
                    <div class="packages-summary">
                    <table summary="Package Summary table, listing packages, and an explanation">""");
        checkOutput("type-search-index.js", true,
                """
                    {"l":"All Classes and Interfaces","u":"allclasses-index.html"}""");
        checkOutput("package-search-index.js", true,
                """
                    {"l":"All Packages","u":"allpackages-index.html"}""");
        checkOutput("index-all.html", true,
                """
                    <br><a href="allclasses-index.html">All&nbsp;Classes&nbsp;and&nbsp;Interfaces</a\
                    ><span class="vertical-separator">|</span><a href="allpackages-index.html">All&n\
                    bsp;Packages</a>""");
    }
}
