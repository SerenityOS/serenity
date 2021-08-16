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

/*
 * @test
 * @bug 8175057
 * @summary Verify that having module-info on patch path works correctly.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main ModuleInfoPatchPath
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task.OutputKind;

public class ModuleInfoPatchPath extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new ModuleInfoPatchPath().runTests();
    }

    @Test
    public void testModuleInfoToModulePath(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { exports api; }",
                          "package api; public class Api {}");
        Path patch = base.resolve("patch");
        tb.writeJavaFiles(patch,
                          "module m { requires java.compiler; exports api; }",
                          "package api; public class Api { public static javax.lang.model.element.Element element; }");
        Path classes = base.resolve("classes");
        Path mClasses = classes.resolve("m");
        tb.createDirectories(mClasses);

        System.err.println("Building the vanilla module...");

        new JavacTask(tb)
            .outdir(mClasses)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        Path test = base.resolve("test");
        tb.writeJavaFiles(test,
                          "module test { requires m; }",
                          "package test; public class Test { private void test() { api.Api.element = null; } }");

        Path testClasses = classes.resolve("test");
        tb.createDirectories(testClasses);

        System.err.println("Building patched module...");

        new JavacTask(tb)
            .options("--module-path", mClasses.toString(),
                     "--patch-module", "m=" + patch.toString())
            .outdir(testClasses)
            .files(findJavaFiles(test))
            .run()
            .writeAll();

        Path patchClasses = classes.resolve("patch");
        tb.createDirectories(patchClasses);

        System.err.println("Building patch...");

        new JavacTask(tb)
            .outdir(patchClasses)
            .files(findJavaFiles(patch))
            .run()
            .writeAll();

        tb.cleanDirectory(testClasses);

        Files.delete(patch.resolve("module-info.java"));
        Files.copy(patchClasses.resolve("module-info.class"), patch.resolve("module-info.class"));

        System.err.println("Building patched module against binary patch...");

        new JavacTask(tb)
            .options("--module-path", mClasses.toString(),
                     "--patch-module", "m=" + patch.toString())
            .outdir(testClasses)
            .files(findJavaFiles(test))
            .run()
            .writeAll();
    }

    @Test
    public void testModuleInfoToSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { exports api; }",
                          "package api; public class Api {}",
                          "package test; public class Test { private void test() { api.Api.element = null; } }");
        Path patch = base.resolve("patch");
        tb.writeJavaFiles(patch,
                          "module m { requires java.compiler; exports api; }",
                          "package api; public class Api { public static javax.lang.model.element.Element element; }");
        Path classes = base.resolve("classes");
        Path mClasses = classes.resolve("m");
        tb.createDirectories(mClasses);

        System.err.println("Building patched module against source patch...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "-sourcepath", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(src.resolve("test")))
            .run()
            .writeAll();

        //incremental compilation:
        List<String> log;

        System.err.println("Incremental building of patched module against source patch, no module-info...");

        log = new JavacTask(tb)
                .options("--patch-module", "m=" + patch.toString(),
                         "-sourcepath", src.toString(),
                         "-verbose")
                .outdir(mClasses)
                .files(findJavaFiles(src.resolve("test")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (log.stream().filter(line -> line.contains("[parsing started")).count() != 1) {
            throw new AssertionError("incorrect number of parsing events.");
        }

        System.err.println("Incremental building of patched module against source patch, with module-info...");

        log = new JavacTask(tb)
                .options("--patch-module", "m=" + patch.toString(),
                         "-sourcepath", src.toString(),
                         "-verbose")
                .outdir(mClasses)
                .files(findJavaFiles(patch.resolve("module-info.java"), src.resolve("test")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (log.stream().filter(line -> line.contains("[parsing started")).count() != 2) {
            throw new AssertionError("incorrect number of parsing events.");
        }

        tb.cleanDirectory(mClasses);

        System.err.println("Building patched module against source patch with source patch on patch path...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "-sourcepath", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(src.resolve("test"), patch))
            .run()
            .writeAll();

        Path patchClasses = classes.resolve("patch");
        tb.createDirectories(patchClasses);

        System.err.println("Building patch...");

        new JavacTask(tb)
            .outdir(patchClasses)
            .files(findJavaFiles(patch))
            .run()
            .writeAll();

        tb.cleanDirectory(mClasses);

        Files.delete(patch.resolve("module-info.java"));
        Files.copy(patchClasses.resolve("module-info.class"), patch.resolve("module-info.class"));

        System.err.println("Building patched module against binary patch...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "-sourcepath", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(src.resolve("test")))
            .run()
            .writeAll();

        tb.cleanDirectory(mClasses);

        System.err.println("Building patched module against binary patch with source patch on patch path...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "-sourcepath", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(src.resolve("test"), patch))
            .run()
            .writeAll();
    }

    @Test
    public void testModuleInfoToModuleSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        tb.writeJavaFiles(m,
                          "module m { exports api; }",
                          "package api; public class Api {}",
                          "package test; public class Test { private void test() { api.Api.element = null; } }");
        Path patch = base.resolve("patch");
        tb.writeJavaFiles(patch,
                          "module m { requires java.compiler; exports api; }",
                          "package api; public class Api { public static javax.lang.model.element.Element element; }");
        Path classes = base.resolve("classes");
        Path mClasses = classes.resolve("m");
        tb.createDirectories(mClasses);

        System.err.println("Building patched module against source patch...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "--module-source-path", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(m.resolve("test")))
            .run()
            .writeAll();

        //incremental compilation:

        System.err.println("Incremental building of patched module against source patch...");

        List<String> log = new JavacTask(tb)
                .options("--patch-module", "m=" + patch.toString(),
                         "--module-source-path", src.toString(),
                         "-verbose")
                .outdir(mClasses)
                .files(findJavaFiles(m.resolve("test")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (log.stream().filter(line -> line.contains("[parsing started")).count() != 1) {
            throw new AssertionError("incorrect number of parsing events.");
        }

        tb.cleanDirectory(mClasses);

        System.err.println("Building patched module against source patch with source patch on patch path...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "--module-source-path", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(m.resolve("test"), patch))
            .run()
            .writeAll();

        Path patchClasses = classes.resolve("patch");
        tb.createDirectories(patchClasses);

        System.err.println("Building patch...");

        new JavacTask(tb)
            .outdir(patchClasses)
            .files(findJavaFiles(patch))
            .run()
            .writeAll();

        tb.cleanDirectory(mClasses);

        Files.delete(patch.resolve("module-info.java"));
        Files.copy(patchClasses.resolve("module-info.class"), patch.resolve("module-info.class"));

        System.err.println("Building patched module against binary patch...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "--module-source-path", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(m.resolve("test")))
            .run()
            .writeAll();

        tb.cleanDirectory(mClasses);

        System.err.println("Building patched module against binary patch with source patch on patch path...");

        new JavacTask(tb)
            .options("--patch-module", "m=" + patch.toString(),
                     "--module-source-path", src.toString())
            .outdir(mClasses)
            .files(findJavaFiles(m.resolve("test"), patch))
            .run()
            .writeAll();
    }

}
