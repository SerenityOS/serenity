/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary simple tests of javac compilation modes
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main HelloWorldTest
 */

import java.nio.file.*;
import javax.tools.*;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class HelloWorldTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        HelloWorldTest t = new HelloWorldTest();
        t.runTests();
    }

    public static final String HELLO_WORLD =
          """
              class HelloWorld {
                  public static void main(String... args) {
                      System.out.println("Hello World!");
                  }
              }""";

    public static final String PKG_HELLO_WORLD =
          "package p;\n"
        + HELLO_WORLD;

    @Test
    public void testLegacyMode(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, HELLO_WORLD);

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        Path smallRtJar = base.resolve("small-rt.jar");
        try (JavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            new JarTask(tb, smallRtJar)
                .files(fm, StandardLocation.PLATFORM_CLASS_PATH,
                    "java.lang.**", "java.io.*", "java.util.*")
                .run();
        }

        new JavacTask(tb)
            .options("-source", "8",
                "-target", "8",
                "--boot-class-path", smallRtJar.toString())
            .outdir(classes)
            .files(src.resolve("HelloWorld.java"))
            .run();

        checkFiles(classes.resolve("HelloWorld.class"));
    }

    @Test
    public void testUnnamedModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, HELLO_WORLD);

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .outdir(classes)
            .files(src.resolve("HelloWorld.java"))
            .run();

        checkFiles(classes.resolve("HelloWorld.class"));
    }

    @Test
    public void testSingleModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "module m { }");
        tb.writeJavaFiles(src, PKG_HELLO_WORLD);

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb, Task.Mode.CMDLINE)
            .outdir(classes)
            .files(src.resolve("module-info.java"), src.resolve("p/HelloWorld.java"))
            .run()
            .writeAll();

        checkFiles(
            classes.resolve("module-info.class"),
            classes.resolve("p/HelloWorld.class"));
    }

    @Test
    public void testModuleSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m = src.resolve("m");
        tb.writeFile(src_m.resolve("module-info.java"), "module m { }");
        tb.writeJavaFiles(src_m, PKG_HELLO_WORLD);

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--module-source-path", src.toString())
            .outdir(classes)
            .files(src_m.resolve("p/HelloWorld.java"))
            .run()
            .writeAll();

        checkFiles(
            classes.resolve("m/module-info.class"),
            classes.resolve("m/p/HelloWorld.class"));
    }

    void checkFiles(Path... files) throws Exception {
        for (Path f: files) {
            if (!Files.exists(f))
                throw new Exception("expected file not found: " + f);
        }
    }
}
