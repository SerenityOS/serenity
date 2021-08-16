/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246078
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestHelpPage
 */

import javadoc.tester.JavadocTester;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHelpPage extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHelpPage tester = new TestHelpPage();
        tester.runTests();
    }

    @Test
    public void test() {

        setOutputDirectoryCheck(DirectoryCheck.NONE);
        setAutomaticCheckLinks(false);
        setAutomaticCheckAccessibility(false);

        javadoc("--version");
        List<String> lines = getOutputLines(Output.OUT);
        System.out.println("lines: " + Arrays.toString(lines.toArray()));
        String firstLine = lines.get(0);
        Matcher m = Pattern.compile("javadoc\\s+(.*)").matcher(firstLine);
        m.matches();
        String vstr = m.group(1);
        System.out.printf("vstr='%s'%n", vstr);
        Runtime.Version v = Runtime.Version.parse(vstr);
        System.out.printf("v=%s, v.feature()=%s%n", v, v.feature());

        javadoc("-d", "out",
                "-sourcepath", testSrc,
                testSrc("TestHelpPage.java"));
        checkExit(Exit.OK);

        checking("Reference to a particular version");
        Pattern searchSpecLink = Pattern.compile(
                "\\Q<a href=\"https://docs.oracle.com/en/java/javase/\\E(\\d+)\\Q/docs/specs/javadoc/javadoc-search-spec.html\">\\E");

        String helpContents = readOutputFile("help-doc.html");
        Matcher m2 = searchSpecLink.matcher(helpContents);
        if (!m2.find()) {
            failed("Reference not found: " + helpContents);
            return;
        }

        if (!String.valueOf(v.feature()).equals(m2.group(1))) {
            failed("Reference to a wrong version: " + m2.group(1));
            return;
        }

        boolean foundMore = false;
        while (m2.find()) {
            // print all found before failing
            foundMore = true;
            System.out.println(m2.group(0));
        }
        if (foundMore) {
            failed("Multiple references: " + helpContents);
        } else {
            passed("All good");
        }
    }
}
