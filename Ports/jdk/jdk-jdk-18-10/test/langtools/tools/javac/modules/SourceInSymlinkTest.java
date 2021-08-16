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
 * @bug 8175990
 * @summary source in symbolic link
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main SourceInSymlinkTest
 */

import java.nio.file.*;
import javax.tools.*;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class SourceInSymlinkTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        SourceInSymlinkTest t = new SourceInSymlinkTest();
        t.runTests();
    }

    @Test
    public void testModuleSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m = src.resolve("m");
        tb.writeFile(src_m.resolve("module-info.java"), "module m { }");
        tb.writeJavaFiles(src_m, "package p; public class A{}");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--module-source-path", src.toString())
            .outdir(classes)
            .files(src_m.resolve("p/A.java"))
            .run()
            .writeAll();

        checkFiles(
            classes.resolve("m/module-info.class"),
            classes.resolve("m/p/A.class"));

        // ok normal case works, now create a symlink to the source
        Path lsrc = base.resolve("link");
        Path lsrc_m = src.resolve("m");
        Path lclasses = base.resolve("link-out");
        Files.createDirectories(lclasses);
        try {
            Files.createSymbolicLink(lsrc, Paths.get("src"));
        } catch (FileSystemException fse) {
            System.err.println("warning: test passes vacuously, sym-link could not be created");
            System.err.println(fse.getMessage());
            return;
        }
        new JavacTask(tb)
            .options("--module-source-path", lsrc.toString())
            .outdir(lclasses)
            .files(lsrc_m.resolve("p/A.java"))
            .run()
            .writeAll();

        checkFiles(
            lclasses.resolve("m/module-info.class"),
            lclasses.resolve("m/p/A.class"));

    }

    void checkFiles(Path... files) throws Exception {
        for (Path f: files) {
            if (!Files.exists(f))
                throw new Exception("expected file not found: " + f);
        }
    }
}
