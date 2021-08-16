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
 * @summary Verify that circular sources split on multiple cores can be compiled
 * @bug 8054689
 * @author Fredrik O
 * @author sogoel (rewrite)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper CompileCircularSources
 */

import java.io.IOException;
import java.util.*;
import java.nio.file.*;

public class CompileCircularSources extends SJavacTester {
    public static void main(String... args) throws Exception {
        CompileCircularSources ccs = new CompileCircularSources();
        ccs.test();
    }

    void test() throws Exception {
        Files.createDirectories(BIN);
        Files.createDirectories(GENSRC);

        Map<String,Long> previous_bin_state = collectState(BIN);

        tb.writeFile(GENSRC.resolve("alfa/omega/A.java"),
                     "package alfa.omega; public class A { beta.B b; }");
        tb.writeFile(GENSRC.resolve("beta/B.java"),
                     "package beta; public class B { gamma.C c; }");
        tb.writeFile(GENSRC.resolve("gamma/C.java"),
                     "package gamma; public class C { alfa.omega.A a; }");

        compile(GENSRC.toString(),
                "-d", BIN.toString(),
                "-h", HEADERS.toString(),
                "--state-dir=" + BIN,
                "-j", "3",
                "--log=debug");
        Map<String,Long> new_bin_state = collectState(BIN);
        verifyThatFilesHaveBeenAdded(previous_bin_state,
                                     new_bin_state,
                                     BIN + "/alfa/omega/A.class",
                                     BIN + "/beta/B.class",
                                     BIN + "/gamma/C.class",
                                     BIN + "/javac_state");
    }
}
