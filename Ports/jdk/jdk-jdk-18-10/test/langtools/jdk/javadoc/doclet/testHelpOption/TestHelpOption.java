/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4934778 4777599 6553182 8146427 8146475 8175055 8185371
 * @summary  Make sure that --help, -helpfile and -nohelp options work correctly.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* TestHelpOption
 * @run main TestHelpOption
 */

import java.util.*;
import java.util.stream.*;

import javadoc.tester.JavadocTester;

public class TestHelpOption extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHelpOption tester = new TestHelpOption();
        tester.runTests();
    }

    @Test
    public void testLineLengths() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-X",
                testSrc("TestXOption.java"));
        checkExit(Exit.OK);
        List<String> longLines = getOutputLines(Output.OUT).stream()
                .filter(s -> s.length() > 80)
                .collect(Collectors.toList());
        checking("line lengths");
        if (longLines.isEmpty()) {
            passed("all lines OK");
        } else {
            out.println("long lines:");
            longLines.stream().forEach(s -> out.println(">>>" + s + "<<<"));
            failed(longLines.size() + " long lines");
        }
    }

    @Test
    public void testWithOption() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "--help",
                testSrc("Sample.java"));
        checkExit(Exit.OK);

        checkOutput(true);
    }

    @Test
    public void testWithoutOption() {
        javadoc("-d", "out2",
                "-sourcepath", testSrc,
                testSrc("Sample.java"));
        checkExit(Exit.OK);
    }

    @Test
    public void testNohelpOption() {
        javadoc("-d", "out3",
                "-sourcepath", testSrc,
                "-nohelp",
                testSrc("Sample.java"));
        checkOutput("Sample.html", false, """
            <li><a href="../help-doc.html#class">Help</a></li>""");
        checkExit(Exit.OK);
    }

    @Test
    public void testHelpfileOption() {
        javadoc("-d", "out4",
                "-sourcepath", testSrc,
                "-helpfile", testSrc("test-help.html"),
                testSrc("Sample.java"));
        checkExit(Exit.OK);
        checkOutput("Sample.html", true,
                """
                    <li><a href="test-help.html#class">Help</a></li>""");
        checkOutput("test-help.html", true,
                "Help, help.");
    }

    @Test
    public void testHelpfileReuseOption() {
        javadoc("-d", "out5",
                "-sourcepath", testSrc,
                "-helpfile", testSrc("test-help.html"),
                "-helpfile", testSrc("test-help.html"),
                testSrc("Sample.java"));
        checkExit(Exit.CMDERR);
    }

    @Test
    public void testHelpfileNohelpConflict() {
        javadoc("-d", "out6",
                "-sourcepath", testSrc,
                "-helpfile", testSrc("test-help.html"),
                "-nohelp",
                testSrc("Sample.java"));
        checkExit(Exit.CMDERR);
    }

    private void checkOutput(boolean withOption) {
        checkOutput(Output.OUT, withOption,
                "-d ",
                "-use ",
                "-version ",
                "-author ",
                "-docfilessubdirs\n",
                "-splitindex ",
                "-windowtitle ",
                "-doctitle ",
                "-header ",
                "-footer ",
                "-bottom ",
                "-link ",
                "-linkoffline ",
                "-excludedocfilessubdir ",
                "-group ",
                "-nocomment ",
                "-nodeprecated\n",
                "-noqualifier ",
                "-nosince ",
                "-notimestamp ",
                "-nodeprecatedlist\n",
                "-notree ",
                "-noindex ",
                "-nohelp ",
                "-nonavbar ",
                "-serialwarn ",
                "-tag ",
                "-taglet ",
                "-tagletpath ",
                "-charset ",
                "-helpfile ",
                "-linksource ",
                "-sourcetab ",
                "-keywords ",
                "-stylesheetfile ",
                "--add-stylesheet ",
                "-docencoding ",
                "-html5 ",
                "-top ",
                "-author ",
                "-noqualifier ",
                "-nosince ",
                "-notimestamp ",
                "-sourcetab ");

        checkFileAndOutput("Sample.html", !withOption,
                """
                    <li><a href="help-doc.html">Help</a></li>""");
    }
}
