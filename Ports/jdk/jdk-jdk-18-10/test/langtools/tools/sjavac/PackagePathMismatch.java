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
 * @bug 8059349
 * @summary This test makes sure file paths matches package declarations
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper PackagePathMismatch
 */

import java.nio.file.Path;
import java.nio.file.Paths;

public class PackagePathMismatch extends SjavacBase {
    public static void main(String... args) throws Exception {

        Path root = Paths.get(PackagePathMismatch.class.getSimpleName() + "Test");
        Path src = root.resolve("src");
        Path classes = root.resolve("classes");

        toolbox.writeFile(src.resolve("a/x/c/Test.java"),
                          "package a.b.c; class Test { }");

        // Compile should fail since package a.b.c does not match path a/x/c.
        int rc1 = compile("-d", classes,
                          "--state-dir=" + classes,
                          src);
        if (rc1 == 0)
            throw new AssertionError("Compilation succeeded unexpectedly");
    }
}
