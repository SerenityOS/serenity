/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8175346 8175277
 * @summary Test release option interactions
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main ReleaseOptions
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.*;

public class ReleaseOptions extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new ReleaseOptions().runTests();
    }

    @Test
    public void testReleaseWithPatchModule(Path base) throws Exception {
        Path src = Paths.get(base.toString(), "src");
        Path mpath = Paths.get(src. toString(), "m");

        tb.writeJavaFiles(mpath,
                "module m { exports p; }",
                "package p; public class C { }");

        Task.Result result = execNegativeTask("--release", "8",
                "--patch-module", "m=" + mpath.toString(),
                "p");
        assertMessagePresent(".*not allowed with target 8.*");
        assertMessageNotPresent(".*Exception*");
        assertMessageNotPresent(".java.lang.AssertionError.*");
    }

    @Test
    public void testReleaseWithSourcepath(Path base) throws Exception {
        Path src = Paths.get(base.toString(), "src");
        Path mpath = Paths.get(src. toString(), "m");

        tb.writeJavaFiles(mpath,
                "module m { exports p; }",
                "package p; public class C { }");

        Task.Result result = execNegativeTask("--release", "8",
                "--source-path", mpath.toString(),
                "--module", "m");
        assertMessagePresent(".*(use -source 9 or higher to enable modules).*");
        assertMessageNotPresent(".*Exception*");
        assertMessageNotPresent(".java.lang.AssertionError.*");
    }

    @Test
    public void testReleaseWithModuleSourcepath(Path base) throws Exception {
        Path src = Paths.get(base.toString(), "src");
        Path mpath = Paths.get(src.toString(), "m");

        tb.writeJavaFiles(mpath,
                "module m { exports p; }",
                "package p; public class C { }");

        Task.Result result = execNegativeTask("--release", "8",
                "--module-source-path", src.toString(),
                "--module", "m");
        assertMessagePresent(".*not allowed with target 8.*");
        assertMessageNotPresent(".*Exception*");
        assertMessageNotPresent(".java.lang.AssertionError.*");
    }
}
