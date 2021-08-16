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
 * @summary Verify that we can make sources invisible to linking (sourcepath)
 * @bug 8054689
 * @author Fredrik O
 * @author sogoel (rewrite)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper CompileWithInvisibleSources
 */

import java.util.*;
import java.nio.file.*;

public class CompileWithInvisibleSources extends SJavacTester {
    public static void main(String... args) throws Exception {
        CompileWithInvisibleSources cis = new CompileWithInvisibleSources();
        cis.test();
    }

    // Compile gensrc and link against gensrc2 and gensrc3
    // gensrc2 contains broken code in beta.B, thus exclude that package
    // gensrc3 contains a proper beta.B
    void test() throws Exception {
        Files.createDirectories(BIN);

        Map<String,Long> previous_bin_state = collectState(BIN);

        tb.writeFile(GENSRC.resolve("alfa/omega/A.java"),
                     "package alfa.omega; import beta.B; import gamma.C; public class A { B b; C c; }");
        tb.writeFile(GENSRC2.resolve("beta/B.java"),
                     "package beta; public class B { broken");
        tb.writeFile(GENSRC2.resolve("gamma/C.java"),
                     "package gamma; public class C { }");
        tb.writeFile(GENSRC3.resolve("beta/B.java"),
                     "package beta; public class B { }");

        compile(GENSRC.toString(),
                "-x", "beta/*",
                "-sourcepath", GENSRC2.toString(),
                "-sourcepath", GENSRC3.toString(),
                "-d", BIN.toString(),
                "--state-dir=" + BIN,
                "-h", HEADERS.toString(),
                "-j", "1");

        System.out.println("The first compile went well!");
        Map<String,Long> new_bin_state = collectState(BIN);
        verifyThatFilesHaveBeenAdded(previous_bin_state, new_bin_state,
                                     BIN + "/alfa/omega/A.class",
                                     BIN + "/javac_state");

        System.out.println("----- Compile with exluded beta went well!");
        tb.cleanDirectory(BIN);
        compileExpectFailure(GENSRC.toString(),
                             "-sourcepath", GENSRC2.toString(),
                             "-sourcepath", GENSRC3.toString(),
                             "-d", BIN.toString(),
                             "--state-dir=" + BIN,
                             "-h", HEADERS.toString(),
                             "-j", "1");

        System.out.println("----- Compile without exluded beta failed, as expected! Good!");
    }
}
