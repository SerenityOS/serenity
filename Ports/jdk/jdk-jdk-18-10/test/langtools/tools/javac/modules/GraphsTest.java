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
 * @summary tests for module graph resolution issues
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.ModuleBuilder
 *      ModuleTestBase
 * @run main GraphsTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ModuleBuilder;
import toolbox.Task;
import toolbox.ToolBox;

public class GraphsTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        GraphsTest t = new GraphsTest();
        t.runTests();
    }

    /**
     * Tests diamond graph with an automatic module added in.
     * +-------------+          +-----------------------+         +------------------+
     * | module M    |          | module N              |         | module O         |
     * |             | ----->   |                       | --->    |                  |  --> J.jar
     * | require N   |          | requires transitive O |         |                  |
     * | require L   |          |                       |         +------------------+
     * +-------------+          +-----------------------+                  ^
     *       |                                                          |
     *       |                  +-----------------------+                  |
     *       ------------------>| module L              |                  |
     *                          |                       |------------------
     *                          | requires transitive O |
     *                          |                       |
     *                          +-----------------------+
     *
     */
    @Test
    public void diamond(Path base) throws Exception {

        Path modSrc = Files.createDirectories(base.resolve("modSrc"));
        Path modules = Files.createDirectories(base.resolve("modules"));

        new ModuleBuilder(tb, "J")
                .exports("openJ")
                .classes("package openJ; public class J { }")
                .classes("package closedJ; public class J { }")
                .build(base.resolve("jar"));

        Path jarModules = Files.createDirectories(base.resolve("jarModules"));
        Path jar = jarModules.resolve("J.jar");
        new JarTask(tb, jar)
                .baseDir(base.resolve("jar/J"))
                .files(".")
                .run()
                .writeAll();

        new ModuleBuilder(tb, "O")
                .exports("openO")
                .requiresTransitive("J", jarModules)
                .classes("package openO; public class O { openJ.J j; }")
                .classes("package closedO; public class O { }")
                .build(modSrc, modules);
        new ModuleBuilder(tb, "N")
                .requiresTransitive("O", modules, jarModules)
                .exports("openN")
                .classes("package openN; public class N { }")
                .classes("package closedN; public class N { }")
                .build(modSrc, modules);
        new ModuleBuilder(tb, "L")
                .requiresTransitive("O", modules, jarModules)
                .exports("openL")
                .classes("package openL; public class L { }")
                .classes("package closedL; public class L { }")
                .build(modSrc, modules);
        ModuleBuilder m = new ModuleBuilder(tb, "M");
        //positive case
        Path positiveSrc = m
                .requires("N", modules)
                .requires("L", modules)
                .classes("package p; public class Positive { openO.O o; openN.N n; openL.L l; }")
                .write(base.resolve("positiveSrc"));

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "-p", modules + File.pathSeparator + jarModules)
                .outdir(Files.createDirectories(base.resolve("positive")))
                .files(findJavaFiles(positiveSrc))
                .run()
                .writeAll();
        //negative case
        Path negativeSrc = m.classes("package p; public class Negative { closedO.O o; closedN.N n; closedL.L l; }")
                .write(base.resolve("negativeSrc"));
        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-p", modules + File.pathSeparator + jarModules)
                .outdir(Files.createDirectories(base.resolve("negative")))
                .files(findJavaFiles(negativeSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "Negative.java:1:36: compiler.err.package.not.visible: closedO, (compiler.misc.not.def.access.not.exported: closedO, O)",
                "Negative.java:1:49: compiler.err.package.not.visible: closedN, (compiler.misc.not.def.access.not.exported: closedN, N)",
                "Negative.java:1:62: compiler.err.package.not.visible: closedL, (compiler.misc.not.def.access.not.exported: closedL, L)");
        if (!log.containsAll(expected)) {
            throw new Exception("Expected output not found");
        }
        //multi module mode
        m.write(modSrc);
        List<String> out = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", modSrc.toString(),
                        "-p", jarModules.toString()
                )
                .outdir(Files.createDirectories(base.resolve("negative")))
                .files(findJavaFiles(modSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        expected = Arrays.asList(
                "Negative.java:1:36: compiler.err.package.not.visible: closedO, (compiler.misc.not.def.access.not.exported: closedO, O)",
                "Negative.java:1:49: compiler.err.package.not.visible: closedN, (compiler.misc.not.def.access.not.exported: closedN, N)",
                "Negative.java:1:62: compiler.err.package.not.visible: closedL, (compiler.misc.not.def.access.not.exported: closedL, L)");
        if (!out.containsAll(expected)) {
            throw new Exception("Expected output not found");
        }
        //checks if the output does not contain messages about exported packages.
        Pattern regex = Pattern.compile("compiler\\.err.*(openO\\.O|openN\\.N|openL\\.L)");
        for (String s : out) {
            if (regex.matcher(s).find()) {
                throw new Exception("Unexpected output: " + s);
            }
        }
    }

    /**
     * Tests graph where module M reexport package of N, but N export the package only to M.
     *
    +-------------+        +------------------------+        +---------------+
    | module L    |        | module M               |        | module N      |
    |             | -----> |                        | -----> |               |
    |  requires M |        |  requires transitive N |        | exports P to M|
    +-------------+        |                        |        +---------------+
                           +------------------------+
    */
    @Test
    public void reexportOfQualifiedExport(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        new ModuleBuilder(tb, "M")
                .requiresTransitive("N")
                .write(modSrc);
        new ModuleBuilder(tb, "N")
                .exportsTo("pack", "M")
                .classes("package pack; public class Clazz { }")
                .write(modSrc);
        new ModuleBuilder(tb, "L")
                .requires("M")
                .classes("package p; public class A { A(pack.Clazz cl){} } ")
                .write(modSrc);
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", modSrc.toString())
                .outdir(Files.createDirectories(base.resolve("negative")))
                .files(findJavaFiles(modSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String expected = "A.java:1:31: compiler.err.package.not.visible: pack, (compiler.misc.not.def.access.not.exported.to.module: pack, N, L)";
        if (!log.contains(expected)) {
            throw new Exception("Expected output not found");
        }
    }
}
