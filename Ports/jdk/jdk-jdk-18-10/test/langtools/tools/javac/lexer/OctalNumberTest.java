/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8267361
 * @summary JavaTokenizer reads octal numbers mistakenly
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main OctalNumberTest
 */

import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.Task;

public class OctalNumberTest extends TestRunner {
    ToolBox tb;

    OctalNumberTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        var t = new OctalNumberTest();
        t.runTests();
    }

    @Test
    public void testOctalNumber() throws Exception {
        String code = """
                class Digit {
                    int a = 023; // normal
                    int b = 089;
                    int c = 02389;
                    int d = 028a;
                    int e = 02a8;
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "Digit.java:3:14: compiler.err.expected: ';'",
                "Digit.java:4:16: compiler.err.expected: ';'",
                "Digit.java:5:15: compiler.err.expected: ';'",
                "Digit.java:5:17: compiler.err.expected: token.identifier",
                "Digit.java:6:15: compiler.err.expected: ';'",
                "Digit.java:6:17: compiler.err.expected: token.identifier",
                "6 errors");
        tb.checkEqual(expected, output);
    }
}
