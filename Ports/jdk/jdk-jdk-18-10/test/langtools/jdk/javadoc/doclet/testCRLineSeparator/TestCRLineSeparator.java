/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4979486
 * @summary  Make sure tool parses CR line separators properly.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestCRLineSeparator
 */

import java.io.*;
import java.util.*;

import javadoc.tester.JavadocTester;

public class TestCRLineSeparator extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestCRLineSeparator tester = new TestCRLineSeparator();
        tester.runTests();
    }

    @Test
    public void test() throws IOException {
        initFiles(new File(testSrc), new File("src"), "pkg");
        javadoc("-d", "out",
                "-sourcepath", "src",
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/MyClass.html", true,
                "Line 1\n"
                + " Line 2");
    }

    // recursively copy files from fromDir to toDir, replacing newlines
    // with \r
    void initFiles(File fromDir, File toDir, String f) throws IOException {
        File from_f = new File(fromDir, f);
        File to_f = new File(toDir, f);
        if (from_f.isDirectory()) {
            to_f.mkdirs();
            for (String child: from_f.list()) {
                initFiles(from_f, to_f, child);
            }
        } else {
            List<String> lines = new ArrayList<>();
            try (BufferedReader in = new BufferedReader(new FileReader(from_f))) {
                String line;
                while ((line = in.readLine()) != null)
                    lines.add(line);
            }
            try (BufferedWriter out = new BufferedWriter(new FileWriter(to_f))) {
                for (String line: lines) {
                    out.write(line);
                    out.write("\r");
                }
            }
        }
    }
}
