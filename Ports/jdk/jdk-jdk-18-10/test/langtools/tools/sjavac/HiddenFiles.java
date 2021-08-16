/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144226
 * @summary Ensures that excluded files are inaccessible (even for implicit
 *          compilation)
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 *          jdk.compiler/com.sun.tools.sjavac.server
 * @library /tools/lib
 * @build Wrapper toolbox.ToolBox toolbox.Assert
 * @run main Wrapper HiddenFiles
 */


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.javac.main.Main.Result;

import toolbox.Assert;

public class HiddenFiles extends SjavacBase {

    public static void main(String[] ignore) throws Exception {
        Path BIN = Paths.get("bin");
        Path STATE_DIR = Paths.get("state-dir");
        Path SRC = Paths.get("src");

        Files.createDirectories(BIN);
        Files.createDirectories(STATE_DIR);

        toolbox.writeJavaFiles(SRC, "package pkg; class A { B b; }");
        toolbox.writeJavaFiles(SRC, "package pkg; class B { }");

        // This compilation should fail (return RC_FATAL) since A.java refers to B.java and B.java
        // is excluded.
        int rc = compile("-x", "pkg/B.java", SRC.toString(),
                         "-d", BIN.toString(),
                         "--state-dir=" + STATE_DIR);

        Assert.check(rc == Result.ERROR.exitCode, "Compilation succeeded unexpectedly.");
    }
}
