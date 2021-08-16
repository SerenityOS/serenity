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
 * @summary Verify native files are rewritten
 * @bug 8054689
 * @author Fredrik O
 * @author sogoel (rewrite)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper IncCompileUpdateNative
 */

import java.util.*;
import java.nio.file.*;

public class IncCompileUpdateNative extends SJavacTester {
    public static void main(String... args) throws Exception {
        IncCompileUpdateNative un = new IncCompileUpdateNative();
        un.test();
    }

    // Remember the previous bin and headers state here.
    Map<String,Long> previous_bin_state;
    Map<String,Long> previous_headers_state;

    void test() throws Exception {
        Files.createDirectories(GENSRC);
        Files.createDirectories(BIN);
        Files.createDirectories(HEADERS);

        initialCompile();
        incrementalCompileChangeNative();
    }

    // Update B.java with a new value for the final static annotated with @Native
    // Verify that beta_B.h is rewritten again
    void incrementalCompileChangeNative() throws Exception {
        previous_bin_state = collectState(BIN);
        previous_headers_state = collectState(HEADERS);
        System.out.println("\nIn incrementalCompileChangeNative() ");
        System.out.println("Verify that beta_B.h is rewritten again");
        tb.writeFile(GENSRC.resolve("beta/B.java"),
                     "package beta; import alfa.omega.A; public class B {"+
                     "private int b() { return A.DEFINITION; } "+
                      "@java.lang.annotation.Native final static int alfa = 43; }");

        compile(GENSRC.toString(),
                "-d", BIN.toString(),
                "--state-dir=" + BIN,
                "-h", HEADERS.toString(),
                "-j", "1",
                "--log=debug");
        Map<String,Long> new_bin_state = collectState(BIN);
        verifyNewerFiles(previous_bin_state, new_bin_state,
                         BIN + "/beta/B.class",
                         BIN + "/beta/BINT.class",
                         BIN + "/javac_state");
        previous_bin_state = new_bin_state;

        Map<String,Long> new_headers_state = collectState(HEADERS);
        verifyNewerFiles(previous_headers_state, new_headers_state,
                         HEADERS + "/beta_B.h");
        previous_headers_state = new_headers_state;
    }
}
