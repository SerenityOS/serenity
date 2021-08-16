/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Ensure named modules cannot refer to classpath types.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main ModulesAndClassPathTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;

public class ModulesAndClassPathTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new ModulesAndClassPathTest().runTests();
    }

    @Test
    public void testModulesAndClassPath(Path base) throws Exception {
        Path jar = prepareTestJar(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m { }",
                          "package impl; public class Impl { api.Api api; }");

        List<String> modLog = new JavacTask(tb)
                                .options("--class-path", jar.toString(),
                                         "-XDrawDiagnostics")
                                .outdir(classes)
                                .files(findJavaFiles(moduleSrc))
                                .run(Task.Expect.FAIL)
                                .writeAll()
                                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("Impl.java:1:35: compiler.err.package.not.visible: api, (compiler.misc.not.def.access.does.not.read.unnamed: api, m)",
                                              "1 error");

        if (!expected.equals(modLog)) {
            throw new Exception("unexpected output: " + modLog);
        }

        new JavacTask(tb)
          .options("--class-path", jar.toString(),
                   "--add-reads", "m=ALL-UNNAMED")
          .outdir(classes)
          .files(findJavaFiles(moduleSrc))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);

        new JavacTask(tb)
          .options("--class-path", jar.toString() + File.pathSeparator + System.getProperty("test.classes"),
                   "--add-reads", "m=ALL-UNNAMED",
                   "-processor", ProcessorImpl.class.getName())
          .outdir(classes)
          .files(findJavaFiles(moduleSrc))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);
    }

    @Test
    public void testImplicitSourcePathModuleInfo(Path base) throws Exception {
        Path jar = prepareTestJar(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m { }",
                          "package impl; public class Impl { api.Api api; }");

        List<String> modLog = new JavacTask(tb)
                                .options("--class-path", jar.toString(),
                                         "-sourcepath", m1.toString(),
                                         "-XDrawDiagnostics")
                                .outdir(classes)
                                .files(m1.resolve("impl").resolve("Impl.java"))
                                .run(Task.Expect.FAIL)
                                .writeAll()
                                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("Impl.java:1:35: compiler.err.package.not.visible: api, (compiler.misc.not.def.access.does.not.read.unnamed: api, m)",
                                              "1 error");

        if (!expected.equals(modLog)) {
            throw new Exception("unexpected output: " + modLog);
        }
    }

    @Test
    public void testModuleInfoFromOutput(Path base) throws Exception {
        Path jar = prepareTestJar(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m { }",
                          "package impl; public class Impl { api.Api api; }");

        new JavacTask(tb)
          .options("--class-path", jar.toString(),
                   "-XDrawDiagnostics")
          .outdir(classes)
          .files(m1.resolve("module-info.java"))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);

        List<String> modLog = new JavacTask(tb)
                                .options("--class-path", jar.toString(),
                                         "-XDrawDiagnostics")
                                .outdir(classes)
                                .files(m1.resolve("impl").resolve("Impl.java"))
                                .run(Task.Expect.FAIL)
                                .writeAll()
                                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("Impl.java:1:35: compiler.err.package.not.visible: api, (compiler.misc.not.def.access.does.not.read.unnamed: api, m)",
                                              "1 error");

        if (!expected.equals(modLog)) {
            throw new Exception("unexpected output: " + modLog);
        }
    }

    private Path prepareTestJar(Path base) throws Exception {
        Path legacySrc = base.resolve("legacy-src");
        tb.writeJavaFiles(legacySrc,
                          "package api; public abstract class Api {}");
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

        Path lib = base.resolve("lib");

        Files.createDirectories(lib);

        Path jar = lib.resolve("test-api-1.0.jar");

        new JarTask(tb, jar)
          .baseDir(legacyClasses)
          .files("api/Api.class")
          .run();

        return jar;
    }

    @SupportedAnnotationTypes("*")
    public static class ProcessorImpl extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }
    }

    @Test
    public void testClassOutputVisibleForIncrementalCompilation(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m { exports impl; }",
                          "package impl; public class Impl { }",
                          "package src; public class Src { }",
                          "package test; public class TestCP extends impl.Impl { }",
                          "package test; public class TestSP extends src.Src { }");

        new JavacTask(tb)
          .outdir(classes)
          .files(m1.resolve("impl").resolve("Impl.java"))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);

        new JavacTask(tb)
          .outdir(classes)
          .files(m1.resolve("module-info.java"))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);

        new JavacTask(tb)
          .outdir(classes)
          .files(m1.resolve("test").resolve("TestCP.java"))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);

        new JavacTask(tb)
          .options("-sourcepath", m1.toString())
          .outdir(classes)
          .files(m1.resolve("test").resolve("TestSP.java"))
          .run()
          .writeAll()
          .getOutputLines(Task.OutputKind.DIRECT);
    }
}
