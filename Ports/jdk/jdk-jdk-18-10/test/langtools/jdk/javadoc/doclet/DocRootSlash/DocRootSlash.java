/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4524350 4662945 4633447 8196202 8261976
 * @summary stddoclet: {@docRoot} inserts an extra trailing "/"
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main DocRootSlash
 */

import java.util.regex.*;

/**
 * Runs javadoc and runs regression tests on the resulting HTML.
 * It reads each file, complete with newlines, into a string to easily
 * find strings that contain newlines.
 */
import javadoc.tester.JavadocTester;

public class DocRootSlash extends JavadocTester {

    public static void main(String... args) throws Exception {
        DocRootSlash tester = new DocRootSlash();
        tester.runTests();
    }

    @Test
    public void test() {
        // Directory that contains source files that javadoc runs on
        String srcdir = System.getProperty("test.src", ".");


        javadoc("-d", "out",
                "-Xdoclint:none",
                "-overview", (srcdir + "/overview.html"),
                "-header", """
                    <A HREF="{@docroot}/element-list">{&#064;docroot}</A> <A HREF="{@docRoot}/help-doc.html">{&#064;docRoot}</A>""",
                "-sourcepath", srcdir,
                "p1", "p2");

        checkFiles(
                "p1/C1.html",
                "p1/package-summary.html",
                "index.html");
    }

    void checkFiles(String... filenameArray) {
        int count = 0;

        for (String f : filenameArray) {
            // Read contents of file into a string
            String fileString = readFile(f);
            System.out.println("\nSub-tests for file: " + f + " --------------");
            // Loop over all tests in a single file
            for ( int j = 0; j < 7; j++ ) {

                // Compare actual to expected string for a single subtest
                compareActualToExpected(++count, fileString);
            }
        }
    }

    /**
     * Regular expression pattern matching code
     *
     * Prefix Pattern:
     * flag   (?i)            (case insensitive, so "a href" == "A HREF" and all combinations)
     * group1 (
     *          <a or <A
     *          \\s+          (one or more whitespace characters)
     *          href or HREF
     *          \"            (double quote)
     *        )
     * group2 ([^\"]*)        (link reference -- characters that don't include a quote)
     * group3 (\".*?>)        (" target="frameName">)
     * group4 (.*?)           (label - zero or more characters)
     * group5 (</a>)          (end tag)
     */
    private static final String prefix = "(?i)(<a\\s+href=";    // <a href=     (start group1)
    private static final String ref1   = "\")([^\"]*)(\".*?>)"; // doublequotes (end group1, group2, group3)

    /**
     * Compares the actual string to the expected string in the specified string
     * @param str   String to search through
     */
    void compareActualToExpected(int count, String str) {
        checking("comparison for " + str);

        // Pattern must be compiled each run because numTestsRun is incremented
        Pattern actualLinkPattern1 =
            Pattern.compile("Sub-test " + count + " Actual: " + prefix + ref1, Pattern.DOTALL);
        Pattern expectLinkPattern1 =
            Pattern.compile("Sub-test " + count + " Expect: " + prefix + ref1, Pattern.DOTALL);
        // Pattern linkPattern2 = Pattern.compile(prefix + ref2 + label + end, Pattern.DOTALL);

        Matcher actualLinkMatcher1 = actualLinkPattern1.matcher(str);
        Matcher expectLinkMatcher1 = expectLinkPattern1.matcher(str);
        if (expectLinkMatcher1.find() && actualLinkMatcher1.find()) {
            String expectRef = expectLinkMatcher1.group(2);
            String actualRef = actualLinkMatcher1.group(2);
            if (actualRef.equals(expectRef)) {
                passed(expectRef);
                // System.out.println("pattern:   " + actualLinkPattern1.pattern());
                // System.out.println("actualRef: " + actualRef);
                // System.out.println("group0:    " + actualLinkMatcher1.group());
                // System.out.println("group1:    " + actualLinkMatcher1.group(1));
                // System.out.println("group2:    " + actualLinkMatcher1.group(2));
                // System.out.println("group3:    " + actualLinkMatcher1.group(3));
                // System.exit(0);
            } else {
                failed("\n"
                        + "Actual: \"" + actualRef + "\"\n"
                        + "Expect: \"" + expectRef + "\"");
            }
        } else {
            failed("Didn't find <A HREF> that fits the pattern: "
                    + expectLinkPattern1.pattern());
        }
    }
}
