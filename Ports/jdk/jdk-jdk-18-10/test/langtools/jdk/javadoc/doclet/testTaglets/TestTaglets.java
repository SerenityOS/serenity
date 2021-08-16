/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8202947
 * @summary  Test TagletManager initialization
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestTaglets
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

/*
 * This is a golden-file test for the output of the hidden
 * option {@code --show-taglets}. The output is the basis
 * for a table at the end of the doc comment specification,
 * so changes in the golden output may indicate a need for
 * a corresponding update to the spec.
 */
public class TestTaglets extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestTaglets tester = new TestTaglets();
        tester.runTests();
    }

    ToolBox tb = new ToolBox();
    Path src;

    TestTaglets() throws Exception {
        src = Files.createDirectories(Paths.get("src"));
        tb.writeJavaFiles(src, "public class Test { }\n");
    }

    @Test
    public void test() throws Exception {
        javadoc("-d", "out",
                "-javafx",
                "--show-taglets",
                src.resolve("Test.java").toString());
        checkExit(Exit.OK);

        checking("Checking ref file");
        try {
            List<String> refLines = tb.readAllLines(Paths.get(testSrc).resolve("TestTaglets.out"));
            List<String> stdout = getOutputLines(Output.STDOUT);
            tb.checkEqual(refLines, stdout);
            passed("output is as expected");
        } catch (Error e) {
            failed("output not as expected: " + e.getMessage());
        }
    }
}
