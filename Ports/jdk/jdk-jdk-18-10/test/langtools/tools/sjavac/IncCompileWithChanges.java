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
 * @summary Verify incremental changes in gensrc are handled as expected
 * @bug 8054689
 * @author Fredrik O
 * @author sogoel (rewrite)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @ignore Requires dependency code to deal with in-method dependencies.
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper IncCompileWithChanges
 */

import java.util.*;
import java.nio.file.*;

import toolbox.ToolBox;

public class IncCompileWithChanges extends SJavacTester {
    public static void main(String... args) throws Exception {
        IncCompileWithChanges wc = new IncCompileWithChanges();
        wc.test();
    }

    // Remember the previous bin and headers state here.
    Map<String,Long> previous_bin_state;
    Map<String,Long> previous_headers_state;

    void test() throws Exception {
        clean(TEST_ROOT);
        Files.createDirectories(GENSRC);
        Files.createDirectories(BIN);
        Files.createDirectories(HEADERS);

        initialCompile();
        incrementalCompileWithChange();
    }

    /* Update A.java with a new timestamp and new final static definition.
     * This should trigger a recompile, not only of alfa, but also beta.
     * Generated native header should not be updated since native api of B was not modified.
     */
    void incrementalCompileWithChange() throws Exception {
        previous_bin_state = collectState(BIN);
        previous_headers_state = collectState(HEADERS);
        System.out.println("\nIn incrementalCompileWithChange() ");
        System.out.println("A.java updated to trigger a recompile");
        System.out.println("Generated native header should not be updated since native api of B was not modified");
        tb.writeFile(GENSRC.resolve("alfa/omega/A.java"),
                     "package alfa.omega; public class A implements AINT { " +
                     "public final static int DEFINITION = 18;" +
                     "public void aint() { } private void foo() { } }");

        compile(GENSRC.toString(),
                "-d", BIN.toString(),
                "--state-dir=" + BIN,
                "-h", HEADERS.toString(),
                "-j", "1",
                "--log=debug");
        Map<String,Long> new_bin_state = collectState(BIN);

        verifyNewerFiles(previous_bin_state, new_bin_state,
                         BIN + "/alfa/omega/A.class",
                         BIN + "/alfa/omega/AINT.class",
                         BIN + "/alfa/omega/AA$AAAA.class",
                         BIN + "/alfa/omega/AAAAA.class",
                         BIN + "/alfa/omega/AA$AAA.class",
                         BIN + "/alfa/omega/AA.class",
                         BIN + "/alfa/omega/AA$1.class",
                         BIN + "/beta/B.class",
                         BIN + "/beta/BINT.class",
                         BIN + "/javac_state");
        previous_bin_state = new_bin_state;

        Map<String,Long> new_headers_state = collectState(HEADERS);
        verifyEqual(new_headers_state, previous_headers_state);
    }
}
