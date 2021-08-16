/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test support for -source and -target checks
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build OptionModesTester
 * @run main SourceTargetTest
 */

import com.sun.tools.javac.main.Main;
import java.io.IOException;

public class SourceTargetTest extends OptionModesTester {
    public static void main(String... args) throws Exception {
        SourceTargetTest t = new SourceTargetTest();
        t.runTests();
    }

    @Test
    void testSourceTarget() throws IOException {
        String v = System.getProperty("java.specification.version");
        String[] va = v.split("\\.");
        int major = Integer.parseInt(va[0]);
        boolean newVersion = major > 8;
        String latest = (newVersion) ? va[0] : va[1];
        String prev = String.valueOf(Integer.valueOf(latest) - 1);

        writeFile("C.java", "class C { }");

        String[] opts = { "-source", latest, "-target", prev };
        String[] files = { "C.java" };

        runMain(opts, files)
                .checkResult(Main.Result.CMDERR.exitCode);

        runCall(opts, files)
                .checkIllegalStateException();

        runParse(opts, files)
                .checkIllegalStateException();
    }

    @Test
    void testObsoleteSourceTarget() throws IOException {

        writeFile("C.java", "class C { }");

        String[] opts = { "-source", "1.7", "-target", "1.7" };
        String[] files = { "C.java" };

        runMain(opts, files)
                .checkOK()
                .checkLog("obsolete");

        runCall(opts, files)
                .checkOK()
                .checkLog("obsolete");

        runParse(opts, files)
                .checkOK()
                .checkLog("obsolete");
    }
}
