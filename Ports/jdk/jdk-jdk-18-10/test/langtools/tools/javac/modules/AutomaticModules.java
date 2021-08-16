/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8155026 8178011 8220702 8261625
 * @summary Test automatic modules
 * @library /tools/lib
 * @modules
 *      java.desktop
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask ModuleTestBase
 * @run main AutomaticModules
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;

public class AutomaticModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        AutomaticModules t = new AutomaticModules();
        t.runTests();
    }

    @Test
    public void testSimple(Path base) throws Exception {
        Path legacySrc = base.resolve("legacy-src");
        tb.writeJavaFiles(legacySrc,
                          "package api; import java.awt.event.ActionListener; public abstract class Api implements ActionListener {}");
        Path legacyClasses = base.resolve("legacy-classes");
        Files.createDirectories(legacyClasses);

        String log = new JavacTask(tb)
                .options()
                .outdir(legacyClasses)
                .files(findJavaFiles(legacySrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new Exception("unexpected output: " + log);
        }

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path jar = modulePath.resolve("test-api-1.0.jar");

        new JarTask(tb, jar)
          .baseDir(legacyClasses)
          .files("api/Api.class")
          .run();

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { requires test.api; requires java.desktop; }",
                          "package impl; public class Impl { public void e(api.Api api) { api.actionPerformed(null); } }");

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(), "--module-path", modulePath.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();
    }

    @Test
    public void testUnnamedModule(Path base) throws Exception {
        Path legacySrc = base.resolve("legacy-src");
        tb.writeJavaFiles(legacySrc,
                          "package api; public abstract class Api { public void run(CharSequence str) { } private void run(base.Base base) { } }",
                          "package base; public interface Base { public void run(); }");
        Path legacyClasses = base.resolve("legacy-classes");
        Files.createDirectories(legacyClasses);

        String log = new JavacTask(tb)
                .options()
                .outdir(legacyClasses)
                .files(findJavaFiles(legacySrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new Exception("unexpected output: " + log);
        }

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path apiJar = modulePath.resolve("test-api-1.0.jar");

        new JarTask(tb, apiJar)
          .baseDir(legacyClasses)
          .files("api/Api.class")
          .run();

        Path baseJar = base.resolve("base.jar");

        new JarTask(tb, baseJar)
          .baseDir(legacyClasses)
          .files("base/Base.class")
          .run();

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { requires test.api; }",
                          "package impl; public class Impl { public void e(api.Api api) { api.run(\"\"); } }");

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(), "--module-path", modulePath.toString(), "--class-path", baseJar.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();
    }

    @Test
    public void testModuleInfoFromClassFileDependsOnAutomatic(Path base) throws Exception {
        Path automaticSrc = base.resolve("automaticSrc");
        tb.writeJavaFiles(automaticSrc, "package api; public class Api {}");
        Path automaticClasses = base.resolve("automaticClasses");
        tb.createDirectories(automaticClasses);

        String automaticLog = new JavacTask(tb)
                                .outdir(automaticClasses)
                                .files(findJavaFiles(automaticSrc))
                                .run()
                                .writeAll()
                                .getOutput(Task.OutputKind.DIRECT);

        if (!automaticLog.isEmpty())
            throw new Exception("expected output not found: " + automaticLog);

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path automaticJar = modulePath.resolve("automatic-1.0.jar");

        new JarTask(tb, automaticJar)
          .baseDir(automaticClasses)
          .files("api/Api.class")
          .run();

        Path depSrc = base.resolve("depSrc");
        Path depClasses = base.resolve("depClasses");

        Files.createDirectories(depSrc);
        Files.createDirectories(depClasses);

        tb.writeJavaFiles(depSrc,
                          "module m1x { requires transitive automatic; }",
                          "package dep; public class Dep { api.Api api; }");

        new JavacTask(tb)
                .options("--module-path", modulePath.toString())
                .outdir(depClasses)
                .files(findJavaFiles(depSrc))
                .run()
                .writeAll();

        Path moduleJar = modulePath.resolve("m1x.jar");

        new JarTask(tb, moduleJar)
          .baseDir(depClasses)
          .files("module-info.class", "dep/Dep.class")
          .run();

        Path testSrc = base.resolve("testSrc");
        Path testClasses = base.resolve("testClasses");

        Files.createDirectories(testSrc);
        Files.createDirectories(testClasses);

        tb.writeJavaFiles(testSrc,
                          "module m2x { requires automatic; }",
                          "package test; public class Test { }");

        new JavacTask(tb)
                .options("--module-path", modulePath.toString())
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run()
                .writeAll();
    }

    @Test
    public void testAutomaticAndNamedModules(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        for (char c : new char[] {'A', 'B'}) {
            Path automaticSrc = base.resolve("automaticSrc" + c);
            tb.writeJavaFiles(automaticSrc, "package api" + c + "; public class Api {}");
            Path automaticClasses = base.resolve("automaticClasses" + c);
            tb.createDirectories(automaticClasses);

            String automaticLog = new JavacTask(tb)
                                    .outdir(automaticClasses)
                                    .files(findJavaFiles(automaticSrc))
                                    .run()
                                    .writeAll()
                                    .getOutput(Task.OutputKind.DIRECT);

            if (!automaticLog.isEmpty())
                throw new Exception("expected output not found: " + automaticLog);

            Path automaticJar = modulePath.resolve("automatic" + c + "-1.0.jar");

            new JarTask(tb, automaticJar)
              .baseDir(automaticClasses)
              .files("api" + c + "/Api.class")
              .run();
        }

        Path moduleSrc = base.resolve("module-src");

        tb.writeJavaFiles(moduleSrc.resolve("m1x"),
                          "module m1x { requires static automaticA; }",
                          "package impl; public class Impl { apiA.Api a; apiB.Api b; m2x.M2 m;}");

        tb.writeJavaFiles(moduleSrc.resolve("m2x"),
                          "module m2x { exports m2x; }",
                          "package m2x; public class M2 { }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "--module-path", modulePath.toString(),
                         "--add-modules", "automaticB",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("Impl.java:1:59: compiler.err.package.not.visible: m2x, (compiler.misc.not.def.access.does.not.read: m1x, m2x, m2x)",
                                              "1 error");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("Impl.java:1:59: compiler.err.package.not.visible: m2x, (compiler.misc.not.def.access.does.not.read: m1x, m2x, m2x)",
                                 "1 error");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }
    }

    @Test
    public void testWithTrailingVersion(Path base) throws Exception {
        Path legacySrc = base.resolve("legacy-src");
        tb.writeJavaFiles(legacySrc,
                          "package api; public class Api {}");
        Path legacyClasses = base.resolve("legacy-classes");
        Files.createDirectories(legacyClasses);

        String log = new JavacTask(tb)
                .options()
                .outdir(legacyClasses)
                .files(findJavaFiles(legacySrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new Exception("unexpected output: " + log);
        }

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path jar = modulePath.resolve("test1.jar");

        new JarTask(tb, jar)
          .baseDir(legacyClasses)
          .files("api/Api.class")
          .run();

        Path moduleSrc = base.resolve("module-src");
        Path m = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m,
                          "module m { requires test1; }",
                          "package impl; public class Impl { public void e(api.Api api) { } }");

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(), "--module-path", modulePath.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();
    }

    @Test
    public void testMultipleAutomatic(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        for (char c : new char[] {'A', 'B'}) {
            Path automaticSrc = base.resolve("automaticSrc" + c);
            tb.writeJavaFiles(automaticSrc, "package api" + c + "; public class Api {}");
            Path automaticClasses = base.resolve("automaticClasses" + c);
            tb.createDirectories(automaticClasses);

            String automaticLog = new JavacTask(tb)
                                    .outdir(automaticClasses)
                                    .files(findJavaFiles(automaticSrc))
                                    .run()
                                    .writeAll()
                                    .getOutput(Task.OutputKind.DIRECT);

            if (!automaticLog.isEmpty())
                throw new Exception("expected output not found: " + automaticLog);

            Path automaticJar = modulePath.resolve("automatic" + c + "-1.0.jar");

            new JarTask(tb, automaticJar)
              .baseDir(automaticClasses)
              .files("api" + c + "/Api.class")
              .run();
        }

        Path src = base.resolve("src");

        tb.writeJavaFiles(src.resolve("m1x"),
                          "package impl; public class Impl { apiA.Api a; apiB.Api b; }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("Impl.java:1:35: compiler.err.package.not.visible: apiA, (compiler.misc.not.def.access.does.not.read.from.unnamed: apiA, automaticA)",
                                              "Impl.java:1:47: compiler.err.package.not.visible: apiB, (compiler.misc.not.def.access.does.not.read.from.unnamed: apiB, automaticB)",
                                              "2 errors");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--add-modules", "automaticA",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testLintRequireAutomatic(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        for (char c : new char[] {'A', 'B'}) {
            Path automaticSrc = base.resolve("automaticSrc" + c);
            tb.writeJavaFiles(automaticSrc, "package api" + c + "; public class Api {}");
            Path automaticClasses = base.resolve("automaticClasses" + c);
            tb.createDirectories(automaticClasses);

            String automaticLog = new JavacTask(tb)
                                    .outdir(automaticClasses)
                                    .files(findJavaFiles(automaticSrc))
                                    .run()
                                    .writeAll()
                                    .getOutput(Task.OutputKind.DIRECT);

            if (!automaticLog.isEmpty())
                throw new Exception("expected output not found: " + automaticLog);

            Path automaticJar = modulePath.resolve("automatic" + c + "-1.0.jar");

            new JarTask(tb, automaticJar)
              .baseDir(automaticClasses)
              .files("api" + c + "/Api.class")
              .run();
        }

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          """
                              module m1x {
                                  requires transitive automaticA;
                                  requires automaticB;
                              }""");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        List<String> expected;
        List<String> log;

        log = new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("module-info.java:2:25: compiler.warn.requires.transitive.automatic",
                                 "- compiler.err.warnings.and.werror",
                                 "1 error",
                                 "1 warning");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        log = new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-Xlint:requires-automatic",
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("module-info.java:2:25: compiler.warn.requires.transitive.automatic",
                                 "module-info.java:3:14: compiler.warn.requires.automatic",
                                 "- compiler.err.warnings.and.werror",
                                 "1 error",
                                 "2 warnings");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        log = new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-Xlint:-requires-transitive-automatic,requires-automatic",
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("module-info.java:2:25: compiler.warn.requires.automatic",
                                 "module-info.java:3:14: compiler.warn.requires.automatic",
                                 "- compiler.err.warnings.and.werror",
                                 "1 error",
                                 "2 warnings");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-Xlint:-requires-transitive-automatic",
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.SUCCESS)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        tb.writeJavaFiles(src,
                          """
                              @SuppressWarnings("requires-transitive-automatic")
                              module m1x {
                                  requires transitive automaticA;
                                  requires automaticB;
                              }""");

        new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.SUCCESS)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        log = new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-Xlint:requires-automatic",
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("module-info.java:3:25: compiler.warn.requires.automatic",
                                 "module-info.java:4:14: compiler.warn.requires.automatic",
                                 "- compiler.err.warnings.and.werror",
                                 "1 error",
                                 "2 warnings");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }

        tb.writeJavaFiles(src,
                          """
                              @SuppressWarnings("requires-automatic")
                              module m1x {
                                  requires transitive automaticA;
                                  requires automaticB;
                              }""");

        log = new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "--module-path", modulePath.toString(),
                     "-Xlint:requires-automatic",
                     "-XDrawDiagnostics",
                     "-Werror")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("module-info.java:3:25: compiler.warn.requires.transitive.automatic",
                                 "- compiler.err.warnings.and.werror",
                                 "1 error",
                                 "1 warning");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }
    }

    @Test
    public void testAutomaticModuleNameCorrect(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path automaticSrc = base.resolve("automaticSrc");
        tb.writeJavaFiles(automaticSrc, "package api; public class Api {}");
        Path automaticClasses = base.resolve("automaticClasses");
        tb.createDirectories(automaticClasses);

        String automaticLog = new JavacTask(tb)
                                .outdir(automaticClasses)
                                .files(findJavaFiles(automaticSrc))
                                .run()
                                .writeAll()
                                .getOutput(Task.OutputKind.DIRECT);

        if (!automaticLog.isEmpty())
            throw new Exception("expected output not found: " + automaticLog);

        Path automaticJar = modulePath.resolve("automatic-1.0.jar");

        new JarTask(tb, automaticJar)
          .baseDir(automaticClasses)
          .files("api/Api.class")
          .manifest("Automatic-Module-Name: custom.module.name\n\n")
          .run();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "module m { requires custom.module.name; }",
                          "package impl; public class Impl { api.Api a; }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .processors(new AbstractProcessor() {
                         // Processor verifies api.Api is enclosed by an automatic module.
                         @Override
                         public Set<String> getSupportedAnnotationTypes() {
                             return Set.of("*");
                         }

                         @Override
                         public SourceVersion getSupportedSourceVersion() {
                             return SourceVersion.latestSupported();
                         }

                         @Override
                         public boolean process(Set<? extends TypeElement> annotations,
                                                RoundEnvironment roundEnv) {
                             if (!roundEnv.processingOver()) {
                                 var elts = processingEnv.getElementUtils();
                                 if (!elts.isAutomaticModule(elts.getModuleOf(elts.getTypeElement("api.Api")))) {
                                     throw new RuntimeException("module of class api.Api is not automatic");
                                 }
                             }
                             return true;
                         }
                    })
                .run(Task.Expect.SUCCESS);

        tb.writeJavaFiles(src,
                          "module m { requires automatic; }");

        List<String> log = new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                Arrays.asList("module-info.java:1:21: compiler.err.module.not.found: automatic",
                              "1 error");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }
    }

    @Test
    public void testAutomaticModuleNameIncorrect(Path base) throws Exception {
        for (String name : new String[] {"", "999", "foo.class", "foo._"}) {
            if (Files.isDirectory(base)) {
                tb.cleanDirectory(base);
            }
            Path modulePath = base.resolve("module-path");

            Files.createDirectories(modulePath);

            Path automaticSrc = base.resolve("automaticSrc");
            tb.writeJavaFiles(automaticSrc, "package api; public class Api {}");
            Path automaticClasses = base.resolve("automaticClasses");
            tb.createDirectories(automaticClasses);

            String automaticLog = new JavacTask(tb)
                                    .outdir(automaticClasses)
                                    .files(findJavaFiles(automaticSrc))
                                    .run()
                                    .writeAll()
                                    .getOutput(Task.OutputKind.DIRECT);

            if (!automaticLog.isEmpty())
                throw new Exception("expected output not found: " + automaticLog);

            Path automaticJar = modulePath.resolve("automatic-1.0.jar");

            new JarTask(tb, automaticJar)
              .baseDir(automaticClasses)
              .files("api/Api.class")
              .manifest("Automatic-Module-Name: " + name + "\n\n")
              .run();

            Path src = base.resolve("src");

            tb.writeJavaFiles(src,
                              "package impl; public class Impl { api.Api a; }");

            Path classes = base.resolve("classes");

            Files.createDirectories(classes);

            List<String> log = new JavacTask(tb, Mode.CMDLINE)
                    .options("--module-path", modulePath.toString(),
                             "--add-modules", "ALL-MODULE-PATH",
                             "-XDrawDiagnostics")
                    .outdir(classes)
                    .files(findJavaFiles(src))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

            List<String> expected =
                    Arrays.asList("- compiler.err.locn.cant.get.module.name.for.jar: " +
                                      "testAutomaticModuleNameIncorrect/module-path/automatic-1.0.jar".replace("/", File.separator),
                                  "1 error");

            if (!expected.equals(log)) {
                throw new Exception("expected output not found: " + log);
            }
        }
    }

    @Test
    public void testAutomaticModuleNameBroken(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path automaticSrc = base.resolve("automaticSrc");
        tb.writeJavaFiles(automaticSrc, "package api; public class Api {}");
        Path automaticClasses = base.resolve("automaticClasses");
        tb.createDirectories(automaticClasses);

        String automaticLog = new JavacTask(tb)
                                .outdir(automaticClasses)
                                .files(findJavaFiles(automaticSrc))
                                .run()
                                .writeAll()
                                .getOutput(Task.OutputKind.DIRECT);

        if (!automaticLog.isEmpty())
            throw new Exception("expected output not found: " + automaticLog);

        Path automaticJar = modulePath.resolve("automatic-1.0.jar");

        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(automaticJar))) {
            out.putNextEntry(new ZipEntry("api/Api.class"));
            Files.copy(automaticClasses.resolve("api").resolve("Api.class"), out);
        }

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "module m { requires automatic; }",
                          "package impl; public class Impl { api.Api a; }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        tb.writeJavaFiles(src,
                          "module m { requires custom.module.name; }");

        List<String> log = new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                Arrays.asList("module-info.java:1:34: compiler.err.module.not.found: custom.module.name",
                              "1 error");

        if (!expected.equals(log)) {
            throw new Exception("expected output not found: " + log);
        }
    }

    @Test
    public void testAutomaticModulePatchingAndAllModulePath(Path base) throws Exception {
        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path libaSrc = base.resolve("libaSrc");
        tb.writeJavaFiles(libaSrc,
                          "module liba { exports api1; }",
                          "package api1; public class Api1 {}");
        Path libaClasses = modulePath.resolve("liba");
        tb.createDirectories(libaClasses);

        new JavacTask(tb)
            .outdir(libaClasses)
            .files(findJavaFiles(libaSrc))
            .run()
            .writeAll();

        Path libbSrc = base.resolve("libbSrc");
        tb.writeJavaFiles(libbSrc,
                          "module libb { exports api2; }",
                          "package api2; public class Api2 {}");
        Path libbClasses = modulePath.resolve("libb");
        tb.createDirectories(libbClasses);

        new JavacTask(tb)
            .outdir(libbClasses)
            .files(findJavaFiles(libbSrc))
            .run()
            .writeAll();

        Path automaticSrc = base.resolve("automaticSrc");
        tb.writeJavaFiles(automaticSrc, "package aut; public class Aut1 { api1.Api1 a1; }");
        Path automaticClasses = base.resolve("automaticClasses");
        tb.createDirectories(automaticClasses);

        new JavacTask(tb)
            .outdir(automaticClasses)
            .options("--add-modules", "liba",
                     "--module-path", modulePath.toString())
            .files(findJavaFiles(automaticSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        Path automaticJar = modulePath.resolve("automatic-1.0.jar");

        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(automaticJar))) {
            out.putNextEntry(new ZipEntry("aut/Aut1.class"));
            Files.copy(automaticClasses.resolve("aut").resolve("Aut1.class"), out);
        }

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package aut; public class Aut2 { api2.Api2 a2; aut.Aut1 aut1;}");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--patch-module", "automatic=" + src.toString(),
                         "--add-modules", "ALL-MODULE-PATH")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--patch-module", "automatic=" + src.toString(),
                         "--module-source-path", "dummy",
                         "--add-modules", "ALL-MODULE-PATH")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
    }

}
