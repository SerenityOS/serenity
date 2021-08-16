/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8007687
 * @summary  Make sure that the -X option works properly.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestXOption
 */

import java.util.*;
import java.util.stream.*;

import javadoc.tester.JavadocTester;

public class TestXOption extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestXOption tester = new TestXOption();
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
    public void testWithHelpExtraOption() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "--help-extra",
                testSrc("TestXOption.java"));
        checkExit(Exit.OK);
        checkOutput(true);
    }

    @Test
    public void testWithOption() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-X",
                testSrc("TestXOption.java"));
        checkExit(Exit.OK);
        checkOutput(true);
    }

    @Test
    public void testWithoutOption() {
        javadoc("-d", "out2",
                "-sourcepath", testSrc,
                testSrc("TestXOption.java"));
        checkExit(Exit.OK);
        checkOutput(false);
    }

    private void checkOutput(boolean expectFound) {
        checkOutput(Output.OUT, expectFound,
                "-Xmaxerrs ",
                "-Xmaxwarns ",
                "-Xdocrootparent ",
                "-Xdoclint ",
                "-Xdoclint:");
    }
}
