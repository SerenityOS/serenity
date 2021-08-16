/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4275630 4749453 4625400 4753048 4415270 8074521 8182765 8196202
 * @summary  Generated HTML is invalid with frames.
 *           Displays unnecessary horizontal scroll bars.
 *           Missing whitespace in DOCTYPE declaration
 *           HTML table tags inserted in wrong place in pakcage use page
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main ValidHtml
 */

import javadoc.tester.JavadocTester;

public class ValidHtml extends JavadocTester {

    public static void main(String... args) throws Exception {
        ValidHtml tester = new ValidHtml();
        tester.runTests();
    }

    @Test
    public void test() {
        // Test for all cases except the split index page
        javadoc("-d", "out",
                    "-doctitle", "Document Title",
                    "-windowtitle", "Window Title",
                    "-use",
                    "-overview", testSrc("overview.html"),
                    "-sourcepath", testSrc,
                    "p1", "p2");
        checkExit(Exit.OK);
        String HTML5 = "<!DOCTYPE HTML>";
        checkValidHTML(HTML5);
    }

    void checkValidHTML(String doctype) {
        // Test the proper DOCTYPE element are present:
        checkOutput("index.html", true, doctype);
        checkOutput("overview-summary.html", true, doctype);
        checkOutput("p1/package-summary.html", true, doctype);
        checkOutput("p1/C.html", true, doctype);
    }
}
