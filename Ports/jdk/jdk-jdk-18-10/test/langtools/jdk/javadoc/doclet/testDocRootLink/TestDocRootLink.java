/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6553182 8025416 8029504
 * @summary This test verifies the -Xdocrootparent option.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestDocRootLink
 */
import javadoc.tester.JavadocTester;

public class TestDocRootLink extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocRootLink tester = new TestDocRootLink();

        // The test files intentionally contain examples of links that should
        // or should not be affected by the -Xdocrootparent option, and the
        // results are checked explicitly; so, disable the automatic link
        // checker to prevent spurious "missing files" errors from some of
        // these links.
        tester.setAutomaticCheckLinks(false);

        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg1/C1.html", true,
            """
                Refer <a href="../../technotes/guides/index.html">Here</a>""",
            """
                This <a href="../pkg2/C2.html">Here</a> should not be replaced
                 with an absolute link.""",
            """
                Testing <a href="../technotes/guides/index.html">Link 1</a> and
                 <a href="../pkg2/C2.html">Link 2</a>.""");

        checkOutput("pkg1/package-summary.html", true,
            """
                <a href="../../technotes/guides/index.html">
                            Test document 1</a>""",
            """
                <a href="../pkg2/C2.html">
                            Another Test document 1</a>""",
            """
                <a href="../technotes/guides/index.html">
                            Another Test document 2.</a>""");

        // TODO: should this check *any* reference to http://download.oracle.com/
        checkOutput("pkg1/C1.html", false,
            """
                <a href="http://download.oracle.com/javase/7/docs/technotes/guides/index.html">""",
            """
                <a href="http://download.oracle.com/javase/7/docs/pkg2/C2.html">""");

        checkOutput("pkg1/package-summary.html", false,
            """
                <a href="http://download.oracle.com/javase/7/docs/technotes/guides/index.html">""",
            """
                <a href="http://download.oracle.com/javase/7/docs/pkg2/C2.html">""");
    }

    @Test
    public void test2() {
        javadoc("-d", "out-2",
                "-Xdocrootparent", "http://download.oracle.com/javase/7/docs",
                "-sourcepath", testSrc,
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg2/C2.html", true,
            """
                Refer <a href="http://download.oracle.com/javase/7/docs/technotes/guides/index.html">Here</a>""",
            """
                This <a href="../pkg1/C1.html">Here</a> should not be replaced
                 with an absolute link.""",
            """
                Testing <a href="../technotes/guides/index.html">Link 1</a> and
                 <a href="../pkg1/C1.html">Link 2</a>.""");

        checkOutput("pkg2/package-summary.html", true,
            """
                <a href="http://download.oracle.com/javase/7/docs/technotes/guides/index.html">
                            Test document 1</a>""",
            """
                <a href="../pkg1/C1.html">
                            Another Test document 1</a>""",
            """
                <a href="../technotes/guides/index.html">
                            Another Test document 2.</a>""");

        checkOutput("pkg2/C2.html", false,
            """
                <a href="../../technotes/guides/index.html">""",
            """
                <a href="http://download.oracle.com/javase/7/docs/pkg1/C1.html">""");

        checkOutput("pkg2/package-summary.html", false,
            """
                <a href="../../technotes/guides/index.html">""",
            """
                <a href="http://download.oracle.com/javase/7/docs/pkg1/C1.html">""");
    }
}
