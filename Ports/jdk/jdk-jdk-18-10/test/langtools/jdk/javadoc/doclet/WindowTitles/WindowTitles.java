/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4530730 8196202
 * @summary stddoclet: With frames off, window titles have "()" appended
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main WindowTitles
 */

/**
 * Runs javadoc and runs regression tests on the resulting HTML.
 */
import javadoc.tester.JavadocTester;

public class WindowTitles extends JavadocTester {

    public static void main(String... args) throws Exception {
        WindowTitles tester = new WindowTitles();
        tester.runTests();
    }

    @Test
    public void test() {
        // Test for all cases except the split index page
        javadoc("-d", "out-1",
                "-use",
                "-sourcepath", testSrc,
                "p1", "p2");
        checkExit(Exit.OK);
        checkFiles(false, "allclasses-noframe.html");

        checkTitle("index.html",                "Overview");
        checkTitle("overview-tree.html",        "Class Hierarchy");
        checkTitle("p1/package-summary.html",   "p1");
        checkTitle("p1/package-tree.html",      "p1 Class Hierarchy");
        checkTitle("p1/package-use.html",       "Uses of Package p1");
        checkTitle("p1/C1.html",                "C1");
        checkTitle("constant-values.html",      "Constant Field Values");
        checkTitle("deprecated-list.html",      "Deprecated List");
        checkTitle("serialized-form.html",      "Serialized Form");
        checkTitle("help-doc.html",             "API Help");
        checkTitle("index-all.html",            "Index");
        checkTitle("p1/class-use/C1.html",      "Uses of Class p1.C1");
    }

    @Test
    public void test2() {
        // Test only for the split-index case (and run on only one package)
        javadoc("-d", "out-2",
                "-splitindex",
                "-sourcepath", testSrc,
                "p1");
        checkExit(Exit.OK);

        checkTitle("index-files/index-1.html", "C-Index");
    }

    void checkTitle(String file, String title) {
        checkOutput(file, true, "<title>" + title + "</title>");
    }

}
