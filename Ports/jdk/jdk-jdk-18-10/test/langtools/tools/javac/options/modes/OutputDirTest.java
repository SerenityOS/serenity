/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044859
 * @summary test support for output directory options  -d  and -s
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build OptionModesTester
 * @run main OutputDirTest
 */

import com.sun.tools.javac.main.Main;
import java.io.IOException;

public class OutputDirTest extends OptionModesTester {
    public static void main(String... args) throws Exception {
        OutputDirTest t = new OutputDirTest();
        t.writeFile("src/C.java", "class C { }");
        t.runTests();
    }

    @Test
    void testClassOutputDir() throws IOException {
        testOutputDir("-d");
    }

    @Test
    void testSourceOutputDir() throws IOException {
        testOutputDir("-s");
    }

    void testOutputDir(String opt) {
        String[] opts = { opt, "does-not-exist"};
        String[] files = { "src/C.java" };

        runMain(opts, files)
                .checkResult(Main.Result.OK.exitCode);

// The API tests are disabled (for now) because Args.validate does
// not have an easy way to access/validate file manager options,
// which are handled directly by the file manager

//        runCall(opts, files)
//                .checkIllegalStateException();

//        runParse(opts, files)
//                .checkIllegalStateException();
    }
}
