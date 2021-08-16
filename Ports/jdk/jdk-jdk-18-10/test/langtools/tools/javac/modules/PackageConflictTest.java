/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test module/package conflicts
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main PackageConflictTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

public class PackageConflictTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        PackageConflictTest t = new PackageConflictTest();
        t.runTests();
    }

    @Test
    public void testSimple(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package java.util; public class MyList { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("MyList.java:1:1: compiler.err.package.in.other.module: java.base"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testDisjoint(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        Path m2 = base.resolve("m2x");
        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package test; public class A { }");
        tb.writeJavaFiles(m2,
                          "module m2x { }",
                          "package test; public class B { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
          .options("-Werror", "--module-source-path", base.toString())
          .outdir(classes)
          .files(findJavaFiles(base))
          .run()
          .writeAll();
    }

    @Test
    public void testConflictInDependencies(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        Path m2 = base.resolve("m2x");
        Path m3 = base.resolve("m3x");
        tb.writeJavaFiles(m1,
                          "module m1x { exports test; }",
                          "package test; public class A { }");
        tb.writeJavaFiles(m2,
                          "module m2x { exports test; }",
                          "package test; public class B { }");
        tb.writeJavaFiles(m3,
                          "module m3x { requires m1x; requires m2x; }",
                          "package impl; public class Impl { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                       .options("-XDrawDiagnostics", "--module-source-path", base.toString())
                       .outdir(classes)
                       .files(findJavaFiles(base))
                       .run(Task.Expect.FAIL)
                       .writeAll()
                       .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                Arrays.asList("module-info.java:1:1: compiler.err.package.clash.from.requires: m3x, test, m1x, m2x",
                              "1 error");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testSimple2(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "N")
                .exports("pack")
                .classes("package pack; public class A { }")
                .build(modules);
        new ModuleBuilder(tb, "M")
                .requires("N")
                .classes("package pack; public class B { pack.A f; }")
                .write(modSrc);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-p", modules.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc.resolve("M")))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("B.java:1:1: compiler.err.package.in.other.module: N"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testPrivateConflict(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        new ModuleBuilder(tb, "N")
                .exports("publ")
                .classes("package pack; public class A { }")
                .classes("package publ; public class B { }")
                .write(modSrc);
        new ModuleBuilder(tb, "M")
                .requires("N")
                .classes("package pack; public class C { publ.B b; }")
                .write(modSrc);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", modSrc.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains(""))
            throw new Exception("unexpected output not found");

    }

    @Test
    public void testPrivateConflictOnModulePath(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "N")
                .exports("publ")
                .classes("package pack; public class A { }")
                .classes("package publ; public class B { }")
                .build(modules);
        new ModuleBuilder(tb, "M")
                .requires("N")
                .classes("package pack; public class C { publ.B b; }")
                .write(modSrc);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-p", modules.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc.resolve("M")))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains(""))
            throw new Exception("expected output not found");

    }

    @Test
    public void testRequiresConflictExports(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "M")
                .exports("pack")
                .classes("package pack; public class A { }")
                .build(modules);
        new ModuleBuilder(tb, "N")
                .exports("pack")
                .classes("package pack; public class B { }")
                .build(modules);
        new ModuleBuilder(tb, "K")
                .requires("M")
                .requires("N")
                .classes("package pkg; public class C { pack.A a; pack.B b; }")
                .write(modSrc);

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-p", modules.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc.resolve("K")))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                Arrays.asList("module-info.java:1:1: compiler.err.package.clash.from.requires: K, pack, M, N",
                        "1 error");
        if (!log.containsAll(expected))
            throw new Exception("expected output not found");
    }

    @Test
    public void testQualifiedExportsToDifferentModules(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        new ModuleBuilder(tb, "U").write(modSrc);
        new ModuleBuilder(tb, "M")
                .exports("pkg to U")
                .classes("package pkg; public class A { public static boolean flagM; }")
                .write(modSrc);
        new ModuleBuilder(tb, "N")
                .exports("pkg to K")
                .classes("package pkg; public class A { public static boolean flagN; }")
                .write(modSrc);
        ModuleBuilder moduleK = new ModuleBuilder(tb, "K");
        moduleK.requires("M")
                .requires("N")
                .classes("package p; public class DependsOnN { boolean f = pkg.A.flagN; } ")
                .write(modSrc);
        new JavacTask(tb)
                .options("--module-source-path", modSrc.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc.resolve("K")))
                .run(Task.Expect.SUCCESS)
                .writeAll();

        //negative case
        moduleK.classes("package pkg; public class DuplicatePackage { } ")
                .classes("package p; public class DependsOnM { boolean f = pkg.A.flagM; } ")
                .write(modSrc);

        List<String> output = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", modSrc.toString())
                .outdir(Files.createDirectories(base.resolve("classes")))
                .files(findJavaFiles(modSrc.resolve("K")))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "DependsOnM.java:1:55: compiler.err.cant.resolve.location: kindname.variable, flagM, , , (compiler.misc.location: kindname.class, pkg.A, null)");
        if (!output.containsAll(expected)) {
            throw new Exception("expected output not found");
        }
    }

    @Test
    public void testConflictInDependenciesInUnnamed(Path base) throws Exception {
        Path msp = base.resolve("module-path-source");
        Path m1 = msp.resolve("m1x");
        Path m2 = msp.resolve("m2x");
        tb.writeJavaFiles(m1,
                          "module m1x { exports test; }",
                          "package test; public class A { }");
        tb.writeJavaFiles(m2,
                          "module m2x { exports test; }",
                          "package test; public class B { }");
        Path mp = base.resolve("module-path");
        Files.createDirectories(mp);

        new JavacTask(tb)
            .options("--module-source-path", msp.toString())
            .outdir(mp)
            .files(findJavaFiles(msp))
            .run()
            .writeAll();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package impl; public class Impl { }");

        Path out = base.resolve("out");
        Files.createDirectories(out);

        List<String> log = new JavacTask(tb)
                       .options("-XDrawDiagnostic",
                                "--module-path", mp.toString(),
                                "--add-modules", "ALL-MODULE-PATH")
                       .outdir(out)
                       .files(findJavaFiles(src))
                       .run(Task.Expect.FAIL)
                       .writeAll()
                       .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected1 =
                Arrays.asList("error: the unnamed module reads package test from both m1x and m2x",
                              "1 error");

        List<String> expected2 =
                Arrays.asList("error: the unnamed module reads package test from both m2x and m1x",
                              "1 error");

        if (!expected1.equals(log) && !expected2.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

}
