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
 * @bug 4510979
 * @summary Test to make sure that the source documentation is indented properly
 * when -linksourcetab is used.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestSourceTab
 */

import java.io.*;

import javadoc.tester.JavadocTester;

public class TestSourceTab extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSourceTab tester = new TestSourceTab();
        tester.runTests();
    }

    @Test
    public void test() throws Exception {
        String tmpSrcDir = "tmpSrc";
        String outdir1 = "out-tabLengthEight";
        String outdir2 = "out-tabLengthFour";
        initTabs(new File(testSrc), new File(tmpSrcDir));

        // Run Javadoc on a source file with that is indented with a single tab per line
        javadoc("-d", outdir1,
                "-sourcepath", tmpSrcDir,
                "-notimestamp",
                "-linksource",
                tmpSrcDir + "/SingleTab/C.java");
        checkExit(Exit.OK);

        // Run Javadoc on a source file with that is indented with a two tab per line
        // If we double the tabs and decrease the tab length by a half, the output should
        // be the same as the one generated above.
        javadoc("-d", outdir2,
                "-sourcepath", tmpSrcDir,
                "-notimestamp",
                "-sourcetab", "4",
                tmpSrcDir + "/DoubleTab/C.java");
        checkExit(Exit.OK);

        diff(outdir1, outdir2,
                "src-html/C.html",
                "C.html");
    }

    void initTabs(File from, File to) throws IOException {
        for (File f: from.listFiles()) {
            File t = new File(to, f.getName());
            if (f.isDirectory()) {
                initTabs(f, t);
            } else if (f.getName().endsWith(".java")) {
                write(t, read(f).replace("\\t", "\t"));
            }
        }
    }

    String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append(NL);
            }
        }
        return sb.toString();
    }

    void write(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        try (Writer out = new FileWriter(f)) {
            out.write(s);
        }
    }
}
