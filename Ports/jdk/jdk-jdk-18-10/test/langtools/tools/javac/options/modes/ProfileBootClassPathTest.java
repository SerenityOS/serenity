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
 * @summary test support for checking -profile and -bootclasspath
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build OptionModesTester
 * @run main ProfileBootClassPathTest
 */

import com.sun.tools.javac.main.Main;
import java.io.File;
import java.io.IOException;

public class ProfileBootClassPathTest extends OptionModesTester {
    public static void main(String... args) throws Exception {
        ProfileBootClassPathTest t = new ProfileBootClassPathTest();
        t.runTests();
    }

    @OptionModesTester.Test
    void testProfileBootClassPath() throws IOException {
        writeFile("C.java", "class C { }");

        String javaHome = System.getProperty("java.home");
        String rt_jar = new File(javaHome, "jre/lib/rt.jar").getPath();
        String[] opts = { "-profile", "compact1", "-bootclasspath", rt_jar };
        String[] files = { "C.java" };

        runMain(opts, files)
                .checkResult(Main.Result.CMDERR.exitCode);

// The API tests are disabled (for now) because Args.validate does
// not have an easy way to access/validate file manager options,
// which are handled directly by the file manager

//        runCall(opts, files)
//                .checkIllegalStateException();

//        runParse(opts, files)
//                .checkIllegalStateException();
    }
}
